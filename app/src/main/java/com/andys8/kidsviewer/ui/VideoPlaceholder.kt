package com.andys8.kidsviewer.ui

import android.content.Context
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dark grey rather than black, so a video that is still decoding reads as a placeholder. */
val PlaceholderColor = Color(0xFF1B1B1B)

private val FrameColor = Color.White.copy(alpha = 0.18f)
private val FrameInset = 28.dp
private val FrameCorner = 20.dp
private val FrameStroke = 2.dp

/** Small on purpose: big enough to look right full-screen, small enough to stay cheap. */
private const val THUMBNAIL_PX = 512

/**
 * Stands in for a video that isn't showing its own pixels yet — while it slides in, and while
 * it decodes its first frame. It shows the video's own poster frame when the system can give us
 * one, and an empty framed rectangle in the meantime, so a swipe never lands on a blank screen.
 */
@Composable
fun VideoPlaceholder(item: MediaItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(VideoThumbnails.cached(item.id), item.id) {
        if (value == null) value = VideoThumbnails.load(context, item)
    }

    Box(modifier = modifier.fillMaxSize().background(PlaceholderColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = FrameInset.toPx()
            drawRoundRect(
                color = FrameColor,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width - inset * 2,
                    height = size.height - inset * 2
                ),
                cornerRadius = CornerRadius(FrameCorner.toPx()),
                style = Stroke(width = FrameStroke.toPx())
            )
        }

        thumbnail?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
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
            runCatching {
                context.contentResolver
                    .loadThumbnail(item.uri, Size(THUMBNAIL_PX, THUMBNAIL_PX), null)
                    .asImageBitmap()
            }.getOrNull()?.also { cache.put(item.id, it) }
        }
    }
}
