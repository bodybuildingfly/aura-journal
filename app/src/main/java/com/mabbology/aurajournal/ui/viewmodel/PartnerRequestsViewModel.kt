package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.PartnerRequest
import com.mabbology.aurajournal.domain.repository.PartnerRequestsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartnerRequestsState(
    val isLoading: Boolean = false,
    val incomingRequests: List<PartnerRequest> = emptyList(),
    val outgoingRequests: List<PartnerRequest> = emptyList(),
    val error: String? = null,
    val requestApproved: Boolean = false,
    val approvingRequestId: String? = null
)

@HiltViewModel
class PartnerRequestsViewModel @Inject constructor(
    private val repository: PartnerRequestsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PartnerRequestsState())
    val state: StateFlow<PartnerRequestsState> = _state

    init {
        observeRequests()
        syncRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            val incomingFlow = repository.getIncomingRequests()
            val outgoingFlow = repository.getOutgoingRequests()

            combine(incomingFlow, outgoingFlow) { incoming, outgoing ->
                _state.value = _state.value.copy(
                    incomingRequests = incoming,
                    outgoingRequests = outgoing
                )
            }.catch { e: Throwable ->
                _state.value = _state.value.copy(error = "Failed to load requests from local cache.")
            }.collect {}
        }
    }

    fun syncRequests() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.syncRequests()
            if (result.isFailure) {
                _state.value = _state.value.copy(error = "Failed to sync requests with the server.")
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun approveRequest(request: PartnerRequest) {
        viewModelScope.launch {
            _state.update { it.copy(approvingRequestId = request.id) }
            val result = repository.approveRequest(request)
            if (result.isSuccess) {
                _state.update { it.copy(requestApproved = true) }
                syncRequests()
            } else {
                _state.update { it.copy(error = "Failed to approve request.", approvingRequestId = null) }
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectRequest(requestId)
            syncRequests()
        }
    }

    fun onApprovalHandled() {
        _state.update { it.copy(requestApproved = false, approvingRequestId = null) }
    }
}
