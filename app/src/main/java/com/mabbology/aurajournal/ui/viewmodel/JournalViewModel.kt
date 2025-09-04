package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import com.mabbology.aurajournal.domain.model.Journal
import com.mabbology.aurajournal.domain.use_case.journal.JournalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalUseCases: JournalUseCases
) : BaseViewModel<Journal>() {

    init {
        initialize()
    }

    // --- BaseViewModel Overrides ---

    override fun getItemsFlow(scope: Scope): Flow<List<Journal>> {
        val partner = if (scope is Scope.PartnerScope) scope.partner else null
        return journalUseCases.getJournals(partner)
    }
    override suspend fun syncItems(): DataResult<Unit> = journalUseCases.syncJournals()
    override fun getItemStream(id: String): Flow<Journal?> = journalUseCases.getJournal(id)
    override suspend fun deleteItem(id: String): DataResult<Unit> = journalUseCases.deleteJournal(id)

    // --- Journal-Specific Functions ---

    fun createJournalEntry(title: String, content: String, type: String, partnerId: String?, mood: String?) {
        _editorState.value = EditorState(isSaving = true)
        viewModelScope.launch {
            when (val result = journalUseCases.createJournal(title, content, type, partnerId, mood)) {
                is DataResult.Success -> {
                    _editorState.value = EditorState(isSaveSuccess = true)
                    if (type == "shared") {
                        delay(1000)
                        sync()
                    }
                }
                is DataResult.Error -> {
                    _editorState.value = EditorState(error = result.exception.message, isSaving = false)
                }
            }
        }
    }

    fun completeAssignment(assignmentId: String, title: String, content: String, partnerId: String?, mood: String?) {
        _editorState.value = EditorState(isSaving = true)
        viewModelScope.launch {
            when (val result = journalUseCases.completeAssignmentAndCreateJournal(assignmentId, title, content, partnerId, mood)) {
                is DataResult.Success -> {
                    _editorState.value = EditorState(isSaveSuccess = true)
                    delay(1000)
                    sync()
                }
                is DataResult.Error -> {
                    _editorState.value = EditorState(error = result.exception.message, isSaving = false)
                }
            }
        }
    }

    fun updateJournalEntry(id: String, title: String, content: String) {
        _editorState.value = EditorState(isSaving = true, isSaveSuccess = true)
        viewModelScope.launch {
            when (journalUseCases.updateJournal(id, title, content)) {
                is DataResult.Error -> {
                    _listState.update { it.copy(error = "Failed to save changes. Your edit has been reverted.") }
                }
                is DataResult.Success -> { /* No-op */ }
            }
        }
    }
}
