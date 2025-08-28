package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE partnershipId = :partnershipId ORDER BY timestamp ASC")
    fun getMessagesForPartnership(partnershipId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE partnershipId = :partnershipId")
    suspend fun clearMessagesForPartnership(partnershipId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)
}
