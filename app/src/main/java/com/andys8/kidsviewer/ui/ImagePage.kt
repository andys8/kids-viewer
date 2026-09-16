package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.andys8.kidsviewer.data.MediaItem

private const val CROSSFADE_MS = 200

/** How much darker the bottom of a backdrop is than its top. Enough to read as light falling. */
private const val BACKDROP_FALLOFF = 0.88f

@Composable
fun ImagePage(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val request = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(CROSSFADE_MS)
            .build()
    }

    // A photo brings its own background and gets black, as it always has. A bundled picture is
    // cut out, so the backdrop it was given is what fills the screen around it.
    val background = remember(item.backdropColor) { backdrop(item.backdropColor) }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .background(background)
    )
}

private fun backdrop(color: Int?): Brush {
    if (color == null) return SolidColor(Color.Black)
    val top = Color(color)
    val bottom = Color(
        red = top.red * BACKDROP_FALLOFF,
        green = top.green * BACKDROP_FALLOFF,
        blue = top.blue * BACKDROP_FALLOFF,
        alpha = top.alpha
    )
    return Brush.verticalGradient(listOf(top, bottom))
}
