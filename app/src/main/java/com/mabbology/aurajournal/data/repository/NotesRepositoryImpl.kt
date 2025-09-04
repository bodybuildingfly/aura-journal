package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun getNotes(partner: com.mabbology.aurajournal.domain.model.Partner?): Flow<List<Note>> {
        val flow = if (partner == null) {
            noteDao.getPersonalNotes()
        } else {
            noteDao.getSharedNotes(partner.dominantId, partner.submissiveId)
        }
        return flow.map { entities ->
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
                            Query.equal("ownerId", listOf(user.id)),
                            Query.equal("partnerId", listOf(user.id))
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

    override suspend fun createNote(title: String, content: String, type: String, partnerId: String?): DataResult<Note> {
        val user = account.get()
        val tempId = "local_${UUID.randomUUID()}"
        val newNote = Note(
            id = tempId,
            ownerId = user.id,
            partnerId = partnerId,
            title = title,
            content = content,
            type = type
        )

        noteDao.upsertNotes(listOf(newNote.toEntity()))

        CoroutineScope(dispatcherProvider.io).launch {
            try {
                val documentData = mutableMapOf<String, Any>(
                    "ownerId" to user.id,
                    "title" to title,
                    "content" to content,
                    "type" to type
                )
                partnerId?.let { documentData["partnerId"] = it }

                val permissions = if (type == "shared" && partnerId != null) {
                    setOf(
                        Permission.read(Role.user(user.id)),
                        Permission.update(Role.user(user.id)),
                        Permission.delete(Role.user(user.id)),
                        Permission.read(Role.user(partnerId)),
                        Permission.update(Role.user(partnerId))
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
                    "collectionId" to AppwriteConstants.NOTES_COLLECTION_ID,
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

                        if (newDocumentId != null) {
                            val finalNote = newNote.copy(id = newDocumentId)
                            noteDao.deleteNoteById(tempId)
                            noteDao.upsertNotes(listOf(finalNote.toEntity()))
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
                Log.e(TAG, "Error in background note sync: ${e.message}", e)
                noteDao.deleteNoteById(tempId)
            }
        }
        return DataResult.Success(newNote)
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
