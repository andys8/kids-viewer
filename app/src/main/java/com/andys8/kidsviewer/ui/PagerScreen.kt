package com.andys8.kidsviewer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem as Media3Item

private const val AUTO_ADVANCE_DELAY_MS = 3000L

/**
 * A slow ease-in-out glide reads as far smoother for an unattended slideshow than the pager's
 * default spring, which snaps quickly and abruptly.
 */
private const val AUTO_ADVANCE_ANIMATION_MS = 650

/** Compose the neighbouring pages so their photos are decoded before they scroll into view. */
private const val PRELOADED_NEIGHBOUR_PAGES = 1

private val AdvanceAnimation = tween<Float>(
    durationMillis = AUTO_ADVANCE_ANIMATION_MS,
    easing = FastOutSlowInEasing
)

@Composable
fun PagerScreen(items: List<MediaItem>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pagerState = rememberPagerState(initialPage = 0) { items.size }
    val scope = rememberCoroutineScope()

    // One player for the whole pager. One player per page exhausts the device's hardware
    // video decoders and lets a view outlive the player it is drawing.
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Always step on from the page the user is actually on, so a report that arrives mid-swipe
    // can never send them off in the direction they just swiped away from.
    val advanceToNext: () -> Unit = {
        val next = (pagerState.currentPage + 1) % items.size
        scope.launch { pagerState.animateScrollToPage(page = next, animationSpec = AdvanceAnimation) }
    }

    /**
     * The player reports STATE_ENDED for an emptied playlist too, not just for a video that
     * played to its end — and swiping off a video empties it. So a report only counts as
     * "finished" while the video it belongs to is still the one on screen.
     */
    val currentVideoIsPlaying: () -> Boolean = {
        val current = items.getOrNull(pagerState.currentPage)
        current != null &&
            current.isVideo &&
            player.currentMediaItem?.mediaId == current.id.toString()
    }

    DisposableEffect(player, items) {
        val listener = object : Player.Listener {
            // A finished video moves on just like a photo does, so the slideshow keeps flowing
            // instead of repeating the same clip. A lone video has nowhere to go, so it replays.
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return
                if (!currentVideoIsPlaying()) return
                if (items.size <= 1) {
                    player.seekTo(0L)
                    player.play()
                } else {
                    advanceToNext()
                }
            }

            // Skip past anything that can't be decoded instead of sitting on a placeholder.
            override fun onPlayerError(error: PlaybackException) {
                if (items.size > 1 && currentVideoIsPlaying()) advanceToNext()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Never keep decoding while the app isn't in the foreground.
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (player.mediaItemCount > 0) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Start decoding as soon as the page is on its way in rather than once it settles, so the
    // first frame is usually ready by the time the swipe finishes.
    LaunchedEffect(pagerState.currentPage, items) {
        val item = items.getOrNull(pagerState.currentPage)
        if (item != null && item.isVideo) {
            // Tag the item with its own id, rather than relying on what fromUri() defaults the
            // media id to, so "is this still the video on screen?" is answerable later.
            player.setMediaItem(
                Media3Item.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.id.toString())
                    .build()
            )
            player.prepare()
            player.play()
        } else {
            player.pause()
            player.clearMediaItems()
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        beyondViewportPageCount = PRELOADED_NEIGHBOUR_PAGES,
        pageSpacing = 0.dp,
        userScrollEnabled = true,
        key = { page -> items[page].id }
    ) { page ->
        val item = items[page]
        if (item.isVideo) {
            VideoPage(player = player, attached = page == pagerState.settledPage)
        } else {
            ImagePage(item = item)
        }
    }

    // Photos advance on a timer. Videos have no timer: they advance when they finish playing.
    LaunchedEffect(pagerState.settledPage, items) {
        if (items.size <= 1) return@LaunchedEffect
        val current = pagerState.settledPage
        if (items.getOrNull(current)?.isVideo != false) return@LaunchedEffect
        delay(AUTO_ADVANCE_DELAY_MS)
        pagerState.animateScrollToPage(
            page = (current + 1) % items.size,
            animationSpec = AdvanceAnimation
        )
    }
}
