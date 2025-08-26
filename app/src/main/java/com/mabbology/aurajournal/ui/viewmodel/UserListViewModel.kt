package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.PartnerRequestsRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L) // Wait for 500ms of inactivity
                .distinctUntilChanged() // Only search if the text has changed
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        // Clear the user list when the query is empty
                        _userListState.update { it.copy(users = emptyList(), isLoading = false) }
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _userListState.update { it.copy(isLoading = true) }
            when (val result = userProfilesRepository.searchUserProfiles(query)) {
                is DataResult.Success -> _userListState.update { it.copy(isLoading = false, users = result.data) }
                is DataResult.Error -> _userListState.update { it.copy(isLoading = false, error = result.exception.message) }
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
