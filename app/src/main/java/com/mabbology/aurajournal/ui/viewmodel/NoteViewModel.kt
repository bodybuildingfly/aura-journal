package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        getNotes()
    }

    fun getNotes() {
        viewModelScope.launch {
            _noteListState.value = NoteListState(isLoading = true)
            val result = notesRepository.getNotes()
            _noteListState.value = when {
                result.isSuccess -> NoteListState(notes = result.getOrNull() ?: emptyList())
                else -> NoteListState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createNote(title: String, content: String, type: String, partnerId: String?) {
        viewModelScope.launch {
            _noteEditorState.value = NoteEditorState(isSaving = true)
            val result = notesRepository.createNote(title, content, type, partnerId)
            if (result.isSuccess) {
                val refreshResult = notesRepository.getNotes()
                _noteListState.value = when {
                    refreshResult.isSuccess -> NoteListState(notes = refreshResult.getOrNull() ?: emptyList())
                    else -> NoteListState(error = refreshResult.exceptionOrNull()?.message)
                }
                _noteEditorState.value = NoteEditorState(isSaveSuccess = true)
            } else {
                _noteEditorState.value = NoteEditorState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateNote(id: String, title: String, content: String) {
        viewModelScope.launch {
            _noteEditorState.value = NoteEditorState(isSaving = true)
            val result = notesRepository.updateNote(id, title, content)
            if (result.isSuccess) {
                val refreshResult = notesRepository.getNotes()
                _noteListState.value = when {
                    refreshResult.isSuccess -> NoteListState(notes = refreshResult.getOrNull() ?: emptyList())
                    else -> NoteListState(error = refreshResult.exceptionOrNull()?.message)
                }
                _noteEditorState.value = NoteEditorState(isSaveSuccess = true)
            } else {
                _noteEditorState.value = NoteEditorState(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            val result = notesRepository.deleteNote(id)
            if (result.isSuccess) {
                getNotes()
                _selectedNoteState.update { it.copy(isDeleted = true) }
            }
        }
    }

    fun getNoteById(id: String) {
        viewModelScope.launch {
            _selectedNoteState.value = SelectedNoteState(isLoading = true)
            val result = notesRepository.getNote(id)
            _selectedNoteState.value = when {
                result.isSuccess -> SelectedNoteState(note = result.getOrNull())
                else -> SelectedNoteState(error = result.exceptionOrNull()?.message)
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
