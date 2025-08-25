package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
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

    override suspend fun syncJournalEntries(): Result<Unit> {
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
            Log.d(TAG, "Sync successful. Found ${journals.size} journals. Clearing local cache.")
            journalDao.clearJournals()
            journalDao.upsertJournals(journals.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun createJournalEntry(title: String, content: String, type: String, partnerId: String?, mood: String?): Result<Unit> {
        val user = account.get()
        val documentData = mutableMapOf<String, Any>(
            "userId" to user.id,
            "title" to title,
            "content" to content,
            "type" to type
        )
        partnerId?.let { documentData["partnerId"] = it }
        mood?.let { documentData["mood"] = it }

        if (type == "shared" && partnerId != null) {
            return try {
                // ... shared entry logic
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val tempId = "local_${UUID.randomUUID()}"
        val newJournal = Journal(
            id = tempId,
            userId = user.id,
            title = title,
            content = content,
            createdAt = OffsetDateTime.now().toString(),
            type = type,
            partnerId = null,
            mood = mood
        )
        Log.d(TAG, "Optimistic Create: Inserting temporary local journal with id $tempId")
        journalDao.upsertJournals(listOf(newJournal.toEntity()))

        return try {
            val newDocument = databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = "unique()",
                data = documentData,
                permissions = listOf(
                    Permission.read(Role.user(user.id)),
                    Permission.update(Role.user(user.id)),
                    Permission.delete(Role.user(user.id))
                )
            )
            val finalJournal = newJournal.copy(id = newDocument.id, createdAt = newDocument.createdAt)
            Log.d(TAG, "Remote create successful. Replacing temp journal $tempId with final id ${newDocument.id}")
            journalDao.deleteJournalById(tempId)
            journalDao.upsertJournals(listOf(finalJournal.toEntity()))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
            journalDao.deleteJournalById(tempId)
            Result.failure(e)
        }
    }

    override suspend fun updateJournalEntry(id: String, title: String, content: String): Result<Unit> {
        val originalJournalEntity = journalDao.getJournalById(id) ?: return Result.failure(Exception("Journal not found"))
        val updatedJournalEntity = originalJournalEntity.copy(title = title, content = content)

        Log.d(TAG, "Optimistic Update: Updating local journal with id $id")
        journalDao.upsertJournals(listOf(updatedJournalEntity))

        return try {
            //
            // CORRECTED DATA TYPE HERE
            //
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
            Log.d(TAG, "Remote update successful for journal $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote update failed. Rolling back local update for $id. Error: ${e.message}")
            journalDao.upsertJournals(listOf(originalJournalEntity))
            Result.failure(e)
        }
    }

    override suspend fun deleteJournalEntry(id: String): Result<Unit> {
        val originalJournalEntity = journalDao.getJournalById(id) ?: return Result.failure(Exception("Journal not found"))

        Log.d(TAG, "Optimistic Delete: Deleting local journal with id $id")
        journalDao.deleteJournalById(id)

        return try {
            databases.deleteDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNALS_COLLECTION_ID,
                documentId = id
            )
            Log.d(TAG, "Remote delete successful for journal $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote delete failed. Rolling back local delete for $id. Error: ${e.message}")
            journalDao.upsertJournals(listOf(originalJournalEntity))
            Result.failure(e)
        }
    }
}
