package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Upsert
    suspend fun upsertNotes(notes: List<NoteEntity>)


    @Query("SELECT * FROM notes WHERE partnerId IS NULL ORDER BY id DESC")
    fun getPersonalNotes(): Flow<List<NoteEntity>>


    @Query("SELECT * FROM notes WHERE (ownerId = :user1Id AND partnerId = :user2Id) OR (ownerId = :user2Id AND partnerId = :user1Id)")
    fun getSharedNotes(user1Id: String, user2Id: String): Flow<List<NoteEntity>>

    // New function to observe a single note for reactive updates
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdStream(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    // New function to delete a single note by its ID
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)
}
