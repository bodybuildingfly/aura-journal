package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val displayName: String = "",
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userProfilesRepository: UserProfilesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState(isLoading = true)
            val result = userProfilesRepository.getCurrentUserProfile()
            _profileState.value = when {
                result.isSuccess -> {
                    val profile = result.getOrNull()
                    ProfileState(displayName = profile?.displayName ?: "User")
                }
                else -> ProfileState(error = "Failed to load profile.")
            }
        }
    }

    fun updatePassword(password: String, oldPassword: String) {
        viewModelScope.launch {
            val result = authRepository.updatePassword(password, oldPassword)
            _profileState.value = when {
                result.isSuccess -> _profileState.value.copy(successMessage = "Password updated successfully!")
                else -> _profileState.value.copy(error = "Failed to update password.")
            }
        }
    }
}
