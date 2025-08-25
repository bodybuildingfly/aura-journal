package com.mabbology.aurajournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mabbology.aurajournal.domain.model.Partner

@Entity(tableName = "partners")
data class PartnerEntity(
    @PrimaryKey val id: String,
    val dominantId: String,
    val submissiveId: String,
    val dominantName: String,
    val submissiveName: String
)

// Extension function to map from the database entity to the domain model
fun PartnerEntity.toPartner(): Partner {
    return Partner(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        dominantName = dominantName,
        submissiveName = submissiveName
    )
}

// Extension function to map from the domain model to the database entity
fun Partner.toEntity(): PartnerEntity {
    return PartnerEntity(
        id = id,
        dominantId = dominantId,
        submissiveId = submissiveId,
        dominantName = dominantName,
        submissiveName = submissiveName
    )
}
