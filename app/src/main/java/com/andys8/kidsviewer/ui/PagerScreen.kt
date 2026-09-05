package com.andys8.kidsviewer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.andys8.kidsviewer.R
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.media3.common.MediaItem as Media3Item

private const val AUTO_ADVANCE_DELAY_MS = 3000L

/**
 * A slow ease-in-out glide reads as far smoother for an unattended slideshow than the pager's
 * default spring, which snaps quickly and abruptly.
 */
private const val AUTO_ADVANCE_ANIMATION_MS = 650

/** Compose the neighbouring pages so their photos are decoded before they scroll into view. */
private const val PRELOADED_NEIGHBOUR_PAGES = 1

/** Hold the top-left corner this long to switch modes. */
private const val SWITCH_HOLD_MS = 2000L
private const val MODE_INDICATOR_MS = 1400L

/** Generous enough to hit deliberately, small enough to stay out of the way. */
private val SWITCH_TARGET_SIZE = 140.dp

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

    // The id of the video actually seen playing. An end report is only believable for a video
    // that reached READY first, which rules out a stale report from the clip just swiped away.
    val playedMediaId = remember { mutableStateOf<String?>(null) }

    // Swipe-only to begin with: nothing moves until somebody asks it to.
    var slideshow by rememberSaveable { mutableStateOf(false) }

    /*
     * What the label says is written once, by the switch that caused it, and never recomputed.
     * Deriving it from the current mode is what made it contradict itself: any later evaluation
     * could render different text than the one the gesture had announced.
     */
    var labelSaysSlideshow by remember { mutableStateOf(false) }
    var labelVisible by remember { mutableStateOf(false) }
    var switchCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(switchCount) {
        if (switchCount == 0) return@LaunchedEffect
        delay(MODE_INDICATOR_MS)
        labelVisible = false
    }

    LaunchedEffect(slideshow) {
        player.repeatMode = if (slideshow) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
    }

    DisposableEffect(player, items) {
        val listener = object : Player.Listener {
            // A finished video moves on just like a photo does, so the slideshow keeps flowing
            // instead of repeating the same clip. A lone video has nowhere to go, so it replays.
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playedMediaId.value = player.currentMediaItem?.mediaId
                    return
                }
                if (playbackState != Player.STATE_ENDED) return
                if (!slideshow) return
                if (!currentVideoIsPlaying()) return
                if (playedMediaId.value != player.currentMediaItem?.mediaId) return
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = PRELOADED_NEIGHBOUR_PAGES,
            pageSpacing = 0.dp,
            // Swiping belongs to swipe-only mode. While the slideshow runs itself there is
            // nothing for small hands to change: a finger can hold the current item, and
            // nothing else. Programmatic scrolling is unaffected, so the slideshow still moves.
            userScrollEnabled = !slideshow,
            key = { page -> items[page].id }
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPage(
                    item = item,
                    player = player,
                    attached = page == pagerState.settledPage
                )
            } else {
                ImagePage(item = item)
            }
        }

        /*
         * The mode switch: hold this invisible corner target. The whole gesture lives in one
         * coroutine that begins on touch-down and ends when the finger lifts, so exactly one
         * switch per press falls out of the structure -- there is no state to get out of step
         * and no boundary for a drifting finger to cross, because the target itself is the hit
         * area. Nothing is consumed, so a swipe starting here still swipes.
         */
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(SWITCH_TARGET_SIZE)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        val liftedEarly = withTimeoutOrNull(SWITCH_HOLD_MS) {
                            waitForUpOrCancellation()
                            true
                        }
                        if (liftedEarly != null) return@awaitEachGesture

                        // The label announces a mode that is already in effect, never one that
                        // is still being waited for.
                        slideshow = !slideshow
                        labelSaysSlideshow = slideshow
                        labelVisible = true
                        switchCount++
                    }
                }
        )

        ModeIndicator(
            label = stringResource(
                if (labelSaysSlideshow) R.string.mode_slideshow else R.string.mode_swipe_only
            ),
            visible = labelVisible
        )
    }

    // Photos advance on a timer. Videos have no timer: they advance when they finish playing.
    // Nothing a finger does interrupts either: a running slideshow is not pausable.
    LaunchedEffect(pagerState.settledPage, items, slideshow) {
        if (!slideshow) return@LaunchedEffect
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
