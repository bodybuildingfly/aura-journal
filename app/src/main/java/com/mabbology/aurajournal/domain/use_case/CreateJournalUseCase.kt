package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

class CreateJournalUseCase @Inject constructor(
    private val repository: JournalsRepository
) {
    suspend operator fun invoke(title: String, content: String, type: String, partnerId: String?, mood: String?): DataResult<Unit> {
        val tempId = "local_${UUID.randomUUID()}"
        val newJournal = Journal(
            id = tempId,
            userId = "", // This will be filled in by the repository from the account
            title = title,
            content = content,
            createdAt = OffsetDateTime.now().toString(),
            type = type,
            partnerId = partnerId,
            mood = mood
        )

        repository.insertLocalJournal(newJournal)

        return when (val result = repository.createRemoteJournal(newJournal)) {
            is DataResult.Success -> {
                repository.deleteLocalJournalById(tempId)
                repository.insertLocalJournal(result.data)
                DataResult.Success(Unit)
            }
            is DataResult.Error -> {
                repository.deleteLocalJournalById(tempId)
                DataResult.Error(result.exception)
            }
        }
    }
}
