package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.repository.AuthRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val userId: String = "",
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
            when (val result = userProfilesRepository.getCurrentUserProfile()) {
                is DataResult.Success -> {
                    val profile = result.data
                    _profileState.value = ProfileState(
                        userId = profile?.userId ?: "",
                        displayName = profile?.displayName ?: "User"
                    )
                }
                is DataResult.Error -> _profileState.value = ProfileState(error = "Failed to load profile.")
            }
        }
    }

    fun updatePassword(password: String, oldPassword: String) {
        viewModelScope.launch {
            when (authRepository.updatePassword(password, oldPassword)) {
                is DataResult.Success -> _profileState.value = _profileState.value.copy(successMessage = "Password updated successfully!")
                is DataResult.Error -> _profileState.value = _profileState.value.copy(error = "Failed to update password.")
            }
        }
    }
}