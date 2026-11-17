package com.focuslog.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FocusLog entries.
 */
@Dao
interface FocusLogDao {

    /**
     * Observes all active (non-exported) entries ordered from newest to oldest.
     * The newest entry will be at index 0 (top of the card stack).
     */
    @Query("SELECT * FROM focus_log_entries WHERE isExported = 0 ORDER BY endTimeMs DESC")
    fun getAllActiveEntries(): Flow<List<FocusLogEntry>>

    /**
     * Retrieves all unexported entries up to a specific date for daily markdown export.
     */
    @Query("SELECT * FROM focus_log_entries WHERE isExported = 0 ORDER BY startTimeMs ASC")
    suspend fun getAllUnexportedEntries(): List<FocusLogEntry>

    /**
     * Inserts a newly logged entry upon alarm dismissal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FocusLogEntry): Long

    /**
     * Deletes exported entries to clear the app state after 2:00 AM IST daily export.
     */
    @Delete
    suspend fun deleteEntries(entries: List<FocusLogEntry>)

    /**
     * Clears all entries from the table.
     */
    @Query("DELETE FROM focus_log_entries")
    suspend fun clearAll()
}
