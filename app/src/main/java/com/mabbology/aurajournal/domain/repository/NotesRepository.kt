package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getNotes(): Flow<List<Note>>
    fun getNoteStream(id: String): Flow<Note?>
    suspend fun syncNotes(): Result<Unit>
    suspend fun createNote(title: String, content: String, type: String, partnerId: String?): Result<Unit>
    suspend fun updateNote(id: String, title: String, content: String): Result<Unit>
    suspend fun deleteNote(id: String): Result<Unit>
}
