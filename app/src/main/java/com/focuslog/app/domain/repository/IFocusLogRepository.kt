package com.focuslog.app.domain.repository

import com.focuslog.app.data.local.FocusLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface defining repository contracts for focus log records.
 */
interface IFocusLogRepository {
    val activeEntries: Flow<List<FocusLogEntry>>

    suspend fun logActivity(
        taskDescription: String,
        startTimeMs: Long,
        endTimeMs: Long,
        durationMs: Long,
        dateString: String
    ): Long

    suspend fun exportAndClearDailyEntries(): Int
}

