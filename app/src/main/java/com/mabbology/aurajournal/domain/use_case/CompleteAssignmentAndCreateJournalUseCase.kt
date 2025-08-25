package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import javax.inject.Inject

class CompleteAssignmentAndCreateJournalUseCase @Inject constructor(
    private val createJournalUseCase: CreateJournalUseCase,
    private val assignmentsRepository: JournalAssignmentsRepository
) {
    suspend operator fun invoke(assignmentId: String, title: String, content: String, partnerId: String?, mood: String?): DataResult<Unit> {
        return when (val journalResult = createJournalUseCase(title, content, "shared", partnerId, mood)) {
            is DataResult.Success -> {
                assignmentsRepository.completeAssignment(assignmentId)
            }
            is DataResult.Error -> {
                journalResult
            }
        }
    }
}
