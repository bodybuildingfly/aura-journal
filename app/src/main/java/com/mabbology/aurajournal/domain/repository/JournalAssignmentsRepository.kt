package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.JournalAssignment
import kotlinx.coroutines.flow.Flow

interface JournalAssignmentsRepository {
    suspend fun getPendingAssignments(): Flow<List<JournalAssignment>>
    suspend fun syncAssignments(): Result<Unit>
    suspend fun createAssignment(submissiveId: String, prompt: String): Result<Unit>
    // Remove the journalId parameter from this function
    suspend fun completeAssignment(assignmentId: String): Result<Unit>
}
