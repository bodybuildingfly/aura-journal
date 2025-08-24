package com.mabbology.aurajournal.domain.model

data class JournalAssignment(
    val id: String,
    val dominantId: String,
    val submissiveId: String,
    val prompt: String,
    val status: String,
    val journalId: String? = null
)
