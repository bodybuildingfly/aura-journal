package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import javax.inject.Inject

private const val TAG = "UserProfilesRepository"

class UserProfilesRepositoryImpl @Inject constructor(
    private val client: Client
) : UserProfilesRepository {

    private val databases by lazy { Databases(client) }
    private val account by lazy { Account(client) }

    override suspend fun getUserProfiles(): Result<List<UserProfile>> {
        return try {
            val currentUser = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID
            )
            val profiles = response.documents
                .map { document ->
                    UserProfile(
                        userId = document.data["userId"] as String,
                        displayName = document.data["displayName"] as String,
                        avatarUrl = document.data["avatarUrl"] as? String
                    )
                }
                .filter { it.userId != currentUser.id }

            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchUserProfiles(query: String): Result<List<UserProfile>> {
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
                Result.failure(profilesResult.exceptionOrNull() ?: Exception("Unknown error searching profiles"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserProfile(userId: String, displayName: String): Result<Unit> {
        Log.d(TAG, "Attempting to create profile for userId: $userId")
        return try {
            databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID,
                documentId = userId,
                data = mapOf(
                    "userId" to userId,
                    "displayName" to displayName
                )
            )
            Log.d(TAG, "Successfully created profile for userId: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return try {
            val user = account.get()
            val document = databases.getDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID,
                documentId = user.id
            )
            val userProfile = UserProfile(
                userId = document.data["userId"] as String,
                displayName = document.data["displayName"] as String,
                avatarUrl = document.data["avatarUrl"] as? String
            )
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val document = databases.getDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID,
                documentId = userId
            )
            val userProfile = UserProfile(
                userId = document.data["userId"] as String,
                displayName = document.data["displayName"] as String,
                avatarUrl = document.data["avatarUrl"] as? String
            )
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
