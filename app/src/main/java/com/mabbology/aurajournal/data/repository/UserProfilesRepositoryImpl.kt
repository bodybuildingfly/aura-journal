package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Query
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
        // Return an empty list by default to encourage searching
        DataResult.Success(emptyList())
    }

    override suspend fun searchUserProfiles(query: String): DataResult<List<UserProfile>> = withContext(dispatcherProvider.io) {
        if (query.isBlank()) {
            return@withContext DataResult.Success(emptyList())
        }
        try {
            val currentUser = account.get()
            val queries = listOf(Query.equal("email", listOf(query)))

            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID,
                queries = queries
            )

            val profiles = response.documents
                .map { document ->
                    UserProfile(
                        userId = document.data["userId"] as String,
                        displayName = document.data["displayName"] as String,
                        email = document.data["email"] as? String,
                        avatarUrl = document.data["avatarUrl"] as? String
                    )
                }
                .filter { it.userId != currentUser.id }

            DataResult.Success(profiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user profiles: ${e.message}", e)
            DataResult.Error(e)
        }
    }

    override suspend fun createUserProfile(userId: String, displayName: String, email: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "Attempting to create profile for userId: $userId")
        try {
            databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.USER_PROFILES_COLLECTION_ID,
                documentId = userId,
                data = mapOf(
                    "userId" to userId,
                    "displayName" to displayName,
                    "email" to email
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
                email = document.data["email"] as? String,
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
                email = document.data["email"] as? String,
                avatarUrl = document.data["avatarUrl"] as? String
            )
            DataResult.Success(userProfile)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
