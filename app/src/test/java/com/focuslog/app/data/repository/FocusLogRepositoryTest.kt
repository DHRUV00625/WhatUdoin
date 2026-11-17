package com.focuslog.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focuslog.app.data.datastore.TimerPreferences
import com.focuslog.app.data.local.FocusLogDatabase
import com.focuslog.app.data.local.LogEntry
import com.focuslog.app.data.local.LogEntryDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FocusLogRepository wrapping DAO and Timer DataStore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FocusLogRepositoryTest {

    private lateinit var database: FocusLogDatabase
    private lateinit var dao: LogEntryDao
    private lateinit var timerPreferences: TimerPreferences
    private lateinit var repository: FocusLogRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.logEntryDao()
        timerPreferences = TimerPreferences(context)
        repository = FocusLogRepository(dao, timerPreferences)
    }

    @After
    fun tearDown() = runTest {
        repository.clearTimerState()
        database.close()
    }

    @Test
    fun repository_insertAndGetAllEntries_worksProperly() = runTest {
        val entry = LogEntry(
            text = "Completed code review",
            timerDurationSeconds = 1800L,
            startedAtMillis = 50000L,
            completedAtMillis = 51800L
        )

        val id = repository.insertEntry(entry)
        assertTrue(id > 0)

        val all = repository.getAllEntries()
        assertEquals(1, all.size)
        assertEquals("Completed code review", all[0].text)
    }

    @Test
    fun repository_observeAllEntries_emitsUpdates() = runTest {
        val entry = LogEntry(
            text = "Feature implementation",
            timerDurationSeconds = 1200L,
            startedAtMillis = 10000L,
            completedAtMillis = 11200L
        )

        repository.insertEntry(entry)

        val flowEmission = repository.allEntries.first()
        assertEquals(1, flowEmission.size)
        assertEquals("Feature implementation", flowEmission[0].text)
    }

    @Test
    fun repository_deleteAllEntries_clearsTable() = runTest {
        repository.insertEntry(
            LogEntry(
                text = "Task to delete",
                timerDurationSeconds = 600L,
                startedAtMillis = 1000L,
                completedAtMillis = 1600L
            )
        )
        assertEquals(1, repository.getAllEntries().size)

        repository.deleteAllEntries()
        assertTrue(repository.getAllEntries().isEmpty())
    }

    @Test
    fun repository_timerStatePersistence_savesAndRestores() = runTest {
        val endTime = System.currentTimeMillis() + 1500000L
        val duration = 1500L

        repository.saveTimerState(
            endTimeMillis = endTime,
            durationSeconds = duration,
            isRunning = true,
            isAwaitingEntry = false
        )

        val state = repository.timerState.first()
        assertEquals(endTime, state.endTimeMillis)
        assertEquals(duration, state.durationSeconds)
        assertTrue(state.isRunning)
        assertFalse(state.isAwaitingEntry)

        repository.setAwaitingEntry(true)
        val awaitingState = repository.timerState.first()
        assertTrue(awaitingState.isAwaitingEntry)
        assertFalse(awaitingState.isRunning)

        repository.clearTimerState()
        val clearedState = repository.timerState.first()
        assertFalse(clearedState.isRunning)
        assertFalse(clearedState.isAwaitingEntry)
    }
}

