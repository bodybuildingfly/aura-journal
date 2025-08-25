package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.data.local.PartnerRequestDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toPartnerRequest
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.PartnerRequest
import com.mabbology.aurajournal.domain.repository.PartnerRequestsRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private const val TAG = "PartnerRequestsRepo"

class PartnerRequestsRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val userProfilesRepository: UserProfilesRepository,
    private val partnerRequestDao: PartnerRequestDao
) : PartnerRequestsRepository {

    override suspend fun getIncomingRequests(): Flow<List<PartnerRequest>> {
        val userId = account.get().id
        return partnerRequestDao.getIncomingRequests(userId).map { entities ->
            entities.map { it.toPartnerRequest() }
        }
    }

    override suspend fun getOutgoingRequests(): Flow<List<PartnerRequest>> {
        val userId = account.get().id
        return partnerRequestDao.getOutgoingRequests(userId).map { entities ->
            entities.map { it.toPartnerRequest() }
        }
    }

    override suspend fun syncRequests(): Result<Unit> {
        return try {
            val user = account.get()
            val incomingResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("dominantId", user.id), Query.equal("status", "pending"))
            )
            val outgoingResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("submissiveId", user.id), Query.equal("status", "pending"))
            )

            val remoteIncoming = incomingResponse.documents.mapNotNull { doc ->
                val submissiveId = doc.data["submissiveId"] as String
                userProfilesRepository.getUserProfile(submissiveId).getOrNull()?.let { profile ->
                    PartnerRequest(doc.id, doc.data["dominantId"] as String, submissiveId, doc.data["status"] as String, profile.displayName)
                }
            }

            val remoteOutgoing = outgoingResponse.documents.mapNotNull { doc ->
                val dominantId = doc.data["dominantId"] as String
                userProfilesRepository.getUserProfile(dominantId).getOrNull()?.let { profile ->
                    PartnerRequest(doc.id, dominantId, doc.data["submissiveId"] as String, doc.data["status"] as String, profile.displayName)
                }
            }

            val allRequests = remoteIncoming + remoteOutgoing
            partnerRequestDao.clearRequests()
            partnerRequestDao.upsertRequests(allRequests.map { it.toEntity() })

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPartnerRequest(dominantId: String, submissiveId: String): Result<Unit> {
        val dominantProfile = userProfilesRepository.getUserProfile(dominantId).getOrNull() ?: return Result.failure(Exception("Dominant profile not found"))

        val tempId = "local_${UUID.randomUUID()}"
        val newRequest = PartnerRequest(
            id = tempId,
            dominantId = dominantId,
            submissiveId = submissiveId,
            status = "pending",
            counterpartyName = dominantProfile.displayName
        )
        Log.d(TAG, "Optimistic Create: Inserting temporary local request with id $tempId")
        partnerRequestDao.upsertRequests(listOf(newRequest.toEntity()))

        return try {
            val payload = Gson().toJson(mapOf("dominantId" to dominantId, "submissiveId" to submissiveId))
            functions.createExecution(functionId = AppwriteConstants.CONNECTION_REQUESTS_FUNCTION_ID, body = payload)
            // We don't need the remote ID, so we just sync to get the final version
            syncRequests()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
            partnerRequestDao.deleteRequestById(tempId)
            Result.failure(e)
        }
    }

    override suspend fun approveRequest(request: PartnerRequest): Result<Unit> {
        return try {
            val payload = Gson().toJson(mapOf("requestId" to request.id, "dominantId" to request.dominantId, "submissiveId" to request.submissiveId))
            functions.createExecution(functionId = AppwriteConstants.APPROVE_CONNECTION_REQUEST_FUNCTION_ID, body = payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                documentId = requestId,
                data = mapOf("status" to "rejected")
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
