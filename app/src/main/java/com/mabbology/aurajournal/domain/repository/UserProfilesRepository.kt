package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.UserProfile

interface UserProfilesRepository {
    suspend fun getUserProfiles(): DataResult<List<UserProfile>>
    suspend fun searchUserProfiles(query: String): DataResult<List<UserProfile>>
    suspend fun createUserProfile(userId: String, displayName: String, email: String): DataResult<Unit>
    suspend fun getCurrentUserProfile(): DataResult<UserProfile?>
    suspend fun getUserProfile(userId: String): DataResult<UserProfile?>
}
