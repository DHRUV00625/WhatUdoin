package com.focuslog.app.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.focuslog.app.data.datastore.TimerPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BootReceiver state restoration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var timerPreferences: TimerPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        try {
            androidx.work.WorkManager.initialize(context, config)
        } catch (_: Exception) {}
        timerPreferences = TimerPreferences(context)
    }

    @After
    fun tearDown() = runBlocking {
        timerPreferences.clearTimerState()
    }

    @Test
    fun bootReceiver_restoresStateWhenRunningTimerExists() = runBlocking {
        val now = System.currentTimeMillis()
        val futureEnd = now + 1500000L
        val durationSec = 1500L

        timerPreferences.saveTimerState(
            endTimeMillis = futureEnd,
            durationSeconds = durationSec,
            isRunning = true,
            isAwaitingEntry = false
        )

        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        val restoredState = timerPreferences.timerState.first()
        assertTrue(restoredState.isRunning)
        assertEquals(futureEnd, restoredState.endTimeMillis)
        assertEquals(durationSec, restoredState.durationSeconds)
    }

    @Test
    fun bootReceiver_handlesExpiredTimerWhileDeviceOff() = runBlocking {
        val pastEnd = System.currentTimeMillis() - 5000L
        val durationSec = 300L

        timerPreferences.saveTimerState(
            endTimeMillis = pastEnd,
            durationSeconds = durationSec,
            isRunning = true,
            isAwaitingEntry = false
        )

        val receiver = BootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)

        // Poll for the background Dispatchers.IO update
        val deadline = System.currentTimeMillis() + 3000L
        var state = timerPreferences.timerState.first()
        while (!state.isAwaitingEntry && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            state = timerPreferences.timerState.first()
        }

        assertTrue("Expected isAwaitingEntry to become true within timeout", state.isAwaitingEntry)
    }
}
