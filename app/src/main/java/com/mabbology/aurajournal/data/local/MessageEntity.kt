package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mabbology.aurajournal.domain.model.Message
import java.util.Date

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val partnershipId: String,
    val senderId: String,
    val content: String,
    val timestamp: Date,
    val mediaUrl: String?,
    val mediaType: String?,
    val status: String,
    val deletedFor: String
)

fun MessageEntity.toMessage(): Message {
    val deletedForList = try {
        Gson().fromJson<List<String>>(deletedFor, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
    return Message(
        id = id,
        partnershipId = partnershipId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        status = status,
        deletedFor = deletedForList
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        partnershipId = partnershipId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        status = status,
        deletedFor = Gson().toJson(deletedFor)
    )
}