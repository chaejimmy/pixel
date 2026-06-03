/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacedream.common.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Single source of truth for rendering monetary values across the app.
 *
 * Previously every feature rolled its own formatter:
 *  - Checkout used [NumberFormat.getCurrencyInstance] with no rounding tweaks.
 *  - Wanted/RequestCard used a hand-maintained `when (currency)` symbol map that
 *    only knew about USD/EUR/GBP and silently fell back to `"$code "` for anything
 *    else (so JPY, etc. rendered wrong).
 *
 * This object replaces both. It relies on [NumberFormat.getCurrencyInstance] +
 * [Currency.getInstance] so every ISO 4217 code is handled correctly — including
 * codes the old manual map never covered, and zero-fraction currencies such as JPY.
 *
 * ## Rounding policy
 * The platform [NumberFormat] uses [java.math.RoundingMode.HALF_EVEN] (banker's
 * rounding) for the fractional digits it keeps. We do not override that.
 *
 * ## Whole-number display
 * To preserve the established RequestCard behaviour, when the value is a whole
 * number the fraction digits are dropped entirely (e.g. `$1,234` rather than
 * `$1,234.00`). Fractional values keep the currency's default fraction digits
 * (e.g. `$1,234.50`, `¥1234` since JPY has zero fraction digits).
 */
object MoneyFormatter {

    /**
     * Formats an amount given in the smallest currency unit (e.g. US cents).
     *
     * Note: this assumes a 1/100 minor unit. Values are divided by 100 before
     * formatting, matching the existing Checkout behaviour.
     */
    fun formatCents(
        cents: Long,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String = formatAmount(cents / 100.0, currencyCode, locale)

    /**
     * Formats a major-unit amount (e.g. dollars) in the given currency.
     *
     * Falls back to a plain `"$%.2f"` rendering if [currencyCode] is not a valid
     * ISO 4217 code, mirroring the old Checkout fallback.
     */
    fun formatAmount(
        amount: Double,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String = try {
        val isWhole = amount % 1.0 == 0.0
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(currencyCode.uppercase())
            if (isWhole) {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
        }.format(amount)
    } catch (_: Exception) {
        String.format("$%.2f", amount)
    }
}
