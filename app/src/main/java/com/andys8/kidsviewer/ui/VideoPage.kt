package com.andys8.kidsviewer.ui

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.andys8.kidsviewer.R
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.delay

/**
 * Never leave the placeholder covering a video that is in fact playing: if the first frame is
 * never reported — it may already have been rendered before this page attached the player — give
 * up waiting and show the video anyway.
 */
private const val PLACEHOLDER_TIMEOUT_MS = 600L

/**
 * Renders the shared player's video output. Only the settled page attaches the player: a single
 * player instance means a single hardware decoder, and nothing ever renders a released player.
 */
@Composable
fun VideoPage(
    item: MediaItem,
    player: Player,
    attached: Boolean,
    modifier: Modifier = Modifier
) {
    if (!attached) {
        VideoPlaceholder(item = item, modifier = modifier)
        return
    }

    var showingVideo by remember(item.id) { mutableStateOf(false) }

    DisposableEffect(player, item.id) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                showingVideo = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(item.id) {
        delay(PLACEHOLDER_TIMEOUT_MS)
        showingVideo = true
    }

    Box(modifier = modifier.fillMaxSize().background(PlaceholderColor)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_player, null) as PlayerView
                view.player = player
                view
            },
            onRelease = { view ->
                // Detach before the player can be released elsewhere, so the view never
                // touches a released player while it is being torn down.
                view.player = null
            }
        )

        // Sits above the video surface until there are real pixels behind it.
        if (!showingVideo) {
            VideoPlaceholder(item = item)
        }
    }
}
