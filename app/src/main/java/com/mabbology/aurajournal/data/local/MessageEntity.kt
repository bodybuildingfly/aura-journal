package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val status: String
)

fun MessageEntity.toMessage(): Message {
    return Message(
        id = id,
        partnershipId = partnershipId,
        senderId = senderId,
        content = content,
        timestamp = timestamp,
        mediaUrl = mediaUrl,
        mediaType = mediaType,
        status = status
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
        status = status
    )
}
