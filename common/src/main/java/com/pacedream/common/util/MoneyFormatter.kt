package com.pacedream.common.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Single source of truth for rendering monetary values across the app.
 *
 * Replaces the previously duplicated, inconsistent formatters (CheckoutScreen's
 * `formatCents` and the wanted feature's `formatBudget` manual symbol map).
 *
 * ## Rounding & fraction-digit policy
 * Formatting delegates to [NumberFormat.getCurrencyInstance], so values use the
 * platform's default currency rounding mode (`HALF_EVEN`) and locale-aware
 * grouping/symbol placement.
 *
 * The one deliberate deviation from the platform default: **whole values drop
 * their fraction digits** (e.g. `$1,234` rather than `$1,234.00`), matching the
 * pre-existing `formatBudget` behavior. Fractional values keep the currency's
 * default fraction digits (e.g. `$1,234.50`). Because the fraction-digit count
 * comes from the [Currency] itself, zero-decimal currencies such as JPY render
 * correctly (`¥1,234`) — something the old hand-rolled symbol map got wrong.
 *
 * If the supplied currency code is not a valid ISO 4217 code, formatting falls
 * back to a plain `"<CODE> <amount>"` rendering rather than throwing.
 */
object MoneyFormatter {

    /**
     * Formats a minor-unit amount (e.g. cents) as currency.
     *
     * @param cents amount in the currency's minor units (1/100 of the major unit).
     * @param currencyCode ISO 4217 currency code, case-insensitive (e.g. "usd").
     * @param locale locale used for grouping separators and symbol placement.
     */
    fun formatCents(
        cents: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String = formatAmount(cents / 100.0, currencyCode, locale)

    /**
     * Formats a major-unit amount (e.g. dollars) as currency.
     *
     * @param amount amount in the currency's major units.
     * @param currencyCode ISO 4217 currency code, case-insensitive (e.g. "usd").
     * @param locale locale used for grouping separators and symbol placement.
     */
    fun formatAmount(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val isWhole = amount % 1.0 == 0.0
        return try {
            NumberFormat.getCurrencyInstance(locale).apply {
                currency = Currency.getInstance(currencyCode.uppercase())
                // Drop fraction digits for whole values to match today's
                // RequestCard behavior; keep the currency default otherwise.
                if (isWhole) {
                    minimumFractionDigits = 0
                    maximumFractionDigits = 0
                }
            }.format(amount)
        } catch (_: IllegalArgumentException) {
            val code = currencyCode.uppercase()
            val rendered = if (isWhole) amount.toLong().toString() else "%.2f".format(amount)
            "$code $rendered"
        }
    }
}
