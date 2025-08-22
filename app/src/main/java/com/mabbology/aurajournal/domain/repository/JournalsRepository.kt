package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Journal

interface JournalsRepository {
    suspend fun getJournalEntries(): Result<List<Journal>>
    suspend fun createJournalEntry(title: String, content: String): Result<Unit>
}
