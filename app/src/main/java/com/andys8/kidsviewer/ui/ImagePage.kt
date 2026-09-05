package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.andys8.kidsviewer.data.MediaItem

@Composable
fun ImagePage(item: MediaItem, modifier: Modifier = Modifier) {
    AsyncImage(
        model = item.uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
