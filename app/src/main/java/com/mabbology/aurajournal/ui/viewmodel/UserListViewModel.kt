package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.UserProfile
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListState(
    val isLoading: Boolean = false,
    val users: List<UserProfile> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val userProfilesRepository: UserProfilesRepository
) : ViewModel() {

    private val _userListState = MutableStateFlow(UserListState())
    val userListState: StateFlow<UserListState> = _userListState

    init {
        loadUserProfiles()
    }

    private fun loadUserProfiles() {
        viewModelScope.launch {
            _userListState.value = UserListState(isLoading = true)
            val result = userProfilesRepository.getUserProfiles()
            _userListState.value = when {
                result.isSuccess -> UserListState(users = result.getOrNull() ?: emptyList())
                else -> UserListState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun searchUserProfiles(query: String) {
        viewModelScope.launch {
            _userListState.value = UserListState(isLoading = true)
            val result = userProfilesRepository.searchUserProfiles(query)
            _userListState.value = when {
                result.isSuccess -> UserListState(users = result.getOrNull() ?: emptyList())
                else -> UserListState(error = result.exceptionOrNull()?.message)
            }
        }
    }
}
