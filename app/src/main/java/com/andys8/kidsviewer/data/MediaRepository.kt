package com.andys8.kidsviewer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CAMERA_BUCKET_NAME = "Camera"

class MediaRepository(private val context: Context) {

    suspend fun loadCameraMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        val selection = "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} = ? " +
            "AND (${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? " +
            "OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"

        val selectionArgs = arrayOf(
            CAMERA_BUCKET_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val items = mutableListOf<MediaItem>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val isVideo = cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                items += MediaItem(
                    id = id,
                    uri = ContentUris.withAppendedId(baseUri, id),
                    isVideo = isVideo,
                    dateAdded = cursor.getLong(dateAddedCol)
                )
            }
        }

        items
    }
}
