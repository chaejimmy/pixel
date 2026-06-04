package com.pacedream.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Locks down the single-source-of-truth currency formatting contract.
 *
 * All assertions pin an explicit [Locale.US] so grouping separators and symbol
 * placement are deterministic regardless of the machine running the tests.
 */
class MoneyFormatterTest {

    // ── Checkout parity: USD must render exactly as before ────────────────

    @Test
    fun `formatCents renders fractional USD with two decimals`() {
        assertEquals(
            "$1,234.50",
            MoneyFormatter.formatCents(123450L, "USD", Locale.US),
        )
    }

    @Test
    fun `formatCents drops decimals for whole USD`() {
        assertEquals(
            "$1,234",
            MoneyFormatter.formatCents(123400L, "USD", Locale.US),
        )
    }

    // ── formatAmount across the previously hand-mapped currencies ─────────

    @Test
    fun `formatAmount renders whole USD without decimals`() {
        assertEquals("$1,234", MoneyFormatter.formatAmount(1234.0, "USD", Locale.US))
    }

    @Test
    fun `formatAmount renders fractional USD with decimals`() {
        assertEquals("$1,234.50", MoneyFormatter.formatAmount(1234.5, "USD", Locale.US))
    }

    @Test
    fun `formatAmount renders EUR symbol`() {
        assertEquals("€1,234.50", MoneyFormatter.formatAmount(1234.5, "EUR", Locale.US))
        assertEquals("€1,234", MoneyFormatter.formatAmount(1234.0, "EUR", Locale.US))
    }

    @Test
    fun `formatAmount renders GBP symbol`() {
        assertEquals("£1,234.50", MoneyFormatter.formatAmount(1234.5, "GBP", Locale.US))
        assertEquals("£1,234", MoneyFormatter.formatAmount(1234.0, "GBP", Locale.US))
    }

    @Test
    fun `currency code is case-insensitive`() {
        assertEquals("$1,234", MoneyFormatter.formatAmount(1234.0, "usd", Locale.US))
    }

    // ── The case the old manual symbol map silently got wrong ─────────────

    @Test
    fun `formatAmount renders zero-decimal JPY correctly`() {
        // The old map fell through to "JPY 1234"; the NumberFormat path knows
        // JPY has no minor unit and renders the proper symbol with no decimals.
        assertEquals("¥1,234", MoneyFormatter.formatAmount(1234.0, "JPY", Locale.US))
    }

    @Test
    fun `formatCents renders zero-decimal JPY correctly`() {
        assertEquals("¥1,234", MoneyFormatter.formatCents(123400L, "JPY", Locale.US))
    }

    // ── Graceful fallback for an unknown currency code ────────────────────

    @Test
    fun `unknown currency code falls back without throwing`() {
        assertEquals("ZZZ 1234", MoneyFormatter.formatAmount(1234.0, "ZZZ", Locale.US))
        assertEquals("ZZZ 12.50", MoneyFormatter.formatAmount(12.5, "ZZZ", Locale.US))
    }
}
