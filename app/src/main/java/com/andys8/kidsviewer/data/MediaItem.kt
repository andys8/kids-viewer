package com.andys8.kidsviewer.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateAdded: Long,
    /**
     * The colour a bundled picture sits on, as a packed ARGB int. Photos and videos from the
     * phone carry their own background in the frame and leave this unset, which keeps them on
     * black exactly as before.
     */
    val backdropColor: Int? = null
)
