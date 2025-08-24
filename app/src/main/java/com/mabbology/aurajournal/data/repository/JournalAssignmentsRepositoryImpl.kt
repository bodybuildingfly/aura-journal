package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import javax.inject.Inject

private const val TAG = "JournalAssignmentsRepo"

class JournalAssignmentsRepositoryImpl @Inject constructor(
    private val client: Client
) : JournalAssignmentsRepository {

    private val databases by lazy { Databases(client) }
    private val account by lazy { Account(client) }

    override suspend fun getPendingAssignments(): Result<List<JournalAssignment>> {
        return try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID, // Ensure this is in Constants.kt
                queries = listOf(
                    Query.equal("submissiveId", user.id),
                    Query.equal("status", "pending")
                )
            )
            val assignments = response.documents.map { document ->
                JournalAssignment(
                    id = document.id,
                    dominantId = document.data["dominantId"] as String,
                    submissiveId = document.data["submissiveId"] as String,
                    prompt = document.data["prompt"] as String,
                    status = document.data["status"] as String,
                    journalId = document.data["journalId"] as? String
                )
            }
            Result.success(assignments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching assignments: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createAssignment(submissiveId: String, prompt: String): Result<Unit> {
        return try {
            val user = account.get()
            val dominantId = user.id

            databases.createDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                documentId = "unique()",
                data = mapOf(
                    "dominantId" to dominantId,
                    "submissiveId" to submissiveId,
                    "prompt" to prompt,
                    "status" to "pending"
                ),
                permissions = listOf(
                    Permission.read(Role.user(dominantId)),
                    Permission.read(Role.user(submissiveId)),
                    Permission.update(Role.user(submissiveId)) // Only submissive can complete it
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating assignment: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun completeAssignment(assignmentId: String, journalId: String): Result<Unit> {
        return try {
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                documentId = assignmentId,
                data = mapOf(
                    "status" to "completed",
                    "journalId" to journalId
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing assignment: ${e.message}", e)
            Result.failure(e)
        }
    }
}
