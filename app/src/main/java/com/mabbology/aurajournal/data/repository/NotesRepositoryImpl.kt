package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.repository.NotesRepository
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import javax.inject.Inject

private const val TAG = "NotesRepository"

class NotesRepositoryImpl @Inject constructor(
    private val client: Client
) : NotesRepository {

    private val databases by lazy { Databases(client) }
    private val account by lazy { Account(client) }
    private val functions by lazy { Functions(client) }

    override suspend fun getNotes(): Result<List<Note>> {
        return try {
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
            Result.success(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notes: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createNote(title: String, content: String, type: String, partnerId: String?): Result<Unit> {
        return try {
            val user = account.get()
            val ownerId = user.id

            val documentData = mutableMapOf<String, Any>(
                "ownerId" to ownerId,
                "title" to title,
                "content" to content,
                "type" to type
            )
            partnerId?.let { documentData["partnerId"] = it }

            if (type == "shared" && partnerId != null) {
                // Use the server function for shared entries
                val permissions = listOf(
                    Permission.read(Role.user(ownerId)),
                    Permission.read(Role.user(partnerId)),
                    Permission.update(Role.user(ownerId)),
                    Permission.update(Role.user(partnerId)),
                    Permission.delete(Role.user(ownerId)),
                    Permission.delete(Role.user(partnerId))
                )
                val payload = Gson().toJson(mapOf(
                    "databaseId" to AppwriteConstants.DATABASE_ID,
                    "collectionId" to AppwriteConstants.NOTES_COLLECTION_ID,
                    "documentData" to documentData,
                    "permissions" to permissions
                ))
                functions.createExecution(
                    functionId = AppwriteConstants.CREATE_SHARED_DOCUMENT_FUNCTION_ID,
                    body = payload
                )
            } else {
                // Create personal entries directly
                databases.createDocument(
                    databaseId = AppwriteConstants.DATABASE_ID,
                    collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                    documentId = "unique()",
                    data = documentData,
                    permissions = listOf(
                        Permission.read(Role.user(ownerId)),
                        Permission.update(Role.user(ownerId)),
                        Permission.delete(Role.user(ownerId))
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating note: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getNote(id: String): Result<Note?> {
        return try {
            val document = databases.getDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                documentId = id
            )
            val note = Note(
                id = document.id,
                ownerId = document.data["ownerId"] as String,
                partnerId = document.data["partnerId"] as? String,
                title = document.data["title"] as String,
                content = document.data["content"] as String,
                type = document.data["type"] as String
            )
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(id: String, title: String, content: String): Result<Unit> {
        return try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
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

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            databases.deleteDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.NOTES_COLLECTION_ID,
                documentId = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
