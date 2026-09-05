package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.andys8.kidsviewer.data.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_ADVANCE_DELAY_MS = 4000L

@Composable
fun PagerScreen(items: List<MediaItem>) {
    val pagerState = rememberPagerState(initialPage = 0) { items.size }
    val scope = rememberCoroutineScope()

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
            VideoPage(
                item = item,
                isActive = page == pagerState.currentPage,
                onPlaybackError = {
                    val next = (pagerState.currentPage + 1) % items.size
                    scope.launch { pagerState.animateScrollToPage(next) }
                }
            )
        } else {
            ImagePage(item = item)
        }
    }

    LaunchedEffect(pagerState.settledPage, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        val current = pagerState.settledPage
        if (items[current].isVideo) return@LaunchedEffect
        delay(AUTO_ADVANCE_DELAY_MS)
        val next = (current + 1) % items.size
        pagerState.animateScrollToPage(next)
    }
}
