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

            // Fetch journals where the user is the author OR the partner
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                queries = listOf(
                    Query.or(
                        listOf(
                            Query.equal("userId", userId),
                            Query.equal("partnerId", userId)
                        )
                    )
                )
            )
            Log.d(TAG, "Successfully fetched ${response.documents.size} documents.")
            val journals = response.documents.map { document ->
                Journal(
                    id = document.id,
                    userId = document.data["userId"] as String,
                    title = document.data["title"] as String,
                    content = document.data["content"] as String,
                    createdAt = document.createdAt,
                    type = document.data["type"] as String,
                    partnerId = document.data["partnerId"] as? String
                )
            }
            Result.success(journals)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching journal entries: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createJournalEntry(title: String, content: String, type: String, partnerId: String?): Result<Unit> {
        return try {
            val user = account.get()
            val userId = user.id

            val data = mutableMapOf<String, Any>(
                "userId" to userId,
                "title" to title,
                "content" to content,
                "type" to type
            )
            partnerId?.let { data["partnerId"] = it }

            val permissions = if (type == "shared" && partnerId != null) {
                listOf(
                    Permission.read(Role.user(userId)),
                    Permission.read(Role.user(partnerId)),
                    Permission.update(Role.user(userId)),
                    Permission.update(Role.user(partnerId)),
                    Permission.delete(Role.user(userId)),
                    Permission.delete(Role.user(partnerId))
                )
            } else {
                listOf(
                    Permission.read(Role.user(userId)),
                    Permission.update(Role.user(userId)),
                    Permission.delete(Role.user(userId))
                )
            }

            databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = "unique()",
                data = data,
                permissions = permissions
            )
            Result.success(Unit)
        } catch (e: Exception) {
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
                createdAt = document.createdAt,
                type = document.data["type"] as String,
                partnerId = document.data["partnerId"] as? String
            )
            Result.success(journal)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching single journal entry: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateJournalEntry(id: String, title: String, content: String): Result<Unit> {
        return try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id,
                data = mapOf(
                    "title" to title,
                    "content" to content
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(id: String): Result<Unit> {
        return try {
            databases.deleteDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
