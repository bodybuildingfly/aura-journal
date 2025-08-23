package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.ConnectionRequest

interface ConnectionRequestsRepository {
    suspend fun sendConnectionRequest(recipientId: String): Result<Unit>
    suspend fun getIncomingRequests(): Result<List<ConnectionRequest>>
    suspend fun getOutgoingRequests(): Result<List<ConnectionRequest>>
    suspend fun approveRequest(requestId: String): Result<Unit>
    suspend fun rejectRequest(requestId: String): Result<Unit>
}
