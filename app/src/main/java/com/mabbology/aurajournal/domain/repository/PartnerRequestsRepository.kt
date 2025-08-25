package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.PartnerRequest
import kotlinx.coroutines.flow.Flow

interface PartnerRequestsRepository {
    suspend fun getIncomingRequests(): Flow<List<PartnerRequest>>
    suspend fun getOutgoingRequests(): Flow<List<PartnerRequest>>
    suspend fun syncRequests(): DataResult<Unit>
    suspend fun sendPartnerRequest(dominantId: String, submissiveId: String): DataResult<Unit>
    suspend fun approveRequest(request: PartnerRequest): DataResult<Unit>
    suspend fun rejectRequest(requestId: String): DataResult<Unit>
}
