package com.mabbology.aurajournal.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Message
import com.mabbology.aurajournal.domain.repository.MessagesRepository
import com.mabbology.aurajournal.domain.repository.UserProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChatState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val error: String? = null,
    val partnerName: String = "",
    val currentUserId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val userProfilesRepository: UserProfilesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    private val partnershipId: String = savedStateHandle.get<String>("partnershipId")!!
    private val partnerId: String = savedStateHandle.get<String>("partnerId")!!

    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        loadPartnerInfo()
        observeMessages()
        syncMessages()
        loadCurrentUser()
        messagesRepository.subscribeToMessages()
    }

    override fun onCleared() {
        super.onCleared()
        messagesRepository.closeSubscription()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = userProfilesRepository.getCurrentUserProfile()) {
                is DataResult.Success -> {
                    _state.update { it.copy(currentUserId = result.data?.userId ?: "") }
                }
                is DataResult.Error -> {
                    // Handle error
                }
            }
        }
    }

    private fun loadPartnerInfo() {
        viewModelScope.launch {
            when (val result = userProfilesRepository.getUserProfile(partnerId)) {
                is DataResult.Success -> {
                    _state.update { it.copy(partnerName = result.data?.displayName ?: "Partner") }
                }
                is DataResult.Error -> {
                    _state.update { it.copy(error = "Failed to load partner information.") }
                }
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            messagesRepository.getMessages(partnershipId)
                .catch { _ -> _state.update { it.copy(error = "Failed to load messages.") } }
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                }
        }
    }

    fun syncMessages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = messagesRepository.syncMessages(partnershipId)) {
                is DataResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to sync messages: ${result.exception.message}") }
                }
                is DataResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun sendMessage(content: String, mediaFile: File? = null, mediaType: String? = null) {
        viewModelScope.launch {
            messagesRepository.sendMessage(partnershipId, partnerId, content, mediaFile, mediaType)
        }
    }
}
