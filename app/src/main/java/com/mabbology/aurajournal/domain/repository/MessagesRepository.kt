package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Message
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MessagesRepository {
    fun getMessages(partnershipId: String): Flow<List<Message>>
    suspend fun syncMessages(partnershipId: String): DataResult<Unit>
    suspend fun sendMessage(partnershipId: String, partnerId: String, content: String, mediaFile: File? = null, mediaType: String? = null)
    fun subscribeToMessages()
    fun closeSubscription()
}
