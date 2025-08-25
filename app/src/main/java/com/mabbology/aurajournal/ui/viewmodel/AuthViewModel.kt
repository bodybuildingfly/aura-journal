package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
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
            when (authRepository.checkSessionStatus()) {
                is DataResult.Success -> _authState.value = AuthState(isAuthenticated = true, isLoading = false)
                is DataResult.Error -> _authState.value = AuthState(isAuthenticated = false, isLoading = false)
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            when (val result = authRepository.register(email, password, name)) {
                is DataResult.Success -> _authState.value = AuthState(isAuthenticated = true)
                is DataResult.Error -> _authState.value = AuthState(error = result.exception.message)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            when (val result = authRepository.login(email, password)) {
                is DataResult.Success -> _authState.value = AuthState(isAuthenticated = true)
                is DataResult.Error -> _authState.value = AuthState(error = result.exception.message)
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
