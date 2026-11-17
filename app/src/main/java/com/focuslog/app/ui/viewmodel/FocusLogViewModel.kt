package com.focuslog.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focuslog.app.data.datastore.TimerPreferences
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.local.LogEntry
import com.focuslog.app.data.repository.FocusLogRepository
import com.focuslog.app.util.TimerAlarmManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Main ViewModel orchestrating the focus timer, exact AlarmManager triggers,
 * DataStore timer state persistence, and Room database records for the card stack.
 */
class FocusLogViewModel(
    application: Application,
    private val repository: FocusLogRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FocusLogUiState())
    private var tickerJob: Job? = null

    val uiState: StateFlow<FocusLogUiState> = combine(
        _uiState,
        repository.allEntries
    ) { state, activeEntries ->
        state.copy(entries = activeEntries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusLogUiState()
    )

    init {
        restoreTimerStateFromDataStore()
    }

    private fun restoreTimerStateFromDataStore() {
        viewModelScope.launch {
            repository.timerState.collect { savedState ->
                if (savedState.isRunning) {
                    val now = System.currentTimeMillis()
                    val remainingSecs = ((savedState.endTimeMillis - now + 999L) / 1000L).coerceAtLeast(0L)
                    if (remainingSecs > 0) {
                        _uiState.update {
                            it.copy(
                                isTimerRunning = true,
                                remainingSeconds = remainingSecs,
                                totalSeconds = savedState.durationSeconds,
                                endTimeMillis = savedState.endTimeMillis,
                                currentSessionStartTimeMs = savedState.endTimeMillis - (savedState.durationSeconds * 1000L)
                            )
                        }
                        startCountdownTicker(savedState.endTimeMillis, savedState.durationSeconds)
                    } else {
                        // Timer completed while app was backgrounded/closed
                        _uiState.update {
                            it.copy(isTimerRunning = false, remainingSeconds = 0, endTimeMillis = 0)
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(isTimerRunning = false, remainingSeconds = 0, endTimeMillis = 0)
                    }
                    tickerJob?.cancel()
                }
            }
        }
    }

    fun onMinutesChange(minutes: String) {
        _uiState.update { it.copy(minutesInput = minutes) }
    }

    fun onSecondsChange(seconds: String) {
        _uiState.update { it.copy(secondsInput = seconds) }
    }

    fun resetInputs() {
        _uiState.update {
            it.copy(
                minutesInput = "25",
                secondsInput = "0"
            )
        }
    }

    /**
     * Starts the focus timer:
     * 1. Persists state to DataStore so it survives app kill and reboot.
     * 2. Schedules an exact wake-up alarm with AlarmManager.setAlarmClock() for Doze mode.
     * 3. Runs an in-app coroutine ticker computed from endTimeMillis for rock-solid precision.
     */
    fun startTimer() {
        val minutes = _uiState.value.minutesInput.toLongOrNull() ?: 0L
        val seconds = _uiState.value.secondsInput.toLongOrNull() ?: 0L
        val totalSecs = (minutes * 60) + seconds

        if (totalSecs <= 0) return

        val durationMs = totalSecs * 1000L
        val startTimeMs = System.currentTimeMillis()
        val triggerAtMs = startTimeMs + durationMs

        scheduleSystemAlarm(startTimeMs, durationMs, triggerAtMs)

        // Persist to DataStore
        viewModelScope.launch {
            repository.saveTimerState(
                endTimeMillis = triggerAtMs,
                durationSeconds = totalSecs,
                isRunning = true,
                isAwaitingEntry = false
            )
        }

        _uiState.update {
            it.copy(
                isTimerRunning = true,
                remainingSeconds = totalSecs,
                totalSeconds = totalSecs,
                endTimeMillis = triggerAtMs,
                currentSessionStartTimeMs = startTimeMs
            )
        }

        startCountdownTicker(triggerAtMs, totalSecs)
    }

    /**
     * Live ticker continuously derived from the persisted endTimeMillis.
     * Guarantees zero clock drift.
     */
    private fun startCountdownTicker(endTimeMillis: Long, totalSecs: Long) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((endTimeMillis - now + 999L) / 1000L).coerceAtLeast(0L)
                _uiState.update {
                    it.copy(
                        remainingSeconds = remaining,
                        totalSeconds = totalSecs,
                        isTimerRunning = (remaining > 0)
                    )
                }
                if (remaining <= 0) {
                    repository.setAwaitingEntry(true)
                    break
                }
                delay(500L)
            }
        }
    }

    /**
     * Cancels the active timer, clears DataStore state, and unregisters the AlarmManager alarm.
     */
    fun cancelTimer() {
        tickerJob?.cancel()
        tickerJob = null
        cancelSystemAlarm()

        viewModelScope.launch {
            repository.clearTimerState()
        }

        _uiState.update {
            it.copy(
                isTimerRunning = false,
                remainingSeconds = 0L,
                totalSeconds = 0L,
                endTimeMillis = 0L
            )
        }
    }

    /**
     * Inserts a submitted log entry into Room and resets the timer DataStore state.
     */
    fun insertLogEntry(
        text: String,
        startedAtMillis: Long,
        completedAtMillis: Long,
        durationSeconds: Long
    ) {
        viewModelScope.launch {
            val entry = LogEntry(
                text = text,
                timerDurationSeconds = durationSeconds,
                startedAtMillis = startedAtMillis,
                completedAtMillis = completedAtMillis
            )
            repository.insertEntry(entry)
            repository.clearTimerState()
        }
    }

    private fun scheduleSystemAlarm(startTimeMs: Long, durationMs: Long, triggerAtMs: Long) {
        val context = getApplication<Application>()
        TimerAlarmManager.scheduleTimerAlarm(
            context = context,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            triggerAtMs = triggerAtMs
        )
    }

    private fun cancelSystemAlarm() {
        val context = getApplication<Application>()
        TimerAlarmManager.cancelTimerAlarm(context)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = FocusLogDatabase.getDatabase(application)
                    val timerPreferences = TimerPreferences(application)
                    val repo = FocusLogRepository(db.logEntryDao(), timerPreferences)
                    return FocusLogViewModel(application, repo) as T
                }
            }
    }
}
