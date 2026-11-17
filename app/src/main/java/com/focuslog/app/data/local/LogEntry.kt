package com.focuslog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged focus session entry in Room.
 *
 * @property id Unique auto-generated identifier.
 * @property text The user-submitted description of what they worked on.
 * @property timerDurationSeconds Configured focus timer duration in seconds.
 * @property startedAtMillis Epoch timestamp when timer started.
 * @property completedAtMillis Epoch timestamp when task was logged / submitted.
 */
@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timerDurationSeconds: Long,
    val startedAtMillis: Long,
    val completedAtMillis: Long
)

