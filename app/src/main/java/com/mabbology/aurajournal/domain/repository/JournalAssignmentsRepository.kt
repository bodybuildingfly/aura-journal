package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.JournalAssignment

interface JournalAssignmentsRepository {
    suspend fun getPendingAssignments(): Result<List<JournalAssignment>>
    suspend fun createAssignment(submissiveId: String, prompt: String): Result<Unit>
    suspend fun completeAssignment(assignmentId: String, journalId: String): Result<Unit>
}
