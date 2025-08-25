package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetJournalUseCase @Inject constructor(
    private val repository: JournalsRepository
) {
    operator fun invoke(id: String): Flow<Journal?> {
        return repository.getJournalEntryStream(id)
    }
}
