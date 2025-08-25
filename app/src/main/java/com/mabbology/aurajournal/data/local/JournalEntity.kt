package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mabbology.aurajournal.domain.model.Journal

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val type: String,
    val partnerId: String?,
    val mood: String?
)

fun JournalEntity.toJournal(): Journal {
    return Journal(
        id = id,
        userId = userId,
        title = title,
        content = content,
        createdAt = createdAt,
        type = type,
        partnerId = partnerId,
        mood = mood
    )
}

fun Journal.toEntity(): JournalEntity {
    return JournalEntity(
        id = id,
        userId = userId,
        title = title,
        content = content,
        createdAt = createdAt,
        type = type,
        partnerId = partnerId,
        mood = mood
    )
}
