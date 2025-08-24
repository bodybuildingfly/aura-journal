package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Note

interface NotesRepository {
    suspend fun getNotes(): Result<List<Note>>
    suspend fun createNote(title: String, content: String, type: String, partnerId: String?): Result<Unit>
    suspend fun getNote(id: String): Result<Note?>
    suspend fun updateNote(id: String, title: String, content: String): Result<Unit>
    suspend fun deleteNote(id: String): Result<Unit>
}
