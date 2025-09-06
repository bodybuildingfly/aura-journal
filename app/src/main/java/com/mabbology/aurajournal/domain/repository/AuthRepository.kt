package com.mabbology.aurajournal.domain.repository

import com.mabbology.aurajournal.core.util.DataResult

interface AuthRepository {
    suspend fun getCurrentUserId(): String?
    suspend fun checkSessionStatus(): DataResult<Unit>
    suspend fun register(email: String, password: String, name: String): DataResult<Unit>
    suspend fun login(email: String, password: String): DataResult<Unit>
    suspend fun logout(): DataResult<Unit>
    suspend fun updatePassword(password: String, oldPassword: String): DataResult<Unit>
}
