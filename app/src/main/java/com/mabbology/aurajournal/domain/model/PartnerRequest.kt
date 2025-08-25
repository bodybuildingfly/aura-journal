package com.mabbology.aurajournal.domain.model

data class PartnerRequest(
    val id: String,
    val dominantId: String,
    val submissiveId: String,
    val status: String,
    val counterpartyName: String
)
