package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Journal
import kotlinx.coroutines.flow.Flow

interface JournalsRepository {
    fun getJournalEntries(): Flow<List<Journal>>
    fun getJournalEntryStream(id: String): Flow<Journal?>
    suspend fun syncJournalEntries(): DataResult<Unit>
    suspend fun createJournalEntry(title: String, content: String, type: String, partnerId: String?, mood: String?): DataResult<Unit>
    suspend fun updateJournalEntry(id: String, title: String, content: String): DataResult<Unit>
    suspend fun deleteJournalEntry(id: String): DataResult<Unit>
}
