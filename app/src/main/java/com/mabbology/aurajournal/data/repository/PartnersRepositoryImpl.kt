package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.data.local.PartnerDao
import com.mabbology.aurajournal.data.local.toEntity
import com.mabbology.aurajournal.data.local.toPartner
import com.mabbology.aurajournal.di.AppwriteConstants
import com.mabbology.aurajournal.domain.model.Partner
import com.mabbology.aurajournal.domain.repository.PartnersRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import com.google.gson.Gson
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PartnersRepositoryImpl @Inject constructor(
    private val databases: Databases,
    private val functions: Functions,
    private val account: Account,
    private val userProfilesRepository: UserProfilesRepository,
    private val partnerDao: PartnerDao,
    private val dispatcherProvider: DispatcherProvider
) : PartnersRepository {

    private val TAG = "PartnersRepositoryImpl"

    override fun getPartners(): Flow<List<Partner>> {
        Log.d(TAG, "getPartners: Fetching partners from local DAO")
        return partnerDao.getPartners().map { entities ->
            Log.d(TAG, "getPartners: Found ${entities.size} partners in local DAO")
            entities.map { it.toPartner() }
        }
    }

    override suspend fun syncPartners(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "syncPartners: Starting partner sync")
        try {
            val user = account.get()
            Log.d(TAG, "syncPartners: Current user ID: ${user.id}")

            val queries = listOf(
                Query.or(
                    listOf(
                        Query.equal("dominantId", user.id),
                        Query.equal("submissiveId", user.id)
                    )
                )
            )
            Log.d(TAG, "syncPartners: Querying Appwrite for partners with queries: $queries")

            val response = databases.listDocuments(
                databaseId = AppwriteConstants.DATABASE_ID,
                collectionId = AppwriteConstants.PARTNERS_COLLECTION_ID,
                queries = queries
            )

            Log.d(TAG, "syncPartners: Appwrite response received. Found ${response.total} documents.")

            val remotePartners = response.documents.mapNotNull { doc ->
                Log.d(TAG, "syncPartners: Processing document ${doc.id}")
                val dominantId = doc.data["dominantId"] as String
                val submissiveId = doc.data["submissiveId"] as String
                Log.d(TAG, "syncPartners: Dominant ID: $dominantId, Submissive ID: $submissiveId")

                Log.d(TAG, "syncPartners: Fetching dominant profile for ID $dominantId")
                val dominantProfileResult = userProfilesRepository.getUserProfile(dominantId)
                Log.d(TAG, "syncPartners: Fetching submissive profile for ID $submissiveId")
                val submissiveProfileResult = userProfilesRepository.getUserProfile(submissiveId)

                if (dominantProfileResult is DataResult.Success && submissiveProfileResult is DataResult.Success) {
                    val dominantName = dominantProfileResult.data?.displayName ?: "Unknown"
                    val submissiveName = submissiveProfileResult.data?.displayName ?: "Unknown"
                    Log.d(TAG, "syncPartners: Successfully fetched profiles. Dominant: $dominantName, Submissive: $submissiveName")

                    Partner(
                        id = doc.id,
                        dominantId = dominantId,
                        submissiveId = submissiveId,
                        dominantName = dominantName,
                        submissiveName = submissiveName
                    )
                } else {
                    Log.e(TAG, "syncPartners: Failed to fetch one or both user profiles for document ${doc.id}")
                    if (dominantProfileResult is DataResult.Error) {
                        Log.e(TAG, "syncPartners: Dominant profile fetch error: ${dominantProfileResult.exception}")
                    }
                    if (submissiveProfileResult is DataResult.Error) {
                        Log.e(TAG, "syncPartners: Submissive profile fetch error: ${submissiveProfileResult.exception}")
                    }
                    null
                }
            }
            Log.d(TAG, "syncPartners: Mapped ${remotePartners.size} remote partners.")

            Log.d(TAG, "syncPartners: Clearing local partners table.")
            partnerDao.clearPartners()
            Log.d(TAG, "syncPartners: Inserting ${remotePartners.size} partners into local DAO.")
            partnerDao.upsertPartners(remotePartners.map { it.toEntity() })
            Log.d(TAG, "syncPartners: Partner sync completed successfully.")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncPartners: An error occurred during partner sync", e)
            DataResult.Error(e)
        }
    }

    override suspend fun removePartner(partner: Partner): DataResult<Unit> = withContext(dispatcherProvider.io) {
        Log.d(TAG, "removePartner: Removing partner with ID ${partner.id}")
        try {
            val payload = mapOf(
                "partnerId" to partner.id,
                "dominantId" to partner.dominantId,
                "submissiveId" to partner.submissiveId
            )
            val jsonPayload = Gson().toJson(payload)

            functions.createExecution(
                functionId = AppwriteConstants.REMOVE_PARTNER_FUNCTION_ID,
                body = jsonPayload
            )
            Log.d(TAG, "removePartner: Successfully triggered removePartner function on Appwrite.")

            // After successful execution, sync partners to reflect the changes locally
            syncPartners()

            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "removePartner: An error occurred while removing partner", e)
            DataResult.Error(e)
        }
    }
}
