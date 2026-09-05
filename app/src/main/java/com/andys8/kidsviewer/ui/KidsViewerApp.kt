package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.andys8.kidsviewer.UiState

@Composable
fun KidsViewerApp(
    uiState: UiState,
    showPinningHelp: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissPinningHelp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (uiState) {
            is UiState.Loading -> Unit
            is UiState.PermissionNeeded -> PermissionRationaleScreen(onGrantClick = onGrantPermission)
            is UiState.PermissionDenied -> PermissionDeniedScreen(onOpenSettingsClick = onOpenSettings)
            is UiState.Empty -> EmptyLibraryScreen()
            is UiState.Ready -> PagerScreen(items = uiState.items)
        }

        if (showPinningHelp) {
            PinningHelpOverlay(onDismiss = onDismissPinningHelp)
        }
    }
}
