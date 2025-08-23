package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState(isLoading = true))
    val authState: StateFlow<AuthState> = _authState

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val result = authRepository.checkSessionStatus()
            _authState.value = when {
                result.isSuccess -> AuthState(isAuthenticated = true, isLoading = false)
                else -> AuthState(isAuthenticated = false, isLoading = false)
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val result = authRepository.register(email, password, name)
            _authState.value = when {
                result.isSuccess -> AuthState(isAuthenticated = true)
                else -> AuthState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val result = authRepository.login(email, password)
            _authState.value = when {
                result.isSuccess -> AuthState(isAuthenticated = true)
                else -> AuthState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState(isAuthenticated = false)
        }
    }
}
