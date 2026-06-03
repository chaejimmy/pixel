package com.pacedream.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Locks down the single currency-formatting contract that replaced the two
 * divergent formatters (Checkout's [java.text.NumberFormat] path and Wanted's
 * hand-rolled symbol map).
 *
 * All assertions pin [Locale.US] so the grouping/symbol output is stable across
 * JDK/ICU versions; only the JPY symbol (which varies between "¥" and "JP¥"
 * across runtimes) is asserted structurally rather than byte-for-byte.
 */
class MoneyFormatterTest {

    // --- Checkout parity (must stay byte-for-byte identical for USD) ---------

    @Test
    fun `formatCents USD fractional matches legacy checkout output`() {
        assertEquals("$1,234.50", MoneyFormatter.formatCents(123450L, "USD", Locale.US))
    }

    @Test
    fun `formatCents USD whole drops the decimals`() {
        assertEquals("$1,234", MoneyFormatter.formatCents(123400L, "USD", Locale.US))
    }

    @Test
    fun `formatAmount USD fractional and whole`() {
        assertEquals("$1,234.50", MoneyFormatter.formatAmount(1234.50, "USD", Locale.US))
        assertEquals("$1,234", MoneyFormatter.formatAmount(1234.0, "USD", Locale.US))
    }

    // --- Currencies the old manual map knew about ---------------------------

    @Test
    fun `formatAmount EUR`() {
        assertEquals("€1,234.50", MoneyFormatter.formatAmount(1234.50, "EUR", Locale.US))
        assertEquals("€1,234", MoneyFormatter.formatAmount(1234.0, "EUR", Locale.US))
    }

    @Test
    fun `formatAmount GBP`() {
        assertEquals("£1,234.50", MoneyFormatter.formatAmount(1234.50, "GBP", Locale.US))
        assertEquals("£1,234", MoneyFormatter.formatAmount(1234.0, "GBP", Locale.US))
    }

    @Test
    fun `currency code is case insensitive`() {
        assertEquals(
            MoneyFormatter.formatAmount(10.0, "USD", Locale.US),
            MoneyFormatter.formatAmount(10.0, "usd", Locale.US),
        )
    }

    // --- The case the old symbol map silently got wrong ---------------------

    @Test
    fun `formatAmount JPY has no fraction digits`() {
        // JPY is a zero-decimal currency. The old `when (currency)` map had no
        // JPY entry, fell back to "JPY " + the number, and would have shown
        // bogus decimals for fractional yen. The NumberFormat path knows JPY
        // carries no minor unit, so it renders whole yen with no decimal point.
        val fractional = MoneyFormatter.formatAmount(1234.5, "JPY", Locale.US)
        assertFalse("JPY output must not contain a decimal point: $fractional", fractional.contains("."))
        assertTrue("JPY output should group thousands: $fractional", fractional.contains("1,235"))

        val whole = MoneyFormatter.formatAmount(1234.0, "JPY", Locale.US)
        assertFalse("JPY output must not contain a decimal point: $whole", whole.contains("."))
        assertTrue("JPY output should group thousands: $whole", whole.contains("1,234"))

        // And it is no longer the broken "JPY 1234" fallback the old map produced.
        assertFalse(whole.startsWith("JPY"))
    }

    // --- Invalid / unknown ISO codes fall back gracefully -------------------

    @Test
    fun `unknown currency code falls back to plain formatting`() {
        // Not a valid ISO 4217 code: Currency.getInstance throws, we fall back.
        assertEquals("$12.50", MoneyFormatter.formatAmount(12.5, "NOTACODE", Locale.US))
    }
}
