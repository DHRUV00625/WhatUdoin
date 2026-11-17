package com.focuslog.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focuslog.app.ui.screens.AlarmActivity

/**
 * BroadcastReceiver triggered by AlarmManager when the focus countdown reaches zero.
 * Operates reliably even if the device is sleeping or the application is closed.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val startTimeMs = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
        val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)

        Log.d(TAG, "Timer ended! Starting AlarmService & launching AlarmActivity.")

        // 1. Start foreground alarm service for continuous loud audio
        AlarmService.start(context, startTimeMs, durationMs)

        // 2. Launch AlarmActivity to display on screen (or over lock screen)
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmService.EXTRA_START_TIME, startTimeMs)
            putExtra(AlarmService.EXTRA_DURATION_MS, durationMs)
        }
        context.startActivity(alarmIntent)
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
    }
}

