package com.mabbology.aurajournal.domain.model

import java.util.Date

data class JournalAssignment(
    val id: String,
    val dominantId: String,
    val submissiveId: String,
    val prompt: String,
    val status: String,
    val journalId: String? = null,
    val createdAt: Date
)
