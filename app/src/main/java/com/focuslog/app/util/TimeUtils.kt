package com.focuslog.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for time calculations, formatting, and IST (Asia/Kolkata) scheduling.
 */
object TimeUtils {

    /** Standard Timezone for FocusLog daily resets: Asia/Kolkata (IST, UTC+5:30) */
    val IST_ZONE_ID: ZoneId = ZoneId.of("Asia/Kolkata")

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    /**
     * Returns the current date string in IST format: "YYYY-MM-DD".
     */
    fun getCurrentDateIst(): String {
        return LocalDate.now(IST_ZONE_ID).format(DATE_FORMATTER)
    }

    /**
     * Converts epoch milliseconds to a readable date string in IST: "YYYY-MM-DD".
     */
    fun formatEpochToDateIst(epochMs: Long): String {
        return Instant.ofEpochMilli(epochMs)
            .atZone(IST_ZONE_ID)
            .toLocalDate()
            .format(DATE_FORMATTER)
    }

    /**
     * Converts epoch milliseconds to a readable time string in IST: "hh:mm a".
     */
    fun formatEpochToTimeIst(epochMs: Long): String {
        return Instant.ofEpochMilli(epochMs)
            .atZone(IST_ZONE_ID)
            .format(TIME_FORMATTER)
    }

    /**
     * Formats duration in milliseconds to "Xm Ys" or "Xm".
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (seconds > 0) {
            String.format(Locale.US, "%dm %02ds", minutes, seconds)
        } else {
            String.format(Locale.US, "%dm", minutes)
        }
    }

    /**
     * Calculates the millisecond delay from now until the next 2:00 AM IST.
     * Used for scheduling the daily export and reset worker.
     */
    fun calculateDelayUntilNext2AmIst(): Long {
        val nowIst = ZonedDateTime.now(IST_ZONE_ID)
        var target2Am = nowIst.withHour(2).withMinute(0).withSecond(0).withNano(0)

        // If 2:00 AM IST has already passed today, schedule for tomorrow's 2:00 AM IST
        if (nowIst.isAfter(target2Am) || nowIst.isEqual(target2Am)) {
            target2Am = target2Am.plusDays(1)
        }

        return Duration.between(nowIst, target2Am).toMillis().coerceAtLeast(1000L)
    }
}
