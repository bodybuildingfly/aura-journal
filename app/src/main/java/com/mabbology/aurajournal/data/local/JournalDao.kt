package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Upsert
    suspend fun upsertJournals(journals: List<JournalEntity>)

    @Query("SELECT * FROM journals ORDER BY createdAt DESC")
    fun getJournals(): Flow<List<JournalEntity>>

    // This function now returns a Flow to observe a single journal for changes
    @Query("SELECT * FROM journals WHERE id = :id")
    fun getJournalByIdStream(id: String): Flow<JournalEntity?>

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getJournalById(id: String): JournalEntity?

    @Query("DELETE FROM journals")
    suspend fun clearJournals()

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteJournalById(id: String)
}
