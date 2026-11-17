package com.focuslog.app.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focuslog.app.data.export.MarkdownExporter
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.util.TimeUtils
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker executed daily at 2:00 AM IST (Asia/Kolkata).
 * Exports all completed sessions into a Markdown file (YYYY-MM-DD.md)
 * and purges them from the app database.
 */
class DailyExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting 2:00 AM IST Daily Export Worker...")
        return try {
            val database = FocusLogDatabase.getDatabase(applicationContext)
            val dao = database.logEntryDao()
            val entries = dao.getAll()

            val exported = MarkdownExporter.exportEntriesToMarkdown(applicationContext, entries)
            if (exported.isNotEmpty()) {
                dao.deleteAll()
                Log.d(TAG, "Daily reset completed: ${exported.size} entries exported and purged.")
            } else {
                Log.d(TAG, "No entries to export.")
            }

            // Reschedule next run for the upcoming 2:00 AM IST
            scheduleDailyExport(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute daily export worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DailyExportWorker"
        const val UNIQUE_WORK_NAME = "focuslog_daily_export_2am_ist"

        /**
         * Schedules the worker to execute at the next 2:00 AM IST.
         */
        fun scheduleDailyExport(context: Context) {
            val initialDelayMs = TimeUtils.calculateDelayUntilNext2AmIst()
            Log.d(TAG, "Scheduling next daily export in ${initialDelayMs / 1000} seconds (2:00 AM IST)")

            val workRequest = OneTimeWorkRequestBuilder<DailyExportWorker>()
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            try {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } catch (e: Exception) {
                Log.w(TAG, "WorkManager could not enqueue daily export", e)
            }
        }
    }
}
