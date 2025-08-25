package com.mabbology.aurajournal.data.repository

import android.util.Log
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import io.appwrite.services.Account
import javax.inject.Inject

private const val TAG = "AuthRepository"

class AuthRepositoryImpl @Inject constructor(
    private val account: Account, // Now injects Account directly
    private val userProfilesRepository: UserProfilesRepository
) : AuthRepository {

    override suspend fun checkSessionStatus(): Result<Unit> {
        return try {
            account.get()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<Unit> {
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

            if (profileResult.isFailure) {
                Log.e(TAG, "Profile creation failed after registration.")
                return Result.failure(profileResult.exceptionOrNull() ?: Exception("Profile creation failed."))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during registration: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            account.createEmailPasswordSession(
                email = email,
                password = password
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            account.deleteSession("current")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(password: String, oldPassword: String): Result<Unit> {
        return try {
            account.updatePassword(password = password, oldPassword = oldPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
