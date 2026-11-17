package com.focuslog.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focuslog.app.data.datastore.TimerPreferences
import com.focuslog.app.ui.screens.MainActivity
import com.focuslog.app.util.TimerAlarmManager
import com.focuslog.app.worker.DailyExportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ensures the daily 2:00 AM IST export schedule and any running timer or pending alarm
 * are reliably restored after device reboot.
 */
class BootReceiver(
    private val coroutineScope: CoroutineScope? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Device rebooted or package replaced. Restoring state...")

            // 1. Reschedule daily 2:00 AM IST export
            DailyExportWorker.scheduleDailyExport(context)

            // 2. Check and restore active timer or alarm awaiting entry
            val pendingResult = try { goAsync() } catch (_: Exception) { null }
            val activeScope = coroutineScope ?: CoroutineScope(Dispatchers.IO)
            activeScope.launch {
                try {
                    val preferences = TimerPreferences(context)
                    val state = preferences.timerState.first()
                    val now = System.currentTimeMillis()

                    if (state.isAwaitingEntry) {
                        Log.d(TAG, "Alarm was awaiting entry before reboot! Triggering alarm immediately.")
                        val startTimeMs = state.endTimeMillis - (state.durationSeconds * 1000L)
                        val durationMs = state.durationSeconds * 1000L

                        try {
                            AlarmService.start(context, startTimeMs, durationMs)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not start AlarmService", e)
                        }

                        try {
                            val alarmIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra(AlarmService.EXTRA_START_TIME, startTimeMs)
                                putExtra(AlarmService.EXTRA_DURATION_MS, durationMs)
                            }
                            context.startActivity(alarmIntent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not start MainActivity", e)
                        }
                    } else if (state.isRunning) {
                        val durationMs = state.durationSeconds * 1000L
                        val startTimeMs = state.endTimeMillis - durationMs

                        if (state.endTimeMillis > now) {
                            Log.d(TAG, "Timer was running before reboot and has not expired. Rescheduling exact alarm.")
                            TimerAlarmManager.scheduleTimerAlarm(
                                context = context,
                                startTimeMs = startTimeMs,
                                durationMs = durationMs,
                                triggerAtMs = state.endTimeMillis
                            )
                        } else {
                            Log.d(TAG, "Timer expired while device was powered off! Triggering alarm now.")
                            preferences.setAwaitingEntry(true)

                            try {
                                AlarmService.start(context, startTimeMs, durationMs)
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not start AlarmService", e)
                            }

                            try {
                                val alarmIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra(AlarmService.EXTRA_START_TIME, startTimeMs)
                                    putExtra(AlarmService.EXTRA_DURATION_MS, durationMs)
                                }
                                context.startActivity(alarmIntent)
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not start MainActivity", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring timer state on boot", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
