package com.focuslog.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.focuslog.app.service.AlarmReceiver
import com.focuslog.app.service.AlarmService
import com.focuslog.app.ui.screens.MainActivity

/**
 * Manages reliable scheduling of exact alarms via AlarmManager.setAlarmClock().
 * Guarantees firing even when device is in deep Doze mode.
 */
object TimerAlarmManager {

    private const val TAG = "TimerAlarmManager"
    const val SHOW_ALARM_REQUEST_CODE = 2002
    const val ALARM_OPERATION_REQUEST_CODE = 2001

    /**
     * Checks whether the application has permission to schedule exact alarms.
     * Always returns true on Android 11 (API 30) and below.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Requests user permission to schedule exact alarms on Android 12+.
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Schedules an exact alarm using AlarmManager.setAlarmClock().
     * setAlarmClock() is the highest-priority alarm type in Android that guarantees
     * wake-up even from Deep Doze power-saving states.
     */
    fun scheduleTimerAlarm(
        context: Context,
        startTimeMs: Long,
        durationMs: Long,
        triggerAtMs: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. PendingIntent opened if the user taps the system clock or alarm indicator
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmService.EXTRA_START_TIME, startTimeMs)
            putExtra(AlarmService.EXTRA_DURATION_MS, durationMs)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            SHOW_ALARM_REQUEST_CODE,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. PendingIntent delivered to AlarmReceiver when the timer expires
        val operationIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_START_TIME, startTimeMs)
            putExtra(AlarmReceiver.EXTRA_DURATION_MS, durationMs)
        }
        val operationPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_OPERATION_REQUEST_CODE,
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMs, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, operationPendingIntent)
            Log.d(TAG, "Successfully scheduled AlarmClock at $triggerAtMs (duration: ${durationMs}ms)")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException on setAlarmClock, falling back to setExactAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        operationPendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        operationPendingIntent
                    )
                }
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Failed to schedule fallback alarm", fallbackException)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, operationPendingIntent)
            }
        }
    }

    /**
     * Cancels any scheduled timer alarms.
     */
    fun cancelTimerAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operationIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_OPERATION_REQUEST_CODE,
            operationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled scheduled timer alarm.")
    }
}

