package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Journal

interface JournalsRepository {
    suspend fun getJournalEntries(): Result<List<Journal>>
    suspend fun createJournalEntry(title: String, content: String, type: String, partnerId: String?): Result<Unit>
    suspend fun getJournalEntry(id: String): Result<Journal?>
    suspend fun updateJournalEntry(id: String, title: String, content: String): Result<Unit>
    suspend fun deleteJournalEntry(id: String): Result<Unit>
}
