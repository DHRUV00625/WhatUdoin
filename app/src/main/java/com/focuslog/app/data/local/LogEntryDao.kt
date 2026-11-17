package com.focuslog.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for LogEntry.
 */
@Dao
interface LogEntryDao {

    /**
     * Inserts a new LogEntry and returns the inserted row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry): Long

    /**
     * Observes all log entries ordered by newest first (descending completedAtMillis).
     */
    @Query("SELECT * FROM log_entries ORDER BY completedAtMillis DESC")
    fun observeAllOrderedByNewest(): Flow<List<LogEntry>>

    /**
     * Retrieves all log entries as a one-shot query.
     */
    @Query("SELECT * FROM log_entries ORDER BY completedAtMillis DESC")
    suspend fun getAll(): List<LogEntry>

    /**
     * Deletes all log entries from the database.
     */
    @Query("DELETE FROM log_entries")
    suspend fun deleteAll()
}

