package com.mabbology.aurajournal.data.repository

import com.mabbology.aurajournal.domain.repository.AuthRepository
import io.appwrite.Client
import io.appwrite.services.Account
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val client: Client
) : AuthRepository {

    private val account by lazy { Account(client) }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            account.create(
                userId = "unique()",
                email = email,
                password = password
            )
            // After registration, we should also create a session to log the user in
            account.createEmailPasswordSession( // Changed here
                email = email,
                password = password
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            account.createEmailPasswordSession( // And here
                email = email,
                password = password
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
