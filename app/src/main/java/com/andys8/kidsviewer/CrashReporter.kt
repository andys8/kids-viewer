package com.andys8.kidsviewer

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "kids_viewer"
private const val KEY_LAST_CRASH = "last_crash"
private const val MAX_REPORT_CHARS = 4000

/**
 * Stores the stack trace of a fatal crash so it can be shown on the next launch. Without a
 * computer attached there is no logcat, so this is the only way to see why the app died.
 */
object CrashReporter {

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val report = "$timestamp\n\n${throwable.stackTraceToString()}".take(MAX_REPORT_CHARS)
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, report)
                    .commit()
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Returns the last recorded crash, if any, and clears it. */
    fun consume(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val report = prefs.getString(KEY_LAST_CRASH, null)
        if (report != null) {
            prefs.edit().remove(KEY_LAST_CRASH).apply()
        }
        return report
    }
}
