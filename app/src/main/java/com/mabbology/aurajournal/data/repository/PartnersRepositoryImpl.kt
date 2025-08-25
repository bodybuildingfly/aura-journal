package com.mabbology.aurajournal.data.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.data.local.PartnerDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toPartner
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Partner
import com.mabbology.aurajournal.domain.repository.PartnersRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PartnersRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val account: Account,
    private val userProfilesRepository: UserProfilesRepository,
    private val partnerDao: PartnerDao
) : PartnersRepository {

    override fun getPartners(): Flow<List<Partner>> {
        return partnerDao.getPartners().map { entities ->
            entities.map { it.toPartner() }
        }
    }

    override suspend fun syncPartners(): DataResult<Unit> {
        return try {
            val user = account.get()
            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNERS_COLLECTION_ID,
                queries = listOf(
                    Query.or(
                        listOf(
                            Query.equal("dominantId", user.id),
                            Query.equal("submissiveId", user.id)
                        )
                    )
                )
            )

            val remotePartners = response.documents.mapNotNull { doc ->
                val dominantId = doc.data["dominantId"] as String
                val submissiveId = doc.data["submissiveId"] as String

                val dominantProfileResult = userProfilesRepository.getUserProfile(dominantId)
                val submissiveProfileResult = userProfilesRepository.getUserProfile(submissiveId)

                if (dominantProfileResult is DataResult.Success && submissiveProfileResult is DataResult.Success) {
                    val dominantName = dominantProfileResult.data?.displayName ?: "Unknown"
                    val submissiveName = submissiveProfileResult.data?.displayName ?: "Unknown"

                    Partner(
                        id = doc.id,
                        dominantId = dominantId,
                        submissiveId = submissiveId,
                        dominantName = dominantName,
                        submissiveName = submissiveName
                    )
                } else {
                    null
                }
            }
            partnerDao.clearPartners()
            partnerDao.upsertPartners(remotePartners.map { it.toEntity() })
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
