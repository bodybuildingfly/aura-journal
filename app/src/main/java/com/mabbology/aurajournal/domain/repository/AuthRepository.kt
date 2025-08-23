package com.mabbology.aurajournal.domain.repository

interface AuthRepository {
    suspend fun checkSessionStatus(): Result<Unit>
    suspend fun register(email: String, password: String, name: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun updatePassword(password: String, oldPassword: String): Result<Unit>
}
