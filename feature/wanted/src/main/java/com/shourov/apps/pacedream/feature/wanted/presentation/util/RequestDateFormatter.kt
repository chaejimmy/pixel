package com.shourov.apps.pacedream.feature.wanted.presentation.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats a request's start/end date pair for display.
 *
 * Newly created requests submit ISO-8601 (`yyyy-MM-dd`) dates from the
 * Material 3 date-range picker. Legacy requests have free-text strings like
 * "Sat 10:00 AM" — those we surface verbatim so old listings still render.
 */
object RequestDateFormatter {

    fun format(
        start: String?,
        end: String? = null,
        locale: Locale = Locale.getDefault(),
    ): String? {
        val startTrimmed = start?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startDate = startTrimmed.parseIsoDateOrNull()
            ?: return startTrimmed
        val formatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
        val startLabel = formatter.format(startDate)
        val endDate = end?.trim()?.takeIf { it.isNotEmpty() }?.parseIsoDateOrNull()
        return if (endDate != null && endDate != startDate) {
            "$startLabel — ${formatter.format(endDate)}"
        } else {
            startLabel
        }
    }

    private fun String.parseIsoDateOrNull(): LocalDate? = try {
        LocalDate.parse(this)
    } catch (_: Exception) {
        null
    }
}
