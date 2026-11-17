package com.focuslog.app.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.repository.FocusLogRepository
import com.focuslog.app.service.AlarmReceiver
import com.focuslog.app.service.AlarmService
import com.focuslog.app.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Main ViewModel orchestrating the focus timer, exact AlarmManager triggers,
 * and Room database records for the card stack.
 */
class FocusLogViewModel(
    application: Application,
    private val repository: FocusLogRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FocusLogUiState())
    private var timerJob: Job? = null

    val uiState: StateFlow<FocusLogUiState> = combine(
        _uiState,
        repository.activeEntries
    ) { state, activeEntries ->
        state.copy(entries = activeEntries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusLogUiState()
    )

    fun onMinutesChange(minutes: String) {
        _uiState.update { it.copy(minutesInput = minutes) }
    }

    fun onSecondsChange(seconds: String) {
        _uiState.update { it.copy(secondsInput = seconds) }
    }

    /**
     * Starts the focus timer:
     * 1. Schedules an exact wake-up alarm with AlarmManager so it sounds even if closed/locked.
     * 2. Runs an in-app coroutine ticker for smooth UI updates when the app is foregrounded.
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

        _uiState.update {
            it.copy(
                isTimerRunning = true,
                remainingSeconds = totalSecs,
                totalSeconds = totalSecs,
                currentSessionStartTimeMs = startTimeMs
            )
        }

        startCountdownJob(totalSecs)
    }

    private fun startCountdownJob(totalSecs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var currentRemaining = totalSecs
            while (currentRemaining > 0) {
                delay(1000L)
                currentRemaining--
                _uiState.update { it.copy(remainingSeconds = currentRemaining) }
            }
            // Timer expired, UI resets running state; AlarmManager / AlarmService handles the loud ring
            _uiState.update { it.copy(isTimerRunning = false, remainingSeconds = 0) }
        }
    }

    /**
     * Cancels the active timer and unregisters the AlarmManager alarm.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        cancelSystemAlarm()

        _uiState.update {
            it.copy(
                isTimerRunning = false,
                remainingSeconds = 0L,
                totalSeconds = 0L
            )
        }
    }

    private fun scheduleSystemAlarm(startTimeMs: Long, durationMs: Long, triggerAtMs: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_START_TIME, startTimeMs)
            putExtra(AlarmReceiver.EXTRA_DURATION_MS, durationMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled exact alarm for $triggerAtMs (duration: ${durationMs}ms)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing or denied", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    private fun cancelSystemAlarm() {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        private const val TAG = "FocusLogViewModel"
        const val ALARM_REQUEST_CODE = 2001

        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = FocusLogDatabase.getDatabase(application)
                    val repo = FocusLogRepository(application, db.focusLogDao())
                    return FocusLogViewModel(application, repo) as T
                }
            }
    }
}
