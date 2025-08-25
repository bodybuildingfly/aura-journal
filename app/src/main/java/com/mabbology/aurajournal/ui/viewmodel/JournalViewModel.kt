package com.mabbology.aurajournal.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalAssignmentsRepository
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private const val TAG = "JournalViewModel"

data class JournalListState(
    val isLoading: Boolean = false,
    val journals: List<Journal> = emptyList(),
    val error: String? = null
)

data class JournalEditorState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

data class SelectedJournalState(
    val isLoading: Boolean = false,
    val journal: Journal? = null,
    val error: String? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalsRepository: JournalsRepository,
    private val assignmentsRepository: JournalAssignmentsRepository
) : ViewModel() {

    private val _journalListState = MutableStateFlow(JournalListState())
    val journalListState: StateFlow<JournalListState> = _journalListState

    private val _journalEditorState = MutableStateFlow(JournalEditorState())
    val journalEditorState: StateFlow<JournalEditorState> = _journalEditorState

    private val _selectedJournalState = MutableStateFlow(SelectedJournalState())
    val selectedJournalState: StateFlow<SelectedJournalState> = _selectedJournalState

    private val isSyncing = AtomicBoolean(false)
    private var journalObserverJob: Job? = null

    init {
        observeJournals()
        syncJournals()
    }

    private fun observeJournals() {
        viewModelScope.launch {
            journalsRepository.getJournalEntries()
                .catch { e -> _journalListState.update { it.copy(error = "Failed to load journals from cache.") } }
                .collect { journals -> _journalListState.update { it.copy(journals = journals) } }
        }
    }

    fun syncJournals() {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress. Skipping.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting journal sync.")
                _journalListState.update { it.copy(isLoading = true) }
                val result = journalsRepository.syncJournalEntries()
                if (result.isFailure) {
                    _journalListState.update { it.copy(error = "Failed to sync journals.") }
                }
            } finally {
                _journalListState.update { it.copy(isLoading = false) }
                isSyncing.set(false)
                Log.d(TAG, "Journal sync finished.")
            }
        }
    }

    fun createJournalEntry(title: String, content: String, type: String, partnerId: String?, mood: String?) {
        _journalEditorState.value = JournalEditorState(isSaving = true)
        viewModelScope.launch {
            val result = journalsRepository.createJournalEntry(title, content, type, partnerId, mood)
            if (result.isSuccess) {
                _journalEditorState.value = JournalEditorState(isSaveSuccess = true)
                if (type == "shared") {
                    delay(1000)
                    syncJournals()
                }
            } else {
                _journalEditorState.value = JournalEditorState(error = result.exceptionOrNull()?.message, isSaving = false)
            }
        }
    }

    fun completeAssignment(assignmentId: String, title: String, content: String, partnerId: String?, mood: String?) {
        _journalEditorState.value = JournalEditorState(isSaving = true)
        viewModelScope.launch {
            val journalResult = journalsRepository.createJournalEntry(title, content, "shared", partnerId, mood)
            if(journalResult.isSuccess) {
                val assignmentResult = assignmentsRepository.completeAssignment(assignmentId)
                if (assignmentResult.isSuccess) {
                    _journalEditorState.value = JournalEditorState(isSaveSuccess = true)
                    delay(1000)
                    syncJournals()
                } else {
                    _journalEditorState.value = JournalEditorState(error = "Failed to complete assignment.", isSaving = false)
                }
            } else {
                _journalEditorState.value = JournalEditorState(error = journalResult.exceptionOrNull()?.message, isSaving = false)
            }
        }
    }

    fun updateJournalEntry(id: String, title: String, content: String) {
        _journalEditorState.value = JournalEditorState(isSaving = true, isSaveSuccess = true)
        viewModelScope.launch {
            val result = journalsRepository.updateJournalEntry(id, title, content)
            if (result.isFailure) {
                _journalListState.update { it.copy(error = "Failed to save changes. Your edit has been reverted.") }
            }
        }
    }

    fun deleteJournalEntry(id: String) {
        _selectedJournalState.update { it.copy(isDeleted = true) }
        viewModelScope.launch {
            val result = journalsRepository.deleteJournalEntry(id)
            if (result.isFailure) {
                _journalListState.update { it.copy(error = "Failed to delete entry. It has been restored.") }
            }
        }
    }

    fun observeJournalById(id: String) {
        journalObserverJob?.cancel()
        _selectedJournalState.update { it.copy(isLoading = true) }
        journalObserverJob = viewModelScope.launch {
            journalsRepository.getJournalEntryStream(id)
                .catch { e -> _selectedJournalState.update { it.copy(error = "Failed to load journal.", isLoading = false) } }
                .collect { journal ->
                    _selectedJournalState.update { it.copy(journal = journal, isLoading = false) }
                }
        }
    }

    fun resetEditorState() {
        _journalEditorState.value = JournalEditorState()
    }

    fun onDeletionHandled() {
        _selectedJournalState.update { it.copy(isDeleted = false) }
    }

    fun clearJournalListError() {
        _journalListState.update { it.copy(error = null) }
    }
}
