package com.mabbology.aurajournal.domain.model

data class Journal(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val createdAt: String
)