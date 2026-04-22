package com.shourov.apps.pacedream.feature.wifi.util

import java.time.Instant
import java.time.format.DateTimeParseException

internal object WifiTime {

    /**
     * Parse a server-emitted ISO-8601 instant. Returns null if the string is
     * unparseable; callers must treat that as "no live session".
     */
    fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * Format a remaining-second count as M:SS or MM:SS. Used in the pill and
     * the bottom sheet so the user always sees an unambiguous value.
     */
    fun formatRemaining(secondsRemaining: Long): String {
        val safe = secondsRemaining.coerceAtLeast(0L)
        val minutes = safe / 60
        val seconds = safe % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
