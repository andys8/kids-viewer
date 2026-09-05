package com.andys8.kidsviewer.ui

import android.content.Context
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Black, like every other backdrop in the app, so a placeholder never looks like a grey panel. */
val PlaceholderColor = Color.Black

private val GlyphColor = Color.White.copy(alpha = 0.32f)
private val GlyphSize = 72.dp
private val GlyphStroke = 3.dp

/** Small on purpose: big enough to look right full-screen, small enough to stay cheap. */
private const val THUMBNAIL_PX = 512

/**
 * Stands in for a video that isn't showing its own pixels yet — while it slides in, and while it
 * decodes its first frame.
 *
 * The poster frame is the placeholder wherever one is available: scaled the same way the video
 * is, it lands in exactly the rectangle the video will occupy, whatever the clip's shape. Only
 * when there is no poster does a small centred play glyph stand in. Nothing is drawn to the edges
 * of the screen, because the screen is not the shape of the video.
 */
@Composable
fun VideoPlaceholder(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(VideoThumbnails.cached(item.id), item.id) {
        if (value == null) value = VideoThumbnails.load(context, item)
    }

    Box(
        modifier = modifier.fillMaxSize().background(PlaceholderColor),
        contentAlignment = Alignment.Center
    ) {
        val poster = thumbnail
        if (poster != null) {
            Image(
                bitmap = poster,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlayGlyph()
        }
    }
}

/** A plain play mark: a ring with a triangle whose centre of area sits on the ring's centre. */
@Composable
private fun PlayGlyph() {
    Canvas(modifier = Modifier.size(GlyphSize)) {
        val stroke = GlyphStroke.toPx()
        val radius = size.minDimension / 2f
        val middle = center

        drawCircle(
            color = GlyphColor,
            radius = radius - stroke / 2f,
            center = middle,
            style = Stroke(width = stroke)
        )

        val triangle = Path().apply {
            moveTo(middle.x - radius * 0.26f, middle.y - radius * 0.45f)
            lineTo(middle.x - radius * 0.26f, middle.y + radius * 0.45f)
            lineTo(middle.x + radius * 0.52f, middle.y)
            close()
        }
        drawPath(path = triangle, color = GlyphColor)
    }
}

/**
 * Poster frames come from MediaStore's own thumbnails, which the system has usually generated
 * already. Pulling a frame out of the video file itself would mean starting a second decoder,
 * which is the last thing to do next to a 4K video that is already playing.
 */
private object VideoThumbnails {

    private val cache = LruCache<Long, ImageBitmap>(24)

    fun cached(id: Long): ImageBitmap? = cache.get(id)

    suspend fun load(context: Context, item: MediaItem): ImageBitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        cache.get(item.id)?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching { loadThumbnail(context, item) }
                .getOrNull()?.also { cache.put(item.id, it) }
        }
    }

    /**
     * Split out so the version guard above is one the compiler and lint can both see: inside the
     * coroutine lambda the check in [load] is no longer visible to them, only to the runtime.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadThumbnail(context: Context, item: MediaItem): ImageBitmap =
        context.contentResolver
            .loadThumbnail(item.uri, Size(THUMBNAIL_PX, THUMBNAIL_PX), null)
            .asImageBitmap()
}
