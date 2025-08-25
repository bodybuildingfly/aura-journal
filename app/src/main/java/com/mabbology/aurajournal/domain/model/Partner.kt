package com.mabbology.aurajournal.domain.model

data class Partner(
    val id: String,
    val dominantId: String,
    val submissiveId: String,
    val dominantName: String,
    val submissiveName: String
)