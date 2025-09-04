package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.model.Partner
import kotlinx.coroutines.flow.Flow

interface JournalsRepository {
    fun getJournalEntries(partner: Partner?): Flow<List<Journal>>
    fun getJournalEntryStream(id: String): Flow<Journal?>
    suspend fun getRemoteJournals(): DataResult<List<Journal>>
    suspend fun clearLocalJournals()
    suspend fun upsertLocalJournals(journals: List<Journal>)
    suspend fun getLocalJournalById(id: String): Journal?
    suspend fun insertLocalJournal(journal: Journal)
    suspend fun deleteLocalJournalById(id: String)
    suspend fun createRemoteJournal(journal: Journal): DataResult<Journal>
    suspend fun updateRemoteJournal(id: String, title: String, content: String): DataResult<Unit>
    suspend fun deleteRemoteJournal(id: String): DataResult<Unit>
}
