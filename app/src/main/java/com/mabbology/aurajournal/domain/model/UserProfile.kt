package com.mabbology.aurajournal.domain.model

data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String?,
    val avatarUrl: String?
)
