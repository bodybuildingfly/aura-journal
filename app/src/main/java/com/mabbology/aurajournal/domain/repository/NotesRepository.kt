package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getNotes(): Flow<List<Note>>
    fun getNoteStream(id: String): Flow<Note?>
    suspend fun syncNotes(): DataResult<Unit>
    suspend fun createNote(title: String, content: String, type: String, partnerId: String?): DataResult<Unit>
    suspend fun updateNote(id: String, title: String, content: String): DataResult<Unit>
    suspend fun deleteNote(id: String): DataResult<Unit>
}
