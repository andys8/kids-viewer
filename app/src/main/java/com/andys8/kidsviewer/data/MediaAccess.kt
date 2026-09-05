package com.andys8.kidsviewer.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Media permissions differ per Android version, and on Android 14+ the user can grant access to
 * only a selection of items. Partial access still shows photos, so it counts as granted — treating
 * it as "denied" would re-prompt on every single launch.
 */
object MediaAccess {

    val permissionsToRequest: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAccess(context: Context): Boolean {
        val full = isGranted(context, Manifest.permission.READ_MEDIA_IMAGES) &&
            isGranted(context, Manifest.permission.READ_MEDIA_VIDEO)

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                full || isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> full

            else -> isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
