package com.andys8.kidsviewer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.andys8.kidsviewer.data.MediaAccess
import com.andys8.kidsviewer.data.MediaRepository
import com.andys8.kidsviewer.ui.KidsViewerApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_NAME = "kids_viewer"
private const val KEY_PERMISSION_ASKED = "permission_asked"

/** Long enough for pinning (and any confirmation dialog) to actually take effect. */
private const val PINNING_CHECK_DELAY_MS = 2500L

/** Long enough to let a reveal swipe finish, short enough that the bars don't linger. */
private const val BARS_REHIDE_INTERVAL_MS = 1000L

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
    private var pinningOutcomeChecked = false

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
        drawBehindDisplayCutout()
        enableImmersiveMode()
        keepSystemBarsHidden()

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
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Lets the window draw underneath the camera cutout instead of being held clear of it.
     * Without this the system keeps the app off the cutout strip and fills that band itself,
     * which looks like a grey bar pinned to the top edge — and, since it is not a system bar at
     * all, hiding the system bars never removes it and rotating only moves it.
     */
    private fun drawBehindDisplayCutout() {
        // The cutout layout mode arrived in API 28, and so did cutouts. Touching the field on an
        // older device throws NoSuchFieldError, so there is nothing to do and nothing to miss.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = mode }
    }

    /**
     * A swipe from the edge reveals translucent system bars over the photo, and they linger.
     * Android gives an app no way to refuse that gesture, so the next best thing is to put the
     * bars straight back.
     *
     * This re-asserts on a timer rather than reacting to an event, deliberately: transient bars
     * are drawn *over* the app without altering its insets — that is the whole point of them, so
     * that the layout doesn't shift — which means an insets listener is never told they appeared.
     * Re-hiding costs nothing when the bars are already hidden.
     */
    private fun keepSystemBarsHidden() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(BARS_REHIDE_INTERVAL_MS)
                    enableImmersiveMode()
                }
            }
        }
    }

    /**
     * Screen pinning is a bonus, never a requirement: the viewer has to keep working on a device
     * that refuses it, whether because the feature is absent, switched off in Settings, or
     * restricted by policy.
     */
    private fun startKioskPinning() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            return
        }

        try {
            startLockTask()
        } catch (e: RuntimeException) {
            // SecurityException, IllegalArgumentException and IllegalStateException have all been
            // seen here across OEM builds. None of them may take the app down.
        }

        if (pinningOutcomeChecked) return
        pinningOutcomeChecked = true

        // The platform may also decline silently, without failing, so the request itself proves
        // nothing. Pinning can take a moment to engage (and may show a confirmation dialog), so
        // judge it by the actual lock-task state shortly afterwards instead.
        lifecycleScope.launch {
            delay(PINNING_CHECK_DELAY_MS)
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                showPinningHelp.value = true
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
