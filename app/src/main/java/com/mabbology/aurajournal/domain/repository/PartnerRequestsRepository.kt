package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.PartnerRequest
import kotlinx.coroutines.flow.Flow

interface PartnerRequestsRepository {
    suspend fun getIncomingRequests(): Flow<List<PartnerRequest>>
    suspend fun getOutgoingRequests(): Flow<List<PartnerRequest>>

    suspend fun syncRequests(): Result<Unit>
    suspend fun sendPartnerRequest(dominantId: String, submissiveId: String): Result<Unit>
    suspend fun approveRequest(request: PartnerRequest): Result<Unit>
    suspend fun rejectRequest(requestId: String): Result<Unit>
}
