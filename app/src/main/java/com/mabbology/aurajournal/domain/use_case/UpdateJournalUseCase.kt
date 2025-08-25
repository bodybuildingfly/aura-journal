package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import javax.inject.Inject

class UpdateJournalUseCase @Inject constructor(
    private val repository: JournalsRepository
) {
    suspend operator fun invoke(id: String, title: String, content: String): DataResult<Unit> {
        val originalJournal = repository.getLocalJournalById(id)
            ?: return DataResult.Error(Exception("Journal not found"))

        val updatedJournal = originalJournal.copy(title = title, content = content)
        repository.insertLocalJournal(updatedJournal)

        return when (val result = repository.updateRemoteJournal(id, title, content)) {
            is DataResult.Success -> {
                DataResult.Success(Unit)
            }
            is DataResult.Error -> {
                repository.insertLocalJournal(originalJournal) // Roll back
                DataResult.Error(result.exception)
            }
        }
    }
}
