package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.repository.JournalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalsRepository: JournalsRepository
) : ViewModel() {

    private val _journalListState = MutableStateFlow(JournalListState())
    val journalListState: StateFlow<JournalListState> = _journalListState

    private val _journalEditorState = MutableStateFlow(JournalEditorState())
    val journalEditorState: StateFlow<JournalEditorState> = _journalEditorState

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

    fun createJournalEntry(title: String, content: String) {
        viewModelScope.launch {
            _journalEditorState.value = JournalEditorState(isSaving = true)
            val result = journalsRepository.createJournalEntry(title, content)
            _journalEditorState.value = when {
                result.isSuccess -> JournalEditorState(isSaveSuccess = true)
                else -> JournalEditorState(error = result.exceptionOrNull()?.message)
            }
        }
    }
}
