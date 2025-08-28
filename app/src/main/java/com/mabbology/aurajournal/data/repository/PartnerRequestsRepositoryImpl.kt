package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private const val TAG = "PartnerRequestsRepo"

class PartnerRequestsRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val userProfilesRepository: UserProfilesRepository,
    private val partnerRequestDao: PartnerRequestDao,
    private val dispatcherProvider: DispatcherProvider
) : PartnerRequestsRepository {

    override suspend fun getIncomingRequests(): Flow<List<PartnerRequest>> = withContext(dispatcherProvider.io) {
        val userId = account.get().id
        partnerRequestDao.getIncomingRequests(userId).map { entities ->
            entities.map { it.toPartnerRequest() }
        }
    }

    override suspend fun getOutgoingRequests(): Flow<List<PartnerRequest>> = withContext(dispatcherProvider.io) {
        val userId = account.get().id
        partnerRequestDao.getOutgoingRequests(userId).map { entities ->
            entities.map { it.toPartnerRequest() }
        }
    }

    override suspend fun syncRequests(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "syncRequests: Starting partner requests sync")
        try {
            val user = account.get()
            Log.d(TAG, "syncRequests: Current user ID: ${user.id}")

            Log.d(TAG, "syncRequests: Fetching incoming requests")
            val incomingResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("dominantId", listOf(user.id)), Query.equal("status", listOf("pending")))
            )
            Log.d(TAG, "syncRequests: Found ${incomingResponse.total} incoming requests from server.")

            Log.d(TAG, "syncRequests: Fetching outgoing requests")
            val outgoingResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("submissiveId", listOf(user.id)), Query.equal("status", listOf("pending")))
            )
            Log.d(TAG, "syncRequests: Found ${outgoingResponse.total} outgoing requests from server.")


            val remoteIncoming = incomingResponse.documents.mapNotNull { doc ->
                val submissiveId = doc.data["submissiveId"] as String
                Log.d(TAG, "syncRequests: Processing incoming request ${doc.id}. Fetching profile for submissive $submissiveId")
                when(val profileResult = userProfilesRepository.getUserProfile(submissiveId)) {
                    is DataResult.Success -> {
                        profileResult.data?.let { profile ->
                            Log.d(TAG, "syncRequests: Successfully fetched profile for ${profile.displayName}")
                            PartnerRequest(doc.id, doc.data["dominantId"] as String, submissiveId, doc.data["status"] as String, profile.displayName)
                        }
                    }
                    is DataResult.Error -> {
                        Log.e(TAG, "syncRequests: Failed to fetch profile for submissive $submissiveId. Error: ${profileResult.exception}")
                        null
                    }
                }
            }

            val remoteOutgoing = outgoingResponse.documents.mapNotNull { doc ->
                val dominantId = doc.data["dominantId"] as String
                Log.d(TAG, "syncRequests: Processing outgoing request ${doc.id}. Fetching profile for dominant $dominantId")
                when(val profileResult = userProfilesRepository.getUserProfile(dominantId)) {
                    is DataResult.Success -> {
                        profileResult.data?.let { profile ->
                            Log.d(TAG, "syncRequests: Successfully fetched profile for ${profile.displayName}")
                            PartnerRequest(doc.id, dominantId, doc.data["submissiveId"] as String, doc.data["status"] as String, profile.displayName)
                        }
                    }
                    is DataResult.Error -> {
                        Log.e(TAG, "syncRequests: Failed to fetch profile for dominant $dominantId. Error: ${profileResult.exception}")
                        null
                    }
                }
            }

            val allRequests = remoteIncoming + remoteOutgoing
            Log.d(TAG, "syncRequests: Total requests successfully processed: ${allRequests.size}. Clearing local DAO.")
            partnerRequestDao.clearRequests()
            Log.d(TAG, "syncRequests: Inserting ${allRequests.size} requests into local DAO.")
            partnerRequestDao.upsertRequests(allRequests.map { it.toEntity() })

            Log.d(TAG, "syncRequests: Partner requests sync completed successfully.")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncRequests: An error occurred during partner requests sync", e)
            DataResult.Error(e)
        }
    }

    override suspend fun sendPartnerRequest(dominantId: String, submissiveId: String): DataResult<PartnerRequest> {
        val dominantProfileResult = userProfilesRepository.getUserProfile(dominantId)
        val dominantProfile = when (dominantProfileResult) {
            is DataResult.Success -> dominantProfileResult.data ?: return DataResult.Error(Exception("Dominant profile not found"))
            is DataResult.Error -> return DataResult.Error(dominantProfileResult.exception)
        }

        val tempId = "local_${UUID.randomUUID()}"
        val newRequest = PartnerRequest(
            id = tempId,
            dominantId = dominantId,
            submissiveId = submissiveId,
            status = "pending",
            counterpartyName = dominantProfile.displayName
        )
        partnerRequestDao.upsertRequests(listOf(newRequest.toEntity()))

        CoroutineScope(dispatcherProvider.io).launch {
            try {
                val payload = Gson().toJson(mapOf("dominantId" to dominantId, "submissiveId" to submissiveId))
                functions.createExecution(functionId = AppwriteConstants.CONNECTION_REQUESTS_FUNCTION_ID, body = payload)
                syncRequests()
            } catch (e: Exception) {
                Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
                partnerRequestDao.deleteRequestById(tempId)
            }
        }
        return DataResult.Success(newRequest)
    }

    override suspend fun approveRequest(request: PartnerRequest): DataResult<Unit> {
        partnerRequestDao.deleteRequestById(request.id)

        CoroutineScope(dispatcherProvider.io).launch {
            try {
                val payload = Gson().toJson(mapOf("requestId" to request.id, "dominantId" to request.dominantId, "submissiveId" to request.submissiveId))
                functions.createExecution(functionId = AppwriteConstants.APPROVE_CONNECTION_REQUEST_FUNCTION_ID, body = payload)
            } catch (_: Exception) {
                partnerRequestDao.upsertRequests(listOf(request.toEntity()))
            }
        }
        return DataResult.Success(Unit)
    }

    override suspend fun rejectRequest(requestId: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNER_REQUESTS_COLLECTION_ID,
                documentId = requestId,
                data = mapOf("status" to "rejected")
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
