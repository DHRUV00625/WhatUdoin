package com.focuslog.app.ui.viewmodel

import com.focuslog.app.data.local.LogEntry

/**
 * UI State representing the current dashboard, timer countdown, and active card stack.
 */
data class FocusLogUiState(
    val entries: List<LogEntry> = emptyList(),
    val isTimerRunning: Boolean = false,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val minutesInput: String = "25",
    val secondsInput: String = "0",
    val currentSessionStartTimeMs: Long = 0L
)
