package com.andys8.kidsviewer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val VISIBLE_MS = 1400L
private const val FADE_IN_MS = 150
private const val FADE_OUT_MS = 450

/**
 * Confirms a mode change and then gets out of the way. It only ever appears in response to a
 * switch gesture, so there is nothing on screen for a child to find or press.
 *
 * Showing it is a matter of increasing [announcement]; the label is read once, at that moment,
 * and held until it has faded. Rendering the live mode instead is what once made the label
 * contradict itself: any later evaluation could redraw it with text the gesture never announced.
 */
@Composable
fun ModeIndicator(label: String, announcement: Int, modifier: Modifier = Modifier) {
    var announced by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    // Whatever the count is on the first composition is the state of things, not an event.
    var lastAnnounced by remember { mutableIntStateOf(announcement) }

    LaunchedEffect(announcement) {
        if (announcement == lastAnnounced) return@LaunchedEffect
        lastAnnounced = announcement
        announced = label
        visible = true
        delay(VISIBLE_MS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(FADE_IN_MS)),
        exit = fadeOut(animationSpec = tween(FADE_OUT_MS)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Text(
                text = announced,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
