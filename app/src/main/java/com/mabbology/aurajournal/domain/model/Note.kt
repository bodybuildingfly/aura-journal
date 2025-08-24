package com.mabbology.aurajournal.domain.model

data class Note(
    val id: String,
    val ownerId: String,
    val partnerId: String?,
    val title: String,
    val content: String,
    val type: String
)