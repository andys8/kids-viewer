package com.andys8.kidsviewer.ui

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.andys8.kidsviewer.R

/** Dark grey rather than black, so a video that is still decoding reads as a placeholder. */
val PlaceholderColor = Color(0xFF141414)

/**
 * Renders the shared player's video output. Only the settled page attaches the player: a single
 * player instance means a single hardware decoder, and nothing ever renders a released player.
 */
@Composable
fun VideoPage(player: Player, attached: Boolean, modifier: Modifier = Modifier) {
    if (!attached) {
        Box(modifier = modifier.fillMaxSize().background(PlaceholderColor))
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(PlaceholderColor),
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
}
