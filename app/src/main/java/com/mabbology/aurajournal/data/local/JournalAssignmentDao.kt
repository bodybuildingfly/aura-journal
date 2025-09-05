package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalAssignmentDao {

    @Upsert
    suspend fun upsertAssignments(assignments: List<JournalAssignmentEntity>)


    @Query("SELECT * FROM journal_assignments WHERE (dominantId = :userId1 AND submissiveId = :userId2) OR (dominantId = :userId2 AND submissiveId = :userId1)")
    fun getAssignmentsBetweenUsers(userId1: String, userId2: String): Flow<List<JournalAssignmentEntity>>

    @Query("DELETE FROM journal_assignments")
    suspend fun clearAssignments()

    // New function to delete a single assignment by its ID
    @Query("SELECT * FROM journal_assignments WHERE id = :id")
    suspend fun getAssignmentById(id: String): JournalAssignmentEntity?

    @Query("DELETE FROM journal_assignments WHERE id = :id")
    suspend fun deleteAssignmentById(id: String)
}
