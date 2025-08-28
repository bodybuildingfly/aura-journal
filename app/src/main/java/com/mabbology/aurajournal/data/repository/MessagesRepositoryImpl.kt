package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.MessageDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toMessage
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Message
import com.mabbology.aurajournal.domain.repository.MessagesRepository
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.models.InputFile
import io.appwrite.models.RealtimeSubscription
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import io.appwrite.services.Realtime
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.util.Date
import java.util.UUID
import javax.inject.Inject

private const val TAG = "MessagesRepositoryImpl"

class MessagesRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val storage: Storage,
    private val functions: Functions,
    private val realtime: Realtime,
    private val messageDao: MessageDao,
    private val dispatcherProvider: DispatcherProvider
) : MessagesRepository {

    private var subscription: RealtimeSubscription? = null

    override fun getMessages(partnershipId: String): Flow<List<Message>> {
        return messageDao.getMessagesForPartnership(partnershipId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    override suspend fun syncMessages(partnershipId: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.MESSAGES_COLLECTION_ID,
                queries = listOf(Query.equal("partnershipId", partnershipId))
            )
            val messages = response.documents.map {
                val odt = OffsetDateTime.parse(it.data["timestamp"] as String)
                val timestamp = Date.from(odt.toInstant())
                Message(
                    id = it.id,
                    partnershipId = it.data["partnershipId"] as String,
                    senderId = it.data["senderId"] as String,
                    content = it.data["content"] as String,
                    timestamp = timestamp,
                    mediaUrl = it.data["mediaUrl"] as? String,
                    mediaType = it.data["mediaType"] as? String,
                    status = "sent"
                )
            }
            messageDao.upsertMessages(messages.map { it.toEntity() })
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun sendMessage(partnershipId: String, partnerId: String, content: String, mediaFile: File?, mediaType: String?) {
        val currentUser = account.get()
        val tempId = "local_${UUID.randomUUID()}"
        val tempMessage = Message(
            id = tempId,
            partnershipId = partnershipId,
            senderId = currentUser.id,
            content = content,
            timestamp = Date(),
            mediaUrl = mediaFile?.path,
            mediaType = mediaType,
            status = "sending"
        )

        messageDao.upsertMessages(listOf(tempMessage.toEntity()))

        CoroutineScope(dispatcherProvider.io).launch {
            var mediaUrl: String? = null
            var uploadSuccessful = true

            if (mediaFile != null) {
                try {
                    Log.d(TAG, "Starting file upload directly to Storage...")
                    val fileId = UUID.randomUUID().toString()
                    val inputFile = InputFile.fromFile(mediaFile)
                    val fileResponse = storage.createFile(
                        bucketId = AppwriteConstants.STORAGE_BUCKET_ID,
                        fileId = fileId,
                        file = inputFile,
                        permissions = listOf(Permission.read(Role.any())),
                        onProgress = { progress ->
                            Log.d(TAG, "File upload progress: ${progress.progress}%")
                        }
                    )
                    mediaUrl = fileResponse.id
                    Log.d(TAG, "File upload successful. File ID: $mediaUrl")
                } catch (e: Exception) {
                    Log.e(TAG, "File upload failed.", e)
                    uploadSuccessful = false
                    messageDao.upsertMessages(listOf(tempMessage.copy(status = "failed").toEntity()))
                }
            }

            if (uploadSuccessful) {
                try {
                    Log.d(TAG, "Creating message document via function...")
                    val documentData = mutableMapOf<String, Any>(
                        "partnershipId" to partnershipId,
                        "senderId" to currentUser.id,
                        "content" to content,
                        "timestamp" to OffsetDateTime.now().toString()
                    )
                    mediaUrl?.let {
                        documentData["mediaUrl"] = it
                        documentData["mediaType"] = mediaType ?: ""
                    }

                    val permissions = listOf(
                        "read(\"user:${currentUser.id}\")",
                        "read(\"user:$partnerId\")"
                    )

                    val payload = mapOf(
                        "databaseId" to AppwriteConstants.DATABASE_ID,
                        "collectionId" to AppwriteConstants.MESSAGES_COLLECTION_ID,
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
                            val newDocumentTimestamp = (documentMap?.get("timestamp") as? String)
                                ?: OffsetDateTime.now().toString()

                            if (newDocumentId != null) {
                                val odt = OffsetDateTime.parse(newDocumentTimestamp)
                                val finalTimestamp = Date.from(odt.toInstant())

                                val finalMessage = tempMessage.copy(
                                    id = newDocumentId,
                                    timestamp = finalTimestamp,
                                    mediaUrl = mediaUrl,
                                    status = "sent"
                                )

                                messageDao.deleteMessageById(tempId)
                                messageDao.upsertMessages(listOf(finalMessage.toEntity()))
                                Log.d(TAG, "Message document created successfully.")
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
                    Log.e(TAG, "Message document creation failed.", e)
                    messageDao.upsertMessages(listOf(tempMessage.copy(status = "failed").toEntity()))
                }
            }
        }
    }

    override fun subscribeToMessages() {
        val channel = "databases.${AppwriteConstants.DATABASE_ID}.collections.${AppwriteConstants.MESSAGES_COLLECTION_ID}.documents"
        Log.d(TAG, "Subscribing to real-time channel: $channel")
        subscription = realtime.subscribe(channel) {
            Log.d(TAG, "Real-time event received: ${it.events}")
            if (it.events.contains("databases.*.collections.*.documents.*.create")) {
                Log.d(TAG, "New message created event received. Payload: ${it.payload}")
                CoroutineScope(dispatcherProvider.io).launch {
                    @Suppress("UNCHECKED_CAST")
                    val payload = it.payload as Map<String, Any>
                    val odt = OffsetDateTime.parse(payload["timestamp"] as String)
                    val timestamp = Date.from(odt.toInstant())
                    val newMessage = Message(
                        id = payload["\$id"] as String,
                        partnershipId = payload["partnershipId"] as String,
                        senderId = payload["senderId"] as String,
                        content = payload["content"] as String,
                        timestamp = timestamp,
                        mediaUrl = payload["mediaUrl"] as? String,
                        mediaType = payload["mediaType"] as? String,
                        status = "sent"
                    )
                    Log.d(TAG, "Upserting new message from real-time event into local DB: ${newMessage.id}")
                    messageDao.upsertMessages(listOf(newMessage.toEntity()))
                }
            }
        }
    }

    override fun closeSubscription() {
        subscription?.close()
    }
}
