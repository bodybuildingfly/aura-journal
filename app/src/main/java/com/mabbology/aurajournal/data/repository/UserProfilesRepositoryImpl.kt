package com.mabbology.aurajournal.data.repository

import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Client
import io.appwrite.services.Databases
import javax.inject.Inject
import kotlin.collections.filter

// Note: Replace "YOUR_DATABASE_ID" and "YOUR_USER_PROFILES_COLLECTION_ID"
// with your actual IDs from your Appwrite project console.

class UserProfilesRepositoryImpl @Inject constructor(
    private val client: Client
) : UserProfilesRepository {

    private val databases by lazy { Databases(client) }

    override suspend fun getUserProfiles(): Result<List<UserProfile>> {
        return try {
            val response = databases.listDocuments(
                databaseId = "YOUR_DATABASE_ID",
                collectionId = "YOUR_USER_PROFILES_COLLECTION_ID"
            )
            val profiles = response.documents.map { document ->
                UserProfile(
                    userId = document.id,
                    displayName = document.data["displayName"] as String,
                    avatarUrl = document.data["avatarUrl"] as? String
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUserProfiles(query: String): Result<List<UserProfile>> {
        // Appwrite's client-side SDK doesn't have a direct text search query.
        // A common approach is to filter the full list on the client side for small datasets,
        // or use Appwrite Functions for more complex server-side queries.
        // For this example, we will fetch all and then filter.
        return try {
            val profilesResult = getUserProfiles()
            if (profilesResult.isSuccess) {
                val profiles = profilesResult.getOrThrow()
                val filteredProfiles = if (query.isBlank()) {
                    profiles
                } else {
                    profiles.filter {
                        it.displayName.contains(query, ignoreCase = true)
                    }
                }
                Result.success(filteredProfiles)
            } else {
                Result.failure(profilesResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
