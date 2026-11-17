package com.focuslog.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room database instance for FocusLog.
 * Operates purely locally with no network synchronization.
 */
@Database(
    entities = [FocusLogEntry::class],
    version = 1,
    exportSchema = false
)
abstract class FocusLogDatabase : RoomDatabase() {

    abstract fun focusLogDao(): FocusLogDao

    companion object {
        @Volatile
        private var INSTANCE: FocusLogDatabase? = null

        fun getDatabase(context: Context): FocusLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusLogDatabase::class.java,
                    "focus_log_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
