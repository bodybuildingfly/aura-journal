package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Journal
import kotlinx.coroutines.flow.Flow

interface JournalsRepository {
    fun getJournalEntries(): Flow<List<Journal>>
    // New function to get a reactive stream for a single journal
    fun getJournalEntryStream(id: String): Flow<Journal?>
    suspend fun syncJournalEntries(): Result<Unit>
    suspend fun createJournalEntry(title: String, content: String, type: String, partnerId: String?, mood: String?): Result<Unit>
    suspend fun updateJournalEntry(id: String, title: String, content: String): Result<Unit>
    suspend fun deleteJournalEntry(id: String): Result<Unit>
}
