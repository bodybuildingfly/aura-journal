package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.model.Partner
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getNotes(partner: Partner?): Flow<List<Note>>
    fun getNoteStream(id: String): Flow<Note?>
    suspend fun syncNotes(): DataResult<Unit>
    suspend fun createNote(title: String, content: String, type: String, partnerId: String?): DataResult<Note>
    suspend fun updateNote(id: String, title: String, content: String): DataResult<Unit>
    suspend fun deleteNote(id: String): DataResult<Unit>
}
