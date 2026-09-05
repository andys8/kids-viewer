package com.andys8.kidsviewer.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long
)
