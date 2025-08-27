package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.JournalAssignmentDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toJournalAssignment
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val assignmentDao: JournalAssignmentDao,
    private val dispatcherProvider: DispatcherProvider
) : JournalAssignmentsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAssignments(): Flow<List<JournalAssignment>> {
        return flow { emit(account.get().id) }.flatMapLatest { userId ->
            assignmentDao.getAssignments(userId).map { entities ->
                entities.map { it.toJournalAssignment() }
            }
        }
    }

    override suspend fun syncAssignments(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                queries = listOf(
                    Query.equal("submissiveId", user.id)
                )
            )
            val assignments = response.documents.map { document ->
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
                val newDocument = databases.createDocument(
                    databaseId = AppwriteConstants.DATABASE_ID,
                    collectionId = AppwriteConstants.JOURNAL_ASSIGNMENTS_COLLECTION_ID,
                    documentId = "unique()",
                    data = mapOf(
                        "dominantId" to user.id,
                        "submissiveId" to submissiveId,
                        "prompt" to prompt,
                        "status" to "pending"
                    ),
                    permissions = listOf(
                        Permission.read(Role.user(user.id)),
                        Permission.read(Role.user(submissiveId)),
                        Permission.update(Role.user(submissiveId))
                    )
                )
                val odt = OffsetDateTime.parse(newDocument.createdAt)
                val finalCreatedAt = Date.from(odt.toInstant())
                val finalAssignment = newAssignment.copy(id = newDocument.id, createdAt = finalCreatedAt)
                assignmentDao.deleteAssignmentById(tempId)
                assignmentDao.upsertAssignments(listOf(finalAssignment.toEntity()))
            } catch (e: Exception) {
                Log.e(TAG, "Remote create failed. Rolling back local insert for $tempId. Error: ${e.message}")
                assignmentDao.deleteAssignmentById(tempId)
            }
        }
        return DataResult.Success(newAssignment)
    }

    override suspend fun completeAssignment(assignmentId: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        val userId = account.get().id
        // Optimistically remove the assignment from the local database
        val originalAssignmentEntity = assignmentDao.getAssignments(userId)
            .map { list -> list.firstOrNull { it.id == assignmentId } }
            .first() ?: return@withContext DataResult.Error(Exception("Assignment not found locally"))

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
