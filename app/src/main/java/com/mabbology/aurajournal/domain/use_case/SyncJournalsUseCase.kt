package com.mabbology.aurajournal.domain.use_case.journal

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import javax.inject.Inject

class SyncJournalsUseCase @Inject constructor(
    private val repository: JournalsRepository
) {
    suspend operator fun invoke(): DataResult<Unit> {
        return when (val result = repository.getRemoteJournals()) {
            is DataResult.Success -> {
                try {
                    repository.clearLocalJournals()
                    repository.upsertLocalJournals(result.data)
                    DataResult.Success(Unit)
                } catch (e: Exception) {
                    DataResult.Error(e)
                }
            }
            is DataResult.Error -> {
                result
            }
        }
    }
}
