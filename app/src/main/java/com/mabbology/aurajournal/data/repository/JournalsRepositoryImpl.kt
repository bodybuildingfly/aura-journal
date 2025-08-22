package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import javax.inject.Inject

private const val TAG = "JournalsRepository"

class JournalsRepositoryImpl @Inject constructor(
    private val client: Client
) : JournalsRepository {

    private val databases by lazy { Databases(client) }
    private val account by lazy { Account(client) }

    override suspend fun getJournalEntries(): Result<List<Journal>> {
        return try {
            val user = account.get()
            val userId = user.id
            Log.d(TAG, "Fetching journal entries for userId: $userId")

            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                queries = listOf(Query.equal("userId", userId))
            )
            Log.d(TAG, "Successfully fetched ${response.documents.size} documents.")
            val journals = response.documents.map { document ->
                Journal(
                    id = document.id,
                    userId = document.data["userId"] as String,
                    title = document.data["title"] as String,
                    content = document.data["content"] as String,
                    // Fetch the new timestamp from the document's metadata
                    createdAt = document.createdAt
                )
            }
            Result.success(journals)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching journal entries: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createJournalEntry(title: String, content: String): Result<Unit> {
        return try {
            val user = account.get()
            val userId = user.id
            Log.d(TAG, "Creating journal entry for userId: $userId with title: $title")

            databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = "unique()",
                data = mapOf(
                    "userId" to userId,
                    "title" to title,
                    "content" to content
                ),
                permissions = listOf(
                    Permission.read(Role.user(userId)),
                    Permission.update(Role.user(userId)),
                    Permission.delete(Role.user(userId)),
                )
            )
            Log.d(TAG, "Successfully created journal entry.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating journal entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getJournalEntry(id: String): Result<Journal?> {
        return try {
            val document = databases.getDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id
            )
            val journal = Journal(
                id = document.id,
                userId = document.data["userId"] as String,
                title = document.data["title"] as String,
                content = document.data["content"] as String,
                createdAt = document.createdAt
            )
            Result.success(journal)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching single journal entry: ${e.message}", e)
            Result.failure(e)
        }
    }
}
