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

package com.shourov.apps.pacedream.model.pricing

import com.shourov.apps.pacedream.model.PricingUnit
import com.shourov.apps.pacedream.model.response.home.rooms.Result

/**
 * Android parity for the web helper at
 * `frontend/src/lib/listing/pricingUnit.ts` (`getPricingUnit` /
 * `isMonthlyListing`).
 *
 * The web normalises a raw frequency string from several
 * differently-named fields (`pricingUnit`, `pricing_type`,
 * `pricing_schedule_type`, `pricingScheduleType`,
 * `price[0].frequency`, `pricing.frequency`, ...) and falls back
 * to structural signals (`month_durations`, `min_months`). None
 * of those fields exist on the Android `Result` DTO — the rooms
 * API carries pricing in the typed nested blocks under
 * `dynamic_price[0]` instead. This helper keeps the same intent
 * translated to that shape: a listing is treated as monthly when
 * it carries a usable monthly price and no shorter-term price
 * (the same fallback `BookingFormViewModel` was applying inline
 * before this helper existed).
 *
 * Reuses the shared `PricingUnit` enum in
 * `com.shourov.apps.pacedream.model` so callers route through a
 * single pricing model.
 */
fun Result.getPricingUnit(): PricingUnit {
    val dp = this.dynamic_price?.firstOrNull() ?: return PricingUnit.HOUR
    val monthly = dp.monthly?.price?.takeIf { it > 0 }
    val hourly = dp.hourly?.price?.takeIf { it > 0 }
    val daily = dp.daily?.price?.takeIf { it > 0 }
    val weekly = dp.weekend?.price?.takeIf { it > 0 }

    if (monthly != null && hourly == null && daily == null && weekly == null) {
        return PricingUnit.MONTH
    }
    return when {
        hourly != null -> PricingUnit.HOUR
        daily != null -> PricingUnit.DAY
        weekly != null -> PricingUnit.WEEK
        monthly != null -> PricingUnit.MONTH
        else -> PricingUnit.HOUR
    }
}

/** True when this listing should be treated as a monthly rental — mirrors web `isMonthlyListing`. */
fun Result.isMonthlyListing(): Boolean = getPricingUnit() == PricingUnit.MONTH
