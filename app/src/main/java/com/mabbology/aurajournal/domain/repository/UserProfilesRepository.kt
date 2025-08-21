package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.UserProfile

interface UserProfilesRepository {
    suspend fun getUserProfiles(): Result<List<UserProfile>>
    suspend fun searchUserProfiles(query: String): Result<List<UserProfile>>
}
