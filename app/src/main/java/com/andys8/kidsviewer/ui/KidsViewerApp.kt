package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.andys8.kidsviewer.R
import com.andys8.kidsviewer.UiState
import com.andys8.kidsviewer.data.PictureLibrary

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
    val context = LocalContext.current

    // Every launch starts on the phone's own photos, the way every launch starts in swipe-only:
    // whoever hands the phone over knows what it is about to show.
    var pictureMode by rememberSaveable { mutableStateOf(false) }

    /*
     * Counted, not just flagged, because the count is what makes a visit a visit. It re-keys the
     * load below, so the pictures come back in a new order, and the pager, so it starts at the
     * first of them instead of restoring a position into a list that is no longer there.
     *
     * Not saved: were the app killed mid-visit, everything the count re-keys would be rebuilt
     * from nothing anyway, and a count restored beside them would say a visit had already
     * happened when none of it is still there.
     */
    var visit by remember { mutableIntStateOf(0) }
    var announcement by remember { mutableIntStateOf(0) }

    // Nothing is read until the mode is entered for the first time.
    val pictures =
        if (pictureMode) remember(visit) { PictureLibrary.load(context) } else emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // An app built without its pictures is a broken build rather than a state to design for,
        // but should it ever happen, falling back to the photos beats a black screen.
        if (pictures.isNotEmpty()) {
            key(visit) { PagerScreen(items = pictures) }
        } else {
            when (uiState) {
                is UiState.Loading -> Unit
                is UiState.PermissionDenied -> PermissionScreen(
                    onGrantClick = onGrantPermission,
                    onOpenSettingsClick = onOpenSettings
                )

                is UiState.Empty -> EmptyLibraryScreen()
                is UiState.Ready -> PagerScreen(items = uiState.items)
            }
        }

        // Hold the top-right corner to switch between the phone's photos and the bundled
        // pictures. It sits over every screen, the permission one included, because the pictures
        // need no permission: there is always something to hand a child, whatever was granted.
        ModeSwitchTarget(modifier = Modifier.align(Alignment.TopEnd)) {
            pictureMode = !pictureMode
            if (pictureMode) visit++
            announcement++
        }

        ModeIndicator(
            label = stringResource(
                if (pictureMode) R.string.mode_pictures else R.string.mode_photos_videos
            ),
            announcement = announcement,
            modifier = Modifier.align(Alignment.Center)
        )

        if (showPinningHelp) {
            PinningHelpOverlay(onDismiss = onDismissPinningHelp)
        }

        if (crashReport != null) {
            CrashReportOverlay(report = crashReport, onDismiss = onDismissCrashReport)
        }
    }
}
