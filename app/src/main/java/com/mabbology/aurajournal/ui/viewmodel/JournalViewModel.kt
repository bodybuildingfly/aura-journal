package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val journalsRepository: JournalsRepository
) : ViewModel() {

    private val _journalListState = MutableStateFlow(JournalListState())
    val journalListState: StateFlow<JournalListState> = _journalListState

    private val _journalEditorState = MutableStateFlow(JournalEditorState())
    val journalEditorState: StateFlow<JournalEditorState> = _journalEditorState

    private val _selectedJournalState = MutableStateFlow(SelectedJournalState())
    val selectedJournalState: StateFlow<SelectedJournalState> = _selectedJournalState

    init {
        getJournalEntries()
    }

    fun getJournalEntries() {
        viewModelScope.launch {
            _journalListState.value = JournalListState(isLoading = true)
            val result = journalsRepository.getJournalEntries()
            _journalListState.value = when {
                result.isSuccess -> JournalListState(journals = result.getOrNull() ?: emptyList())
                else -> JournalListState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createJournalEntry(title: String, content: String, type: String, partnerId: String?) {
        viewModelScope.launch {
            _journalEditorState.value = JournalEditorState(isSaving = true)
            val result = journalsRepository.createJournalEntry(title, content, type, partnerId)

            if (result.isSuccess) {
                getJournalEntries()
                _journalEditorState.value = JournalEditorState(isSaveSuccess = true)
            } else {
                _journalEditorState.value = JournalEditorState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateJournalEntry(id: String, title: String, content: String) {
        viewModelScope.launch {
            _journalEditorState.value = JournalEditorState(isSaving = true)
            val result = journalsRepository.updateJournalEntry(id, title, content)
            if (result.isSuccess) {
                getJournalEntries()
                _journalEditorState.value = JournalEditorState(isSaveSuccess = true)
            } else {
                _journalEditorState.value = JournalEditorState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteJournalEntry(id: String) {
        viewModelScope.launch {
            val result = journalsRepository.deleteJournalEntry(id)
            if (result.isSuccess) {
                getJournalEntries()
                _selectedJournalState.update { it.copy(isDeleted = true) }
            } else {
                // You could add error handling here if needed
            }
        }
    }

    fun getJournalById(id: String) {
        viewModelScope.launch {
            _selectedJournalState.value = SelectedJournalState(isLoading = true)
            val result = journalsRepository.getJournalEntry(id)
            _selectedJournalState.value = when {
                result.isSuccess -> SelectedJournalState(journal = result.getOrNull())
                else -> SelectedJournalState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun resetEditorState() {
        _journalEditorState.value = JournalEditorState()
    }

    fun onDeletionHandled() {
        _selectedJournalState.update { it.copy(isDeleted = false) }
    }
}
