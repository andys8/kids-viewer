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
    crashReport: String?,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissPinningHelp: () -> Unit,
    onDismissCrashReport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (uiState) {
            is UiState.Loading -> Unit
            is UiState.PermissionDenied -> PermissionScreen(
                onGrantClick = onGrantPermission,
                onOpenSettingsClick = onOpenSettings
            )

            is UiState.Empty -> EmptyLibraryScreen()
            is UiState.Ready -> PagerScreen(items = uiState.items)
        }

        if (showPinningHelp) {
            PinningHelpOverlay(onDismiss = onDismissPinningHelp)
        }

        if (crashReport != null) {
            CrashReportOverlay(report = crashReport, onDismiss = onDismissCrashReport)
        }
    }
}
