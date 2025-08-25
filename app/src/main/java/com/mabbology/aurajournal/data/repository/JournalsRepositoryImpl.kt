package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.core.util.DataResult
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

private const val TAG = "JournalsRepositoryImpl"

class JournalsRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val journalDao: JournalDao
) : JournalsRepository {

    override fun getJournalEntries(): Flow<List<Journal>> {
        return journalDao.getJournals().map { entities ->
            entities.map { it.toJournal() }
        }
    }

    override fun getJournalEntryStream(id: String): Flow<Journal?> {
        return journalDao.getJournalByIdStream(id).map { it?.toJournal() }
    }

    override suspend fun getRemoteJournals(): DataResult<List<Journal>> {
        return try {
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

    override suspend fun clearLocalJournals() {
        journalDao.clearJournals()
    }

    override suspend fun upsertLocalJournals(journals: List<Journal>) {
        journalDao.upsertJournals(journals.map { it.toEntity() })
    }

    override suspend fun getLocalJournalById(id: String): Journal? {
        return journalDao.getJournalById(id)?.toJournal()
    }

    override suspend fun insertLocalJournal(journal: Journal) {
        journalDao.upsertJournals(listOf(journal.toEntity()))
    }

    override suspend fun deleteLocalJournalById(id: String) {
        journalDao.deleteJournalById(id)
    }

    override suspend fun createRemoteJournal(journal: Journal): DataResult<Journal> {
        return try {
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
                listOf(
                    Permission.read(Role.user(user.id)),
                    Permission.update(Role.user(user.id)),
                    Permission.delete(Role.user(user.id)),
                    Permission.read(Role.user(journal.partnerId)),
                    Permission.update(Role.user(journal.partnerId))
                )
            } else {
                listOf(
                    Permission.read(Role.user(user.id)),
                    Permission.update(Role.user(user.id)),
                    Permission.delete(Role.user(user.id))
                )
            }

            val newDocument = databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = "unique()",
                data = documentData,
                permissions = permissions
            )
            val finalJournal = journal.copy(id = newDocument.id, createdAt = newDocument.createdAt)
            DataResult.Success(finalJournal)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun updateRemoteJournal(id: String, title: String, content: String): DataResult<Unit> {
        return try {
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

    override suspend fun deleteRemoteJournal(id: String): DataResult<Unit> {
        return try {
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
