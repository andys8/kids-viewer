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
    data object PermissionNeeded : UiState
    data object PermissionDenied : UiState
    data object Empty : UiState
    data class Ready(val items: List<MediaItem>) : UiState
}

class MainViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var hasRequestedPermissionOnce = false

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            loadMedia()
        } else {
            _uiState.value = UiState.PermissionDenied
        }
    }

    fun requestPermissionIfNeeded(alreadyGranted: Boolean, onRequest: () -> Unit) {
        if (alreadyGranted) {
            loadMedia()
            return
        }
        if (!hasRequestedPermissionOnce) {
            hasRequestedPermissionOnce = true
            onRequest()
        } else {
            _uiState.value = UiState.PermissionNeeded
        }
    }

    private fun loadMedia() {
        viewModelScope.launch {
            val items = repository.loadCameraMedia()
            _uiState.value = if (items.isEmpty()) UiState.Empty else UiState.Ready(items)
        }
    }
}
