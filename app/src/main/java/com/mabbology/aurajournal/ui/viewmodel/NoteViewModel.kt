package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteListState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val error: String? = null
)

data class NoteEditorState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

data class SelectedNoteState(
    val isLoading: Boolean = false,
    val note: Note? = null,
    val error: String? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _noteListState = MutableStateFlow(NoteListState())
    val noteListState: StateFlow<NoteListState> = _noteListState

    private val _noteEditorState = MutableStateFlow(NoteEditorState())
    val noteEditorState: StateFlow<NoteEditorState> = _noteEditorState

    private val _selectedNoteState = MutableStateFlow(SelectedNoteState())
    val selectedNoteState: StateFlow<SelectedNoteState> = _selectedNoteState

    private var noteObserverJob: Job? = null

    init {
        observeNotes()
        syncNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            notesRepository.getNotes()
                .catch { e -> _noteListState.update { it.copy(error = "Failed to load notes from cache.") } }
                .collect { notes -> _noteListState.update { it.copy(notes = notes) } }
        }
    }

    fun syncNotes() {
        viewModelScope.launch {
            _noteListState.update { it.copy(isLoading = true) }
            when (notesRepository.syncNotes()) {
                is DataResult.Error -> _noteListState.update { it.copy(error = "Failed to sync notes.") }
                is DataResult.Success -> {
                    // No-op
                }
            }
            _noteListState.update { it.copy(isLoading = false) }
        }
    }

    fun createNote(title: String, content: String, type: String, partnerId: String?) {
        _noteEditorState.value = NoteEditorState(isSaving = true)
        viewModelScope.launch {
            when (val result = notesRepository.createNote(title, content, type, partnerId)) {
                is DataResult.Success -> {
                    // The UI is now unblocked immediately because the repository returns the temporary note.
                    _noteEditorState.value = NoteEditorState(isSaveSuccess = true)
                }
                is DataResult.Error -> {
                    _noteEditorState.value = NoteEditorState(error = result.exception.message, isSaving = false)
                }
            }
        }
    }

    fun updateNote(id: String, title: String, content: String) {
        _noteEditorState.value = NoteEditorState(isSaving = true, isSaveSuccess = true)
        viewModelScope.launch {
            when (notesRepository.updateNote(id, title, content)) {
                is DataResult.Error -> {
                    _noteListState.update { it.copy(error = "Failed to save changes. Your edit has been reverted.") }
                }
                is DataResult.Success -> {
                    // No-op
                }
            }
        }
    }

    fun deleteNote(id: String) {
        _selectedNoteState.update { it.copy(isDeleted = true) }
        viewModelScope.launch {
            when (notesRepository.deleteNote(id)) {
                is DataResult.Error -> {
                    _noteListState.update { it.copy(error = "Failed to delete note. It has been restored.") }
                }
                is DataResult.Success -> {
                    // No-op
                }
            }
        }
    }

    fun observeNoteById(id: String) {
        noteObserverJob?.cancel()
        _selectedNoteState.update { it.copy(isLoading = true) }
        noteObserverJob = viewModelScope.launch {
            notesRepository.getNoteStream(id)
                .catch { e -> _selectedNoteState.update { it.copy(error = "Failed to load note.", isLoading = false) } }
                .collect { note ->
                    _selectedNoteState.update { it.copy(note = note, isLoading = false) }
                }
        }
    }

    fun resetEditorState() {
        _noteEditorState.value = NoteEditorState()
    }

    fun onDeletionHandled() {
        _selectedNoteState.update { it.copy(isDeleted = false) }
    }
}
