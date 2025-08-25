package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
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
    val pendingAssignments: List<JournalAssignment> = emptyList(),
    val completedAssignments: List<JournalAssignment> = emptyList(),
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
            repository.getAssignments()
                .catch { _ -> _state.update { it.copy(error = "Failed to load assignments from cache.") } }
                .collect { assignments ->
                    val pending = assignments.filter { it.status == "pending" }.sortedByDescending { it.createdAt }
                    val completed = assignments.filter { it.status == "completed" }.sortedByDescending { it.createdAt }
                    _state.update {
                        it.copy(
                            pendingAssignments = pending,
                            completedAssignments = completed
                        )
                    }
                }
        }
    }

    fun syncAssignments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (repository.syncAssignments()) {
                is DataResult.Error -> _state.update { it.copy(error = "Failed to sync assignments.") }
                is DataResult.Success -> {
                    // No-op
                }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun createAssignment(submissiveId: String, prompt: String) {
        // Immediately signal success for optimistic UI update and navigation.
        _state.value = _state.value.copy(isLoading = true, assignmentCreated = true)

        // Launch the actual save operation in the background.
        viewModelScope.launch {
            when (repository.createAssignment(submissiveId, prompt)) {
                is DataResult.Error -> {
                    // If the background save fails, notify the user.
                    _state.update { it.copy(error = "Failed to create assignment. It has been removed.") }
                }
                is DataResult.Success -> {
                    // No-op
                }
            }
            // Reset the loading and created flags when the operation is complete.
            _state.update { it.copy(isLoading = false, assignmentCreated = false) }
        }
    }
}
