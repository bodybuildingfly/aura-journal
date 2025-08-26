package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Partner
import kotlinx.coroutines.flow.Flow

interface PartnersRepository {
    fun getPartners(): Flow<List<Partner>>
    suspend fun syncPartners(): DataResult<Unit>
    suspend fun removePartner(partner: Partner): DataResult<Unit>
}
