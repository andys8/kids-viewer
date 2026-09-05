package com.andys8.kidsviewer.ui

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
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Skip past anything that can't be decoded instead of sitting on a black screen.
    DisposableEffect(player, items) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (items.size <= 1) return
                val next = (pagerState.settledPage + 1) % items.size
                scope.launch { pagerState.animateScrollToPage(next) }
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

    // Point the shared player at whatever page we've settled on.
    LaunchedEffect(pagerState.settledPage, items) {
        val item = items.getOrNull(pagerState.settledPage)
        if (item != null && item.isVideo) {
            player.setMediaItem(Media3Item.fromUri(item.uri))
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
        pageSpacing = 0.dp,
        userScrollEnabled = true
    ) { page ->
        val item = items[page]
        if (item.isVideo) {
            VideoPage(player = player, attached = page == pagerState.settledPage)
        } else {
            ImagePage(item = item)
        }
    }

    // Photos advance on a timer; videos stay put and loop until swiped away.
    LaunchedEffect(pagerState.settledPage, items) {
        if (items.size <= 1) return@LaunchedEffect
        val current = pagerState.settledPage
        if (items.getOrNull(current)?.isVideo != false) return@LaunchedEffect
        delay(AUTO_ADVANCE_DELAY_MS)
        pagerState.animateScrollToPage((current + 1) % items.size)
    }
}
