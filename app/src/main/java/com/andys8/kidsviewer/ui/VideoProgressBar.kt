package com.andys8.kidsviewer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import kotlinx.coroutines.delay

private const val POLL_INTERVAL_MS = 200L
private val TRACK_COLOR = Color.White.copy(alpha = 0.20f)
private val FILL_COLOR = Color.White.copy(alpha = 0.75f)

/**
 * A non-interactive progress bar for the playing video.
 *
 * The position is held in a plain float state that is read *only* inside the draw lambda, so a
 * tick invalidates the draw phase alone — no recomposition and no relayout. That keeps it off the
 * critical path while video is decoding.
 */
@Composable
fun VideoProgressBar(player: Player, modifier: Modifier = Modifier) {
    val progress = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            val position = player.currentPosition
            progress.floatValue = if (duration > 0L) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    Canvas(modifier = modifier) {
        drawRect(color = TRACK_COLOR, size = size)
        drawRect(
            color = FILL_COLOR,
            size = Size(width = size.width * progress.floatValue, height = size.height)
        )
    }
}
