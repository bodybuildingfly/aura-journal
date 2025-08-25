package com.mabbology.aurajournal.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {

    // Inserts or updates partners. Replaces existing ones based on the primary key.
    @Upsert
    suspend fun upsertPartners(partners: List<PartnerEntity>)

    // Gets all partners and returns them as a Flow. The UI will automatically
    // update whenever the data in this table changes.
    @Query("SELECT * FROM partners")
    fun getPartners(): Flow<List<PartnerEntity>>

    // Deletes all partners from the table.
    @Query("DELETE FROM partners")
    suspend fun clearPartners()
}
