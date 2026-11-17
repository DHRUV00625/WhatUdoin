package com.focuslog.app.data.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.focuslog.app.data.local.FocusLogEntry
import com.focuslog.app.util.TimeUtils
import java.io.File
import java.io.FileWriter

/**
 * Handles exporting FocusLog entries to daily Markdown files (YYYY-MM-DD.md).
 */
object MarkdownExporter {

    private const val TAG = "MarkdownExporter"
    private const val DIRECTORY_NAME = "FocusLog"

    /**
     * Exports a list of entries grouped by date into markdown files.
     * Saved to: /Android/data/com.focuslog.app/files/Documents/FocusLog/{YYYY-MM-DD}.md
     *
     * @return List of entries that were successfully exported to disk.
     */
    fun exportEntriesToMarkdown(context: Context, entries: List<FocusLogEntry>): List<FocusLogEntry> {
        if (entries.isEmpty()) {
            Log.d(TAG, "No entries available to export.")
            return emptyList()
        }

        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val exportDir = File(baseDir, DIRECTORY_NAME)
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Log.e(TAG, "Failed to create export directory: ${exportDir.absolutePath}")
            return emptyList()
        }

        val exportedEntries = mutableListOf<FocusLogEntry>()
        val groupedByDate = entries.groupBy { it.dateString }

        for ((dateString, dateEntries) in groupedByDate) {
            val file = File(exportDir, "$dateString.md")
            val content = buildMarkdownContent(dateString, dateEntries)

            try {
                FileWriter(file, false).use { writer ->
                    writer.write(content)
                }
                Log.d(TAG, "Successfully exported ${dateEntries.size} entries to ${file.absolutePath}")
                exportedEntries.addAll(dateEntries)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing markdown file for date $dateString", e)
            }
        }

        return exportedEntries
    }

    /**
     * Builds the formatted markdown document for a given day.
     */
    private fun buildMarkdownContent(dateString: String, entries: List<FocusLogEntry>): String {
        val totalSessions = entries.size
        val totalDurationMs = entries.sumOf { it.durationMs }
        val formattedTotalDuration = TimeUtils.formatDuration(totalDurationMs)

        return buildString {
            appendLine("# FocusLog — $dateString")
            appendLine()
            appendLine("## Summary")
            appendLine("- **Total Sessions**: $totalSessions")
            appendLine("- **Total Focus Time**: $formattedTotalDuration")
            appendLine("- **Timezone**: Asia/Kolkata (IST)")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Session Entries")
            appendLine()

            entries.sortedBy { it.startTimeMs }.forEachIndexed { index, entry ->
                val startTimeStr = TimeUtils.formatEpochToTimeIst(entry.startTimeMs)
                val endTimeStr = TimeUtils.formatEpochToTimeIst(entry.endTimeMs)
                val durationStr = TimeUtils.formatDuration(entry.durationMs)

                appendLine("### ${index + 1}. $startTimeStr – $endTimeStr ($durationStr)")
                appendLine(entry.taskDescription.trim())
                appendLine()
            }
        }
    }
}
