package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.domain.model.Partner
import kotlinx.coroutines.flow.Flow

interface JournalAssignmentsRepository {
    fun getAssignments(partner: Partner?): Flow<List<JournalAssignment>>
    suspend fun syncAssignments(): DataResult<Unit>
    suspend fun createAssignment(submissiveId: String, prompt: String): DataResult<JournalAssignment>
    suspend fun completeAssignment(assignmentId: String): DataResult<Unit>
}
