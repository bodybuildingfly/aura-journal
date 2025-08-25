package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.services.Account
import javax.inject.Inject

private const val TAG = "AuthRepository"

class AuthRepositoryImpl @Inject constructor(
    private val account: Account, // Now injects Account directly
    private val userProfilesRepository: UserProfilesRepository
) : AuthRepository {

    override suspend fun checkSessionStatus(): DataResult<Unit> {
        return try {
            account.get()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): DataResult<Unit> {
        return try {
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
            Log.d(TAG, "User registered and session created. Now creating profile...")

            val profileResult = userProfilesRepository.createUserProfile(userId = user.id, displayName = name)

            when (profileResult) {
                is DataResult.Error -> {
                    Log.e(TAG, "Profile creation failed after registration.")
                    return DataResult.Error(profileResult.exception)
                }
                is DataResult.Success -> {
                    // All good
                }
            }

            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during registration: ${e.message}", e)
            DataResult.Error(e)
        }
    }

    override suspend fun login(email: String, password: String): DataResult<Unit> {
        return try {
            account.createEmailPasswordSession(
                email = email,
                password = password
            )
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun logout(): DataResult<Unit> {
        return try {
            account.deleteSession("current")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    override suspend fun updatePassword(password: String, oldPassword: String): DataResult<Unit> {
        return try {
            account.updatePassword(password = password, oldPassword = oldPassword)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
