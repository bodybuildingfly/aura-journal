package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.UserProfile

interface UserProfilesRepository {
    suspend fun getUserProfiles(): Result<List<UserProfile>>
    suspend fun searchUserProfiles(query: String): Result<List<UserProfile>>
    suspend fun createUserProfile(userId: String, displayName: String): Result<Unit>
    suspend fun getCurrentUserProfile(): Result<UserProfile?>
    suspend fun getUserProfile(userId: String): Result<UserProfile?>
}
