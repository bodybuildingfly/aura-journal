package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalAssignmentDao {

    @Upsert
    suspend fun upsertAssignments(assignments: List<JournalAssignmentEntity>)

    @Query("SELECT * FROM journal_assignments WHERE submissiveId = :userId AND status = 'pending'")
    fun getPendingAssignments(userId: String): Flow<List<JournalAssignmentEntity>>

    @Query("DELETE FROM journal_assignments")
    suspend fun clearAssignments()

    // New function to delete a single assignment by its ID
    @Query("DELETE FROM journal_assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: String)
}
