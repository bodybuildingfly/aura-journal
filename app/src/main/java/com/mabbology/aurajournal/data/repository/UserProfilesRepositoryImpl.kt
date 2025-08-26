package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "UserProfilesRepository"

class UserProfilesRepositoryImpl @Inject constructor(
    private val databases: Databases, // Now injects Databases directly
    private val account: Account,      // And Account directly
    private val dispatcherProvider: DispatcherProvider
) : UserProfilesRepository {

    override suspend fun getUserProfiles(): DataResult<List<UserProfile>> = withContext(dispatcherProvider.io) {
        try {
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

            DataResult.Success(profiles)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun searchUserProfiles(query: String): DataResult<List<UserProfile>> = withContext(dispatcherProvider.io) {
        try {
            when (val profilesResult = getUserProfiles()) {
                is DataResult.Success -> {
                    val profiles = profilesResult.data
                    val filteredProfiles = if (query.isBlank()) {
                        profiles
                    } else {
                        profiles.filter {
                            it.displayName.contains(query, ignoreCase = true)
                        }
                    }
                    DataResult.Success(filteredProfiles)
                }
                is DataResult.Error -> {
                    DataResult.Error(profilesResult.exception)
                }
            }
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun createUserProfile(userId: String, displayName: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "Attempting to create profile for userId: $userId")
        try {
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
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile: ${e.message}", e)
            DataResult.Error(e)
        }
    }

    override suspend fun getCurrentUserProfile(): DataResult<UserProfile?> = withContext(dispatcherProvider.io) {
        try {
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
            DataResult.Success(userProfile)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun getUserProfile(userId: String): DataResult<UserProfile?> = withContext(dispatcherProvider.io) {
        try {
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
            DataResult.Success(userProfile)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
