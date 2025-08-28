package com.mabbology.aurajournal.domain.model

import java.util.Date

data class Message(
    val id: String,
    val partnershipId: String,
    val senderId: String,
    val content: String,
    val timestamp: Date,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val status: String,
    val deletedFor: List<String> = emptyList()
)