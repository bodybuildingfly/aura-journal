package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.JournalAssignment
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun getPendingAssignments() {
        viewModelScope.launch {
            _state.value = JournalAssignmentState(isLoading = true)
            val result = repository.getPendingAssignments()
            _state.value = when {
                result.isSuccess -> JournalAssignmentState(assignments = result.getOrNull() ?: emptyList())
                else -> JournalAssignmentState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createAssignment(submissiveId: String, prompt: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.createAssignment(submissiveId, prompt)
            _state.value = when {
                result.isSuccess -> _state.value.copy(isLoading = false, assignmentCreated = true)
                else -> _state.value.copy(isLoading = false, error = "Failed to create assignment.")
            }
        }
    }
}
