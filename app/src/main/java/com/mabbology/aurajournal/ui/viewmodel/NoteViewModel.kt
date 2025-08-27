package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Note
import com.mabbology.aurajournal.domain.repository.NotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : BaseViewModel<Note>() {

    init {
        initialize()
    }

    // --- BaseViewModel Overrides ---

    override fun getItemsFlow(): Flow<List<Note>> = notesRepository.getNotes()
    override suspend fun syncItems(): DataResult<Unit> = notesRepository.syncNotes()
    override fun getItemStream(id: String): Flow<Note?> = notesRepository.getNoteStream(id)
    override suspend fun deleteItem(id: String): DataResult<Unit> = notesRepository.deleteNote(id)

    // --- Note-Specific Functions ---

    fun createNote(title: String, content: String, type: String, partnerId: String?) {
        _editorState.value = EditorState(isSaving = true)
        viewModelScope.launch {
            when (val result = notesRepository.createNote(title, content, type, partnerId)) {
                is DataResult.Success -> {
                    _editorState.value = EditorState(isSaveSuccess = true)
                }
                is DataResult.Error -> {
                    _editorState.value = EditorState(error = result.exception.message, isSaving = false)
                }
            }
        }
    }

    fun updateNote(id: String, title: String, content: String) {
        _editorState.value = EditorState(isSaving = true, isSaveSuccess = true)
        viewModelScope.launch {
            when (notesRepository.updateNote(id, title, content)) {
                is DataResult.Error -> {
                    _listState.update { it.copy(error = "Failed to save changes. Your edit has been reverted.") }
                }
                is DataResult.Success -> { /* No-op */ }
            }
        }
    }
}
