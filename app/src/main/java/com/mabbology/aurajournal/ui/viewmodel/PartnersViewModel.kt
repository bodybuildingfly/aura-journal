package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.Partner
import com.mabbology.aurajournal.domain.repository.PartnersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartnersState(
    val isLoading: Boolean = false,
    val partners: List<Partner> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PartnersViewModel @Inject constructor(
    private val repository: PartnersRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PartnersState())
    val state: StateFlow<PartnersState> = _state

    init {
        // Start observing the local database for changes immediately.
        observePartners()
        // Also, trigger a one-time sync to fetch the latest data from the remote server.
        syncPartners()
    }

    private fun observePartners() {
        viewModelScope.launch {
            repository.getPartners()
                .catch { e: Throwable ->
                    _state.value = _state.value.copy(error = "Failed to load partners from local cache.")
                }
                .collect { partners ->
                    _state.value = _state.value.copy(partners = partners)
                }
        }
    }

    fun syncPartners() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.syncPartners()
            if (result.isFailure) {
                _state.value = _state.value.copy(error = "Failed to sync partners with the server.")
            }
            _state.value = _state.value.copy(isLoading = false)
        }
    }
}
