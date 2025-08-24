package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.ConnectionRequest
import com.mabbology.aurajournal.domain.repository.ConnectionRequestsRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import javax.inject.Inject

class ConnectionRequestsRepositoryImpl @Inject constructor(
    private val client: Client,
    private val userProfilesRepository: UserProfilesRepository
) : ConnectionRequestsRepository {

    private val databases by lazy { Databases(client) }
    private val account by lazy { Account(client) }
    private val functions by lazy { Functions(client) }

    override suspend fun sendConnectionRequest(recipientId: String, role: String): Result<Unit> {
        return try {
            val payload = Gson().toJson(mapOf(
                "recipientId" to recipientId,
                "counterpartyRole" to role
            ))
            functions.createExecution(
                functionId = AppwriteConstants.CONNECTION_REQUESTS_FUNCTION_ID,
                body = payload
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIncomingRequests(): Result<List<ConnectionRequest>> {
        return try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.CONNECTION_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("recipientId", user.id))
            )
            val requests = response.documents.map { doc ->
                val requesterId = doc.data["requesterId"] as String
                val profileResult = userProfilesRepository.getUserProfile(requesterId)
                val requesterName = profileResult.getOrNull()?.displayName ?: "Unknown User"

                ConnectionRequest(
                    id = doc.id,
                    requesterId = requesterId,
                    recipientId = doc.data["recipientId"] as String,
                    status = doc.data["status"] as String,
                    counterpartyName = requesterName,
                    counterpartyRole = doc.data["counterpartyRole"] as? String ?: "Partner"
                )
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOutgoingRequests(): Result<List<ConnectionRequest>> {
        return try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.CONNECTION_REQUESTS_COLLECTION_ID,
                queries = listOf(Query.equal("requesterId", user.id))
            )
            val requests = response.documents.map { doc ->
                val recipientId = doc.data["recipientId"] as String
                val profileResult = userProfilesRepository.getUserProfile(recipientId)
                val recipientName = profileResult.getOrNull()?.displayName ?: "Unknown User"

                ConnectionRequest(
                    id = doc.id,
                    requesterId = doc.data["requesterId"] as String,
                    recipientId = recipientId,
                    status = doc.data["status"] as String,
                    counterpartyName = recipientName,
                    counterpartyRole = doc.data["counterpartyRole"] as? String ?: "Partner"
                )
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveRequest(request: ConnectionRequest): Result<Unit> {
        return try {
            val payload = Gson().toJson(mapOf(
                "requestId" to request.id,
                "recipientId" to request.recipientId,
                "requesterId" to request.requesterId,
                "counterpartyRole" to request.counterpartyRole
            ))
            functions.createExecution(
                functionId = AppwriteConstants.APPROVE_CONNECTION_REQUEST_FUNCTION_ID,
                body = payload
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectRequest(requestId: String): Result<Unit> {
        return try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.CONNECTION_REQUESTS_COLLECTION_ID,
                documentId = requestId,
                data = mapOf("status" to "rejected")
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
