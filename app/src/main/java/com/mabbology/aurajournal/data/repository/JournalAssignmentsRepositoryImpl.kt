package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.JournalAssignmentDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toJournalAssignment
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import javax.inject.Inject

private const val TAG = "JournalAssignmentsRepo"

class JournalAssignmentsRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val functions: Functions,
    private val assignmentDao: JournalAssignmentDao,
    private val dispatcherProvider: DispatcherProvider
) : JournalAssignmentsRepository {

    override fun getAssignments(partner: com.mabbology.aurajournal.domain.model.Partner?): Flow<List<JournalAssignment>> {
        return if (partner == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            assignmentDao.getAssignmentsBetweenUsers(partner.dominantId, partner.submissiveId).map { entities ->
                entities.map { it.toJournalAssignment() }
            }
        }
    }

    override suspend fun syncAssignments(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val user = account.get()

            // Fetch assignments where the user is the submissive
            val submissiveAssignmentsResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                queries = listOf(Query.equal("submissiveId", user.id))
            )

            // Fetch assignments where the user is the dominant
            val dominantAssignmentsResponse = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                queries = listOf(Query.equal("dominantId", user.id))
            )

            // Combine and deduplicate the results
            val allDocuments = (submissiveAssignmentsResponse.documents + dominantAssignmentsResponse.documents).distinctBy { it.id }

            val assignments = allDocuments.map { document ->
                val odt = OffsetDateTime.parse(document.createdAt)
                val createdAt = Date.from(odt.toInstant())
                JournalAssignment(
                    id = document.id,
                    dominantId = document.data["dominantId"] as String,
                    submissiveId = document.data["submissiveId"] as String,
                    prompt = document.data["prompt"] as String,
                    status = document.data["status"] as String,
                    journalId = document.data["journalId"] as? String,
                    createdAt = createdAt
                )
            }
            assignmentDao.clearAssignments()
            assignmentDao.upsertAssignments(assignments.map { it.toEntity() })
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun createAssignment(submissiveId: String, prompt: String): DataResult<JournalAssignment> {
        val user = account.get()
        val tempId = "local_${UUID.randomUUID()}"
        val createdAt = Date()
        val newAssignment = JournalAssignment(
            id = tempId,
            dominantId = user.id,
            submissiveId = submissiveId,
            prompt = prompt,
            status = "pending",
            journalId = null,
            createdAt = createdAt
        )

        assignmentDao.upsertAssignments(listOf(newAssignment.toEntity()))

        CoroutineScope(dispatcherProvider.io).launch {
            try {
                val documentData = mapOf(
                    "dominantId" to user.id,
                    "submissiveId" to submissiveId,
                    "prompt" to prompt,
                    "status" to "pending"
                )

                val permissions = listOf(
                    "read(\"user:${user.id}\")",
                    "update(\"user:${user.id}\")",
                    "delete(\"user:${user.id}\")",
                    "read(\"user:$submissiveId\")",
                    "update(\"user:$submissiveId\")"
                )

                val payload = mapOf(
                    "databaseId" to AppwriteConstants.DATABASE_ID,
                    "collectionId" to AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
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
                            val odt = OffsetDateTime.parse(newDocumentCreatedAt)
                            val finalCreatedAt = Date.from(odt.toInstant())
                            val finalAssignment = newAssignment.copy(id = newDocumentId, createdAt = finalCreatedAt)
                            assignmentDao.deleteAssignmentById(tempId)
                            assignmentDao.upsertAssignments(listOf(finalAssignment.toEntity()))
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
                Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
                assignmentDao.deleteAssignmentById(tempId)
            }
        }
        return DataResult.Success(newAssignment)
    }

    override suspend fun completeAssignment(assignmentId: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        // Optimistically remove the assignment from the local database
        val originalAssignmentEntity = assignmentDao.getAssignmentById(assignmentId)
            ?: return@withContext DataResult.Error(Exception("Assignment not found locally"))

        Log.d(TAG, "Optimistic Complete: Deleting local assignment with id $assignmentId")
        assignmentDao.deleteAssignmentById(assignmentId)

        try {
            // Attempt to update the remote database
            databases.updateDocument(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                documentId = assignmentId,
                data = mapOf("status" to "completed")
            )
            Log.d(TAG, "Remote complete successful for assignment $assignmentId")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            // If remote update fails, roll back the local change
            Log.e(TAG, "Remote complete failed. Rolling back local delete for $assignmentId. Error: ${e.message}")
            assignmentDao.upsertAssignments(listOf(originalAssignmentEntity))
            DataResult.Error(e)
        }
    }
}
