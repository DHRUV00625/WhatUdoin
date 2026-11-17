package com.focuslog.app.data.repository

import com.focuslog.app.data.datastore.TimerPreferences
import com.focuslog.app.data.datastore.TimerState
import com.focuslog.app.data.local.LogEntry
import com.focuslog.app.data.local.LogEntryDao
import com.focuslog.app.domain.repository.IFocusLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Concrete Repository wrapping the Room LogEntryDao and DataStore TimerPreferences.
 */
class FocusLogRepository(
    private val dao: LogEntryDao,
    private val timerPreferences: TimerPreferences
) : IFocusLogRepository {

    /**
     * Observes all log entries ordered by newest first.
     */
    override val allEntries: Flow<List<LogEntry>> = dao.observeAllOrderedByNewest()

    /**
     * Observes the active timer state persisted in DataStore.
     */
    override val timerState: Flow<TimerState> = timerPreferences.timerState

    /**
     * Inserts a new log entry.
     */
    override suspend fun insertEntry(entry: LogEntry): Long = withContext(Dispatchers.IO) {
        dao.insert(entry)
    }

    /**
     * Retrieves all log entries as a list.
     */
    override suspend fun getAllEntries(): List<LogEntry> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    /**
     * Deletes all log entries from the database.
     */
    override suspend fun deleteAllEntries() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    /**
     * Saves active timer state to DataStore.
     */
    override suspend fun saveTimerState(
        endTimeMillis: Long,
        durationSeconds: Long,
        isRunning: Boolean,
        isAwaitingEntry: Boolean
    ) = withContext(Dispatchers.IO) {
        timerPreferences.saveTimerState(
            endTimeMillis = endTimeMillis,
            durationSeconds = durationSeconds,
            isRunning = isRunning,
            isAwaitingEntry = isAwaitingEntry
        )
    }

    /**
     * Updates awaiting entry state in DataStore.
     */
    override suspend fun setAwaitingEntry(isAwaiting: Boolean) = withContext(Dispatchers.IO) {
        timerPreferences.setAwaitingEntry(isAwaiting)
    }

    /**
     * Clears active timer state in DataStore.
     */
    override suspend fun clearTimerState() = withContext(Dispatchers.IO) {
        timerPreferences.clearTimerState()
    }
}
