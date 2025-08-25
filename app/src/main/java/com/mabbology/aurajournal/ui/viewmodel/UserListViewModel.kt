package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.PartnerRequestsRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val error: String? = null,
    val requestSentMessage: String? = null
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userProfilesRepository: UserProfilesRepository,
    private val partnerRequestsRepository: PartnerRequestsRepository
) : ViewModel() {

    private val _userListState = MutableStateFlow(UserListState())
    val userListState: StateFlow<UserListState> = _userListState

    init {
        loadUserProfiles()
    }

    private fun loadUserProfiles() {
        viewModelScope.launch {
            _userListState.value = UserListState(isLoading = true)
            when (val result = userProfilesRepository.getUserProfiles()) {
                is DataResult.Success -> _userListState.value = UserListState(users = result.data)
                is DataResult.Error -> _userListState.value = UserListState(error = result.exception.message)
            }
        }
    }

    fun searchUserProfiles(query: String) {
        viewModelScope.launch {
            _userListState.value = _userListState.value.copy(isLoading = true)
            when (val result = userProfilesRepository.searchUserProfiles(query)) {
                is DataResult.Success -> _userListState.value = _userListState.value.copy(isLoading = false, users = result.data)
                is DataResult.Error -> _userListState.value = _userListState.value.copy(isLoading = false, error = result.exception.message)
            }
        }
    }

    fun sendApplication(dominantId: String) {
        viewModelScope.launch {
            when (val currentUserResult = userProfilesRepository.getCurrentUserProfile()) {
                is DataResult.Success -> {
                    val submissiveId = currentUserResult.data?.userId
                    if (submissiveId == null) {
                        _userListState.update { it.copy(error = "Could not identify current user.") }
                        return@launch
                    }

                    // The repository now handles the optimistic update.
                    // We just call the function and trust the UI to react.
                    when (partnerRequestsRepository.sendPartnerRequest(dominantId, submissiveId)) {
                        is DataResult.Success -> {
                            _userListState.update { it.copy(requestSentMessage = "Application sent!") }
                        }
                        is DataResult.Error -> {
                            _userListState.update { it.copy(error = "Failed to send application.") }
                        }
                    }
                }
                is DataResult.Error -> {
                    _userListState.update { it.copy(error = "Could not fetch current user profile.") }
                }
            }
        }
    }

    fun clearRequestSentMessage() {
        _userListState.update { it.copy(requestSentMessage = null) }
    }
}
