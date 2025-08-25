package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalAssignmentState(
    val isLoading: Boolean = false,
    val assignments: List<JournalAssignment> = emptyList(),
    val error: String? = null,
    val assignmentCreated: Boolean = false
)

@HiltViewModel
class JournalAssignmentViewModel @Inject constructor(
    private val repository: JournalAssignmentsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JournalAssignmentState())
    val state: StateFlow<JournalAssignmentState> = _state

    init {
        observeAssignments()
        syncAssignments()
    }

    private fun observeAssignments() {
        viewModelScope.launch {
            repository.getPendingAssignments()
                .catch { e -> _state.update { it.copy(error = "Failed to load assignments from cache.") } }
                .collect { assignments -> _state.update { it.copy(assignments = assignments) } }
        }
    }

    fun syncAssignments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = repository.syncAssignments()
            if (result.isFailure) {
                _state.update { it.copy(error = "Failed to sync assignments.") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun createAssignment(submissiveId: String, prompt: String) {
        // Immediately signal success for optimistic UI update and navigation.
        _state.value = _state.value.copy(isLoading = true, assignmentCreated = true)

        // Launch the actual save operation in the background.
        viewModelScope.launch {
            val result = repository.createAssignment(submissiveId, prompt)
            if (result.isFailure) {
                // If the background save fails, notify the user.
                _state.update { it.copy(error = "Failed to create assignment. It has been removed.") }
            }
            // Reset the loading and created flags when the operation is complete.
            _state.update { it.copy(isLoading = false, assignmentCreated = false) }
        }
    }
}
