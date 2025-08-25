package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mabbology.aurajournal.domain.model.PartnerRequest

@Entity(tableName = "partner_requests")
data class PartnerRequestEntity(
    @PrimaryKey val id: String,
    val dominantId: String,
    val submissiveId: String,
    val status: String,
    val counterpartyName: String
)

fun PartnerRequestEntity.toPartnerRequest(): PartnerRequest {
    return PartnerRequest(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        status = status,
        counterpartyName = counterpartyName
    )
}

fun PartnerRequest.toEntity(): PartnerRequestEntity {
    return PartnerRequestEntity(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        status = status,
        counterpartyName = counterpartyName
    )
}
