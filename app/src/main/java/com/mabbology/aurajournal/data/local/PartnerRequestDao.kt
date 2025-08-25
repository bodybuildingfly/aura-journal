package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerRequestDao {

    @Upsert
    suspend fun upsertRequests(requests: List<PartnerRequestEntity>)

    @Query("SELECT * FROM partner_requests WHERE dominantId = :userId AND status = 'pending'")
    fun getIncomingRequests(userId: String): Flow<List<PartnerRequestEntity>>

    @Query("SELECT * FROM partner_requests WHERE submissiveId = :userId AND status = 'pending'")
    fun getOutgoingRequests(userId: String): Flow<List<PartnerRequestEntity>>

    @Query("DELETE FROM partner_requests")
    suspend fun clearRequests()

    // Add this missing function
    @Query("DELETE FROM partner_requests WHERE id = :id")
    suspend fun deleteRequestById(id: String)
}
