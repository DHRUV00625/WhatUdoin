package com.focuslog.app.data.repository

import android.content.Context
import com.focuslog.app.data.export.MarkdownExporter
import com.focuslog.app.data.local.FocusLogDao
import com.focuslog.app.data.local.FocusLogEntry
import com.focuslog.app.domain.repository.IFocusLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Concrete Repository implementing IFocusLogRepository.
 * Mediates between Room Database, UI ViewModels, and the Markdown Exporter.
 */
class FocusLogRepository(
    private val context: Context,
    private val dao: FocusLogDao
) : IFocusLogRepository {

    /**
     * Observes the active entries for the app dashboard (newest entry first).
     */
    override val activeEntries: Flow<List<FocusLogEntry>> = dao.getAllActiveEntries()

    /**
     * Inserts a newly submitted entry after an alarm is completed.
     */
    override suspend fun logActivity(
        taskDescription: String,
        startTimeMs: Long,
        endTimeMs: Long,
        durationMs: Long,
        dateString: String
    ): Long = withContext(Dispatchers.IO) {
        val entry = FocusLogEntry(
            taskDescription = taskDescription,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            durationMs = durationMs,
            dateString = dateString
        )
        dao.insertEntry(entry)
    }

    /**
     * Exports all unexported entries to disk as Markdown files (YYYY-MM-DD.md)
     * and clears the exported entries from the database.
     */
    override suspend fun exportAndClearDailyEntries(): Int = withContext(Dispatchers.IO) {
        val entries = dao.getAllUnexportedEntries()
        if (entries.isEmpty()) return@withContext 0

        val exported = MarkdownExporter.exportEntriesToMarkdown(context, entries)
        if (exported.isNotEmpty()) {
            dao.deleteEntries(exported)
        }
        exported.size
    }
}
