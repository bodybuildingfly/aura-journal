package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.ConnectionRequest
import com.mabbology.aurajournal.domain.repository.ConnectionRequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionRequestsState(
    val isLoading: Boolean = false,
    val incomingRequests: List<ConnectionRequest> = emptyList(),
    val outgoingRequests: List<ConnectionRequest> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ConnectionRequestsViewModel @Inject constructor(
    private val repository: ConnectionRequestsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionRequestsState())
    val state: StateFlow<ConnectionRequestsState> = _state

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val incomingResult = repository.getIncomingRequests()
            val outgoingResult = repository.getOutgoingRequests()

            _state.value = ConnectionRequestsState(
                isLoading = false,
                incomingRequests = incomingResult.getOrNull() ?: emptyList(),
                outgoingRequests = outgoingResult.getOrNull() ?: emptyList(),
                error = incomingResult.exceptionOrNull()?.message ?: outgoingResult.exceptionOrNull()?.message
            )
        }
    }

    fun approveRequest(request: ConnectionRequest) {
        viewModelScope.launch {
            repository.approveRequest(request)
            loadRequests() // Refresh the lists
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectRequest(requestId)
            loadRequests() // Refresh the lists
        }
    }
}
