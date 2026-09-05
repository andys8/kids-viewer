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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val FADE_IN_MS = 150
private const val FADE_OUT_MS = 450

/**
 * Confirms a mode change and then gets out of the way. It only ever appears in response to the
 * switch gesture, so there is nothing on screen for a child to find or press.
 */
@Composable
fun ModeIndicator(label: String, visible: Boolean, modifier: Modifier = Modifier) {
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
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
