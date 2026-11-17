package com.focuslog.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for Room LogEntryDao.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LogEntryDaoTest {

    private lateinit var database: FocusLogDatabase
    private lateinit var dao: LogEntryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.logEntryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAll_returnsInsertedEntries() = runTest {
        val entry1 = LogEntry(
            id = 1,
            text = "Studied Kotlin coroutines",
            timerDurationSeconds = 1500L,
            startedAtMillis = 100000L,
            completedAtMillis = 101500L
        )
        val entry2 = LogEntry(
            id = 2,
            text = "Designed Room schema",
            timerDurationSeconds = 1200L,
            startedAtMillis = 200000L,
            completedAtMillis = 201200L
        )

        dao.insert(entry1)
        dao.insert(entry2)

        val allEntries = dao.getAll()
        assertEquals(2, allEntries.size)
        // Ordered by completedAtMillis DESC
        assertEquals("Designed Room schema", allEntries[0].text)
        assertEquals("Studied Kotlin coroutines", allEntries[1].text)
    }

    @Test
    fun observeAllOrderedByNewest_emitsEntriesInDescendingOrder() = runTest {
        val older = LogEntry(
            id = 1,
            text = "First older task",
            timerDurationSeconds = 600L,
            startedAtMillis = 1000L,
            completedAtMillis = 2000L
        )
        val newer = LogEntry(
            id = 2,
            text = "Second newer task",
            timerDurationSeconds = 900L,
            startedAtMillis = 3000L,
            completedAtMillis = 4000L
        )

        dao.insert(older)
        dao.insert(newer)

        val emitted = dao.observeAllOrderedByNewest().first()
        assertEquals(2, emitted.size)
        assertEquals(newer.id, emitted[0].id)
        assertEquals(older.id, emitted[1].id)
    }

    @Test
    fun deleteAll_removesAllEntriesFromDatabase() = runTest {
        val entry = LogEntry(
            text = "Temporary task",
            timerDurationSeconds = 300L,
            startedAtMillis = 1000L,
            completedAtMillis = 1300L
        )
        dao.insert(entry)
        assertTrue(dao.getAll().isNotEmpty())

        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }
}

