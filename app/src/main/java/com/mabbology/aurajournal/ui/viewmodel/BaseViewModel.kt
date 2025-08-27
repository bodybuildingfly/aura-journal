package com.mabbology.aurajournal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mabbology.aurajournal.core.util.DataResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// --- Generic State Data Classes ---

data class ListState<T>(
    val isLoading: Boolean = false,
    val items: List<T> = emptyList(),
    val error: String? = null
)

data class EditorState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

data class SelectedState<T>(
    val isLoading: Boolean = false,
    val item: T? = null,
    val error: String? = null,
    val isDeleted: Boolean = false
)

// --- Base ViewModel ---

abstract class BaseViewModel<T : Any> : ViewModel() {

    // --- State Flows ---

    protected val _listState = MutableStateFlow(ListState<T>())
    val listState: StateFlow<ListState<T>> = _listState

    protected val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState

    protected val _selectedState = MutableStateFlow(SelectedState<T>())
    val selectedState: StateFlow<SelectedState<T>> = _selectedState

    // --- Private Properties ---

    private val isSyncing = AtomicBoolean(false)
    private var observerJob: Job? = null

    // --- Abstract Functions (to be implemented by subclasses) ---

    protected abstract fun getItemsFlow(): Flow<List<T>>
    protected abstract suspend fun syncItems(): DataResult<Unit>
    protected abstract fun getItemStream(id: String): Flow<T?>
    protected abstract suspend fun deleteItem(id: String): DataResult<Unit>

    // --- Public Functions ---

    fun initialize() {
        observeItems()
        sync()
    }

    fun sync() {
        if (!isSyncing.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                _listState.update { it.copy(isLoading = true) }
                when (syncItems()) {
                    is DataResult.Error -> _listState.update { it.copy(error = "Failed to sync.") }
                    is DataResult.Success -> { /* No-op */ }
                }
            } finally {
                _listState.update { it.copy(isLoading = false) }
                isSyncing.set(false)
            }
        }
    }

    fun observeItemById(id: String) {
        observerJob?.cancel()
        _selectedState.update { it.copy(isLoading = true) }
        observerJob = viewModelScope.launch {
            getItemStream(id)
                .catch { _ -> _selectedState.update { it.copy(error = "Failed to load item.", isLoading = false) } }
                .collect { item ->
                    _selectedState.update { it.copy(item = item, isLoading = false) }
                }
        }
    }

    fun deleteItemById(id: String) {
        _selectedState.update { it.copy(isDeleted = true) }
        viewModelScope.launch {
            when (deleteItem(id)) {
                is DataResult.Error -> {
                    _listState.update { it.copy(error = "Failed to delete. It has been restored.") }
                }
                is DataResult.Success -> { /* No-op */ }
            }
        }
    }

    // --- Helper Functions ---

    fun resetEditorState() {
        _editorState.value = EditorState()
    }

    fun onDeletionHandled() {
        _selectedState.update { it.copy(isDeleted = false) }
    }

    fun clearListError() {
        _listState.update { it.copy(error = null) }
    }

    private fun observeItems() {
        viewModelScope.launch {
            getItemsFlow()
                .catch { _ -> _listState.update { it.copy(error = "Failed to load from cache.") } }
                .collect { items -> _listState.update { it.copy(items = items) } }
        }
    }
}
