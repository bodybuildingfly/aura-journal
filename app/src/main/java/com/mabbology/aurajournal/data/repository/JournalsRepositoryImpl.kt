package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.JournalDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toJournal
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

private const val TAG = "JournalsRepositoryImpl"

class JournalsRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val journalDao: JournalDao,
    private val dispatcherProvider: DispatcherProvider
) : JournalsRepository {

    override fun getJournalEntries(): Flow<List<Journal>> {
        return journalDao.getJournals().map { entities ->
            entities.map { it.toJournal() }
        }
    }

    override fun getJournalEntryStream(id: String): Flow<Journal?> {
        return journalDao.getJournalByIdStream(id).map { it?.toJournal() }
    }

    override suspend fun getRemoteJournals(): DataResult<List<Journal>> = withContext(dispatcherProvider.io) {
        try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                queries = listOf(
                    Query.or(
                        listOf(
                            Query.equal("userId", user.id),
                            Query.equal("partnerId", user.id)
                        )
                    )
                )
            )
            val journals = response.documents.map { document ->
                Journal(
                    id = document.id,
                    userId = document.data["userId"] as String,
                    title = document.data["title"] as String,
                    content = document.data["content"] as String,
                    createdAt = document.createdAt,
                    type = document.data["type"] as String,
                    partnerId = document.data["partnerId"] as? String,
                    mood = document.data["mood"] as? String
                )
            }
            DataResult.Success(journals)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            DataResult.Error(e)
        }
    }

    override suspend fun clearLocalJournals() = withContext(dispatcherProvider.io) {
        journalDao.clearJournals()
    }

    override suspend fun upsertLocalJournals(journals: List<Journal>) = withContext(dispatcherProvider.io) {
        journalDao.upsertJournals(journals.map { it.toEntity() })
    }

    override suspend fun getLocalJournalById(id: String): Journal? = withContext(dispatcherProvider.io) {
        journalDao.getJournalById(id)?.toJournal()
    }

    override suspend fun insertLocalJournal(journal: Journal) = withContext(dispatcherProvider.io) {
        journalDao.upsertJournals(listOf(journal.toEntity()))
    }

    override suspend fun deleteLocalJournalById(id: String) = withContext(dispatcherProvider.io) {
        journalDao.deleteJournalById(id)
    }

    override suspend fun createRemoteJournal(journal: Journal): DataResult<Journal> {
        // This function now implements the optimistic update.
        // It returns immediately with the temporary journal and syncs in the background.
        val tempId = journal.id

        CoroutineScope(dispatcherProvider.io).launch {
            try {
                val user = account.get()
                val documentData = mutableMapOf<String, Any>(
                    "userId" to user.id,
                    "title" to journal.title,
                    "content" to journal.content,
                    "type" to journal.type
                )
                journal.partnerId?.let { documentData["partnerId"] = it }
                journal.mood?.let { documentData["mood"] = it }

                val permissions = if (journal.type == "shared" && journal.partnerId != null) {
                    setOf(
                        Permission.read(Role.user(user.id)),
                        Permission.update(Role.user(user.id)),
                        Permission.delete(Role.user(user.id)),
                        Permission.read(Role.user(journal.partnerId)),
                        Permission.update(Role.user(journal.partnerId))
                    ).toList()
                } else {
                    listOf(
                        Permission.read(Role.user(user.id)),
                        Permission.update(Role.user(user.id)),
                        Permission.delete(Role.user(user.id))
                    )
                }

                val payload = mapOf(
                    "databaseId" to AppwriteConstants.DATABASE_ID,
                    "collectionId" to AppwriteConstants.JOURNALS_COLLECTION_ID,
                    "documentData" to documentData,
                    "permissions" to permissions
                )

                val execution = functions.createExecution(
                    functionId = AppwriteConstants.CREATE_DOCUMENT_FUNCTION_ID,
                    body = Gson().toJson(payload)
                )

                if (execution.status == "completed") {
                    val responseBody = execution.responseBody
                    val responseType = object : TypeToken<Map<String, Any>>() {}.type
                    val responseMap: Map<String, Any> = Gson().fromJson(responseBody, responseType)

                    if (responseMap["success"] == true) {
                        @Suppress("UNCHECKED_CAST")
                        val documentMap = responseMap["document"] as? Map<String, Any>
                        val newDocumentId = documentMap?.get("\$id") as? String
                        val newDocumentCreatedAt = documentMap?.get("\$createdAt") as? String

                        if (newDocumentId != null && newDocumentCreatedAt != null) {
                            val finalJournal = journal.copy(id = newDocumentId, createdAt = newDocumentCreatedAt)
                            // Atomically replace the temporary entry with the final one
                            deleteLocalJournalById(tempId)
                            insertLocalJournal(finalJournal)
                        } else {
                            throw Exception("Server function response is missing document details.")
                        }
                    } else {
                        val message = responseMap["message"] as? String ?: "Unknown error from server function"
                        throw Exception("Server function failed: $message")
                    }
                } else {
                    throw Exception("Function execution failed with status: ${execution.status}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background journal sync: ${e.message}", e)
                // If anything fails, remove the temporary local entry to avoid orphaned data.
                deleteLocalJournalById(tempId)
            }
        }

        // Return immediately with the temporary journal to unblock the UI.
        return DataResult.Success(journal)
    }

    override suspend fun updateRemoteJournal(id: String, title: String, content: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val data = mapOf<String, Any>(
                "title" to title,
                "content" to content
            )
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id,
                data = data
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun deleteRemoteJournal(id: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            databases.deleteDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
