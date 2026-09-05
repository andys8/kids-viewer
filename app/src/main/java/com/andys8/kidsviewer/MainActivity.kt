package com.andys8.kidsviewer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andys8.kidsviewer.data.MediaAccess
import com.andys8.kidsviewer.data.MediaRepository
import com.andys8.kidsviewer.ui.KidsViewerApp

private const val PREFS_NAME = "kids_viewer"
private const val KEY_PERMISSION_ASKED = "permission_asked"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(MediaRepository(applicationContext)) as T
            }
        }
    }

    private val showPinningHelp = mutableStateOf(false)
    private val crashReport = mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Re-check rather than trusting the raw result: on Android 14+ "Select photos"
        // grants partial access, which is usable even though the full grants come back denied.
        viewModel.onAccessChanged(MediaAccess.hasAccess(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(this)
        crashReport.value = CrashReporter.consume(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableImmersiveMode()

        requestAccessIfNeeded()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val pinningHelpVisible by showPinningHelp
            val crash by crashReport
            BackHandler(enabled = true) { /* no-op: block back gesture/button */ }
            KidsViewerApp(
                uiState = uiState,
                showPinningHelp = pinningHelpVisible,
                crashReport = crash,
                onGrantPermission = { permissionLauncher.launch(MediaAccess.permissionsToRequest) },
                onOpenSettings = ::openAppSettings,
                onDismissPinningHelp = { showPinningHelp.value = false },
                onDismissCrashReport = { crashReport.value = null }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
        startKioskPinning()
        // Picks up access granted from the system settings screen.
        viewModel.onAccessChanged(MediaAccess.hasAccess(this))
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    /**
     * Shows the system permission dialog only when access is actually missing, and only the first
     * time ever. After that the in-app screen offers a button, so the app never nags on launch.
     */
    private fun requestAccessIfNeeded() {
        if (MediaAccess.hasAccess(this)) {
            viewModel.onAccessChanged(true)
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PERMISSION_ASKED, false)) {
            viewModel.onAccessChanged(false)
            return
        }

        prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
        permissionLauncher.launch(MediaAccess.permissionsToRequest)
    }

    private fun enableImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun startKioskPinning() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            return
        }
        try {
            startLockTask()
        } catch (e: IllegalArgumentException) {
            showPinningHelp.value = true
        } catch (e: IllegalStateException) {
            showPinningHelp.value = true
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
