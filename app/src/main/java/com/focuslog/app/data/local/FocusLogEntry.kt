package com.focuslog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single completed focus session logged by the user.
 *
 * @property id Auto-generated primary key.
 * @property taskDescription Mandatory user description of what was accomplished during the timer.
 * @property startTimeMs Timestamp when the timer was started (epoch millis).
 * @property endTimeMs Timestamp when the timer ended and was submitted (epoch millis).
 * @property durationMs Total duration of the focus timer in milliseconds.
 * @property dateString Date string in IST format ("YYYY-MM-DD") for grouping and daily exports.
 * @property isExported Flag marking whether this entry was exported to a markdown file.
 */
@Entity(tableName = "focus_log_entries")
data class FocusLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskDescription: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val dateString: String,
    val isExported: Boolean = false
)
