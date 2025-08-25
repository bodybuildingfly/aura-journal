package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mabbology.aurajournal.domain.model.JournalAssignment

@Entity(tableName = "journal_assignments")
data class JournalAssignmentEntity(
    @PrimaryKey val id: String,
    val dominantId: String,
    val submissiveId: String,
    val prompt: String,
    val status: String,
    val journalId: String?
)

fun JournalAssignmentEntity.toJournalAssignment(): JournalAssignment {
    return JournalAssignment(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        prompt = prompt,
        status = status,
        journalId = journalId
    )
}

fun JournalAssignment.toEntity(): JournalAssignmentEntity {
    return JournalAssignmentEntity(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        prompt = prompt,
        status = status,
        journalId = journalId
    )
}
