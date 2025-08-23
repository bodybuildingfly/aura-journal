package com.mabbology.aurajournal.domain.model

data class Journal(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val type: String, // "personal" or "shared"
    val partnerId: String? = null // Null for personal entries
)
