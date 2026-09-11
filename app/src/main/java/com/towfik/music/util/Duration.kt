package com.towfik.music.util

/** Pure formatting helpers (unit-testable, no Android dependencies). */
object Duration {

    /** 754000ms -> "12:34"; 3754000ms -> "1:02:34". */
    fun format(millis: Long): String {
        val safe = millis.coerceAtLeast(0)
        val totalSeconds = safe / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
