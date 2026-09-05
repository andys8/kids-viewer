package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.andys8.kidsviewer.data.MediaItem
import androidx.media3.common.MediaItem as Media3Item

@Composable
fun VideoPage(
    item: MediaItem,
    isActive: Boolean,
    onPlaybackError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(item.uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    DisposableEffect(item.uri) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlaybackError()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
        if (!isActive) {
            exoPlayer.pause()
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                setOnClickListener(null)
            }
        }
    )
}
