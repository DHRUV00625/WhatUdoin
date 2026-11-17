package com.focuslog.app.domain.repository

import com.focuslog.app.data.datastore.TimerState
import com.focuslog.app.data.local.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface defining repository contracts for FocusLog records and active timer state.
 */
interface IFocusLogRepository {
    val allEntries: Flow<List<LogEntry>>
    val timerState: Flow<TimerState>

    suspend fun insertEntry(entry: LogEntry): Long
    suspend fun getAllEntries(): List<LogEntry>
    suspend fun deleteAllEntries()

    suspend fun saveTimerState(
        endTimeMillis: Long,
        durationSeconds: Long,
        isRunning: Boolean,
        isAwaitingEntry: Boolean
    )
    suspend fun setAwaitingEntry(isAwaiting: Boolean)
    suspend fun clearTimerState()
}
