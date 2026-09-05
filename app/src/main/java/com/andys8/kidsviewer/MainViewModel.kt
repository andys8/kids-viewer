package com.andys8.kidsviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andys8.kidsviewer.data.MediaItem
import com.andys8.kidsviewer.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data object PermissionDenied : UiState
    data object Empty : UiState
    data class Ready(val items: List<MediaItem>) : UiState
}

class MainViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var loading = false

    fun onAccessChanged(hasAccess: Boolean) {
        if (!hasAccess) {
            _uiState.value = UiState.PermissionDenied
            return
        }
        if (_uiState.value is UiState.Ready || loading) return
        loadMedia()
    }

    private fun loadMedia() {
        loading = true
        viewModelScope.launch {
            val items = repository.loadCameraMedia()
            _uiState.value = if (items.isEmpty()) UiState.Empty else UiState.Ready(items)
            loading = false
        }
    }
}
