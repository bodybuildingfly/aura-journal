package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.domain.model.Partner
import kotlinx.coroutines.flow.Flow

interface PartnersRepository {
    // This will now return a Flow, which allows the UI to automatically
    // update whenever the local database changes.
    fun getPartners(): Flow<List<Partner>>

    // This function will trigger a background sync to fetch the latest
    // data from Appwrite and update the local database.
    suspend fun syncPartners(): Result<Unit>
}
