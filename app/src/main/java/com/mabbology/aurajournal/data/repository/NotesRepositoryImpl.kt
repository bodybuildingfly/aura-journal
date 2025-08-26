package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.NoteDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toNote
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.repository.NotesRepository
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject

private const val TAG = "NotesRepositoryImpl"

class NotesRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val noteDao: NoteDao,
    private val dispatcherProvider: DispatcherProvider
) : NotesRepository {

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getNotes().map { entities ->
            entities.map { it.toNote() }
        }
    }

    override fun getNoteStream(id: String): Flow<Note?> {
        return noteDao.getNoteByIdStream(id).map { it?.toNote() }
    }

    override suspend fun syncNotes(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                queries = listOf(
                    Query.or(
                        listOf(
                            Query.equal("ownerId", user.id),
                            Query.equal("partnerId", user.id)
                        )
                    )
                )
            )
            val notes = response.documents.map { document ->
                Note(
                    id = document.id,
                    ownerId = document.data["ownerId"] as String,
                    partnerId = document.data["partnerId"] as? String,
                    title = document.data["title"] as String,
                    content = document.data["content"] as String,
                    type = document.data["type"] as String
                )
            }
            noteDao.clearNotes()
            noteDao.upsertNotes(notes.map { it.toEntity() })
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun createNote(title: String, content: String, type: String, partnerId: String?): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val user = account.get()
        val documentData = mutableMapOf<String, Any>(
            "ownerId" to user.id,
            "title" to title,
            "content" to content,
            "type" to type
        )
        partnerId?.let { documentData["partnerId"] = it }

        if (type == "shared" && partnerId != null) {
            return@withContext try {
                // ... shared entry logic
                DataResult.Success(Unit)
            } catch (e: Exception) {
                DataResult.Error(e)
            }
        }

        val tempId = "local_${UUID.randomUUID()}"
        val newNote = Note(
            id = tempId,
            ownerId = user.id,
            partnerId = null,
            title = title,
            content = content,
            type = type
        )
        Log.d(TAG, "Optimistic Create: Inserting temporary local note with id $tempId")
        noteDao.upsertNotes(listOf(newNote.toEntity()))

        try {
            val newDocument = databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                documentId = "unique()",
                data = documentData,
                permissions = listOf(
                    Permission.read(Role.user(user.id)),
                    Permission.update(Role.user(user.id)),
                    Permission.delete(Role.user(user.id))
                )
            )
            val finalNote = newNote.copy(id = newDocument.id)
            Log.d(TAG, "Remote create successful. Replacing temp note $tempId with final id ${newDocument.id}")
            noteDao.deleteNoteById(tempId)
            noteDao.upsertNotes(listOf(finalNote.toEntity()))
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
            noteDao.deleteNoteById(tempId)
            DataResult.Error(e)
        }
    }

    override suspend fun updateNote(id: String, title: String, content: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val originalNoteEntity = noteDao.getNoteById(id) ?: return@withContext DataResult.Error(Exception("Note not found"))
        val updatedNoteEntity = originalNoteEntity.copy(title = title, content = content)

        Log.d(TAG, "Optimistic Update: Updating local note with id $id")
        noteDao.upsertNotes(listOf(updatedNoteEntity))

        try {
            val data = mapOf<String, Any>(
                "title" to title,
                "content" to content
            )
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                documentId = id,
                data = data
            )
            Log.d(TAG, "Remote update successful for note $id")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote update failed. Rolling back local update for $id. Error: ${e.message}")
            noteDao.upsertNotes(listOf(originalNoteEntity))
            DataResult.Error(e)
        }
    }

    override suspend fun deleteNote(id: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val originalNoteEntity = noteDao.getNoteById(id) ?: return@withContext DataResult.Error(Exception("Note not found"))

        Log.d(TAG, "Optimistic Delete: Deleting local note with id $id")
        noteDao.deleteNoteById(id)

        try {
            databases.deleteDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                documentId = id
            )
            Log.d(TAG, "Remote delete successful for note $id")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Remote delete failed. Rolling back local delete for $id. Error: ${e.message}")
            noteDao.upsertNotes(listOf(originalNoteEntity))
            DataResult.Error(e)
        }
    }
}
