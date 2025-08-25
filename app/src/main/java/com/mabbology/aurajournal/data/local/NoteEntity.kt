package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mabbology.aurajournal.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val partnerId: String?,
    val title: String,
    val content: String,
    val type: String
)

fun NoteEntity.toNote(): Note {
    return Note(
        id = id,
        ownerId = ownerId,
        partnerId = partnerId,
        title = title,
        content = content,
        type = type
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        ownerId = ownerId,
        partnerId = partnerId,
        title = title,
        content = content,
        type = type
    )
}
