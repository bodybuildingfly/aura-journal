package com.mabbology.aurajournal.data.repository

import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.core.util.DispatcherProvider
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.services.Account
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val account: Account, // Now injects Account directly
    private val userProfilesRepository: UserProfilesRepository,
    private val dispatcherProvider: DispatcherProvider
) : AuthRepository {

    override suspend fun getCurrentUserId(): String? = withContext(dispatcherProvider.io) {
        try {
            account.get().id
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun checkSessionStatus(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            account.get()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            val user = account.create(
                userId = "unique()",
                email = email,
                password = password,
                name = name
            )

            account.createEmailPasswordSession(
                email = email,
                password = password
            )

            val profileResult = userProfilesRepository.createUserProfile(userId = user.id, displayName = name, email = email)

            when (profileResult) {
                is DataResult.Error -> {
                    return@withContext DataResult.Error(profileResult.exception)
                }
                is DataResult.Success -> {
                    // All good
                }
            }

            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun login(email: String, password: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            try {
                account.deleteSession("current")
            } catch (_: Exception) {
                // Ignore exceptions, as this will fail if no session exists, which is fine.
            }

            account.createEmailPasswordSession(
                email = email,
                password = password
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun logout(): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            account.deleteSession("current")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun updatePassword(password: String, oldPassword: String): DataResult<Unit> = withContext(dispatcherProvider.io) {
        try {
            account.updatePassword(password = password, oldPassword = oldPassword)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
