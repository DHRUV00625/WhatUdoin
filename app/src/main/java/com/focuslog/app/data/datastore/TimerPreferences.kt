package com.focuslog.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state_prefs")

/**
 * Data class representing the persisted active timer state.
 */
data class TimerState(
    val endTimeMillis: Long = 0L,
    val durationSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isAwaitingEntry: Boolean = false
)

/**
 * Persists and observes active timer state using Jetpack DataStore Preferences.
 * Ensures the timer and alarm status survive app backgrounding, kill, and reboot.
 */
class TimerPreferences(private val context: Context) {

    private object PreferencesKeys {
        val END_TIME_MILLIS = longPreferencesKey("end_time_millis")
        val DURATION_SECONDS = longPreferencesKey("duration_seconds")
        val IS_RUNNING = booleanPreferencesKey("is_running")
        val IS_AWAITING_ENTRY = booleanPreferencesKey("is_awaiting_entry")
    }

    val timerState: Flow<TimerState> = context.timerDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            TimerState(
                endTimeMillis = preferences[PreferencesKeys.END_TIME_MILLIS] ?: 0L,
                durationSeconds = preferences[PreferencesKeys.DURATION_SECONDS] ?: 0L,
                isRunning = preferences[PreferencesKeys.IS_RUNNING] ?: false,
                isAwaitingEntry = preferences[PreferencesKeys.IS_AWAITING_ENTRY] ?: false
            )
        }

    suspend fun saveTimerState(
        endTimeMillis: Long,
        durationSeconds: Long,
        isRunning: Boolean,
        isAwaitingEntry: Boolean
    ) {
        context.timerDataStore.edit { preferences ->
            preferences[PreferencesKeys.END_TIME_MILLIS] = endTimeMillis
            preferences[PreferencesKeys.DURATION_SECONDS] = durationSeconds
            preferences[PreferencesKeys.IS_RUNNING] = isRunning
            preferences[PreferencesKeys.IS_AWAITING_ENTRY] = isAwaitingEntry
        }
    }

    suspend fun setAwaitingEntry(isAwaiting: Boolean) {
        context.timerDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AWAITING_ENTRY] = isAwaiting
            preferences[PreferencesKeys.IS_RUNNING] = false
        }
    }

    suspend fun clearTimerState() {
        context.timerDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.END_TIME_MILLIS)
            preferences.remove(PreferencesKeys.DURATION_SECONDS)
            preferences[PreferencesKeys.IS_RUNNING] = false
            preferences[PreferencesKeys.IS_AWAITING_ENTRY] = false
        }
    }
}

