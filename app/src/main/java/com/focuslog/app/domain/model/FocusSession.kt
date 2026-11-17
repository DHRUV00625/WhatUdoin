package com.focuslog.app.domain.model

/**
 * Domain representation of a completed focus session.
 */
data class FocusSession(
    val id: Long = 0,
    val taskDescription: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val dateString: String
)

