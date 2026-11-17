package com.focuslog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.repository.FocusLogRepository
import com.focuslog.app.service.AlarmService
import com.focuslog.app.worker.DailyExportWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class for FocusLog.
 * Initializes notification channels and schedules the recurring 2:00 AM IST daily export.
 */
class FocusLogApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleDailyExportWorker()
        performStartupExportCheck()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                AlarmService.CHANNEL_ID,
                "FocusLog Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm notifications when a focus session completes."
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(alarmChannel)
        }
    }

    private fun scheduleDailyExportWorker() {
        DailyExportWorker.scheduleDailyExport(this)
    }

    /**
     * Safety catch-up check:
     * If the phone was powered off during 2:00 AM IST, this exports and clears
     * any entries from past days on next app launch.
     */
    private fun performStartupExportCheck() {
        applicationScope.launch {
            try {
                val db = FocusLogDatabase.getDatabase(applicationContext)
                val repo = FocusLogRepository(applicationContext, db.focusLogDao())
                val count = repo.exportAndClearDailyEntries()
                if (count > 0) {
                    Log.d("FocusLogApplication", "Startup catch-up: exported and cleared $count entries.")
                }
            } catch (e: Exception) {
                Log.e("FocusLogApplication", "Startup export check failed", e)
            }
        }
    }
}
