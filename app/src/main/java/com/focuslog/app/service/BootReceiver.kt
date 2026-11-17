package com.focuslog.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focuslog.app.worker.DailyExportWorker

/**
 * Ensures the daily 2:00 AM IST export schedule is restored when the device restarts.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Device rebooted or package replaced. Rescheduling 2:00 AM IST daily export.")
            DailyExportWorker.scheduleDailyExport(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

