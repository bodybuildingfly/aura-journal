package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.PartnerRequest
import com.mabbology.aurajournal.domain.repository.PartnerRequestsRepository
import com.mabbology.aurajournal.domain.repository.PartnersRepository
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
    private val repository: PartnerRequestsRepository,
    private val partnersRepository: PartnersRepository
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
            when (repository.syncRequests()) {
                is DataResult.Error -> _state.value = _state.value.copy(error = "Failed to sync requests with the server.")
                is DataResult.Success -> {
                    // No-op
                }
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun approveRequest(request: PartnerRequest) {
        viewModelScope.launch {
            _state.update { it.copy(approvingRequestId = request.id) }
            when (repository.approveRequest(request)) {
                is DataResult.Success -> {
                    _state.update { it.copy(requestApproved = true) }
                    partnersRepository.syncPartners()
                }
                is DataResult.Error -> _state.update { it.copy(error = "Failed to approve request.", approvingRequestId = null) }
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
