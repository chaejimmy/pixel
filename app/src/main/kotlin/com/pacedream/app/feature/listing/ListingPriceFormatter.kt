package com.pacedream.app.feature.listing

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared helper for rendering listing price labels on listing cards (Home, Discover,
 * Spaces, Items, Search, Wishlist). Consolidates the per-ViewModel parsePrice
 * duplicates so monthly listings show "$800/month" instead of a bare "$800",
 * and daily/hourly listings keep their existing "$6/day" / "$10/hr" labels.
 *
 * Price-badge rules:
 *   - monthly → "$800/month"
 *   - daily   → "$6/day"
 *   - hourly  → "$10/hr"
 *   - unit missing → "$10" (safe fallback)
 */
object ListingPriceFormatter {

    /**
     * Best-effort parse of a listing JSON payload into a display price. Returns null
     * only when no amount can be found anywhere in the known pricing shapes.
     */
    fun parseListingPrice(obj: JsonObject): String? = runCatching {
        val standaloneUnit = obj.standaloneFrequency()

        // dynamic_price[] — supports BOTH the nested sub-object shape
        // ({ hourly:{price}, daily:{price}, monthly:{price}, weekend:{price} })
        // AND the flat shape ({ price, frequency }). Nested wins when present.
        (obj["dynamic_price"] as? JsonArray)?.firstOrNull()?.jsonObject?.let { dp ->
            dp.resolveNestedDynamicPrice()?.let { return@let it }

            val flatPrice = dp["price"]?.jsonPrimitive?.doubleOrNull
                ?: dp["price"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val flatUnit = dp["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: standaloneUnit
            flatPrice?.let { formatPrice(it, flatUnit) }
        }?.let { return@runCatching it }

        // pricing { hourlyFrom | basePrice, frequencyLabel | frequency | pricing_type | unit }
        (obj["pricing"] as? JsonObject)?.let { p ->
            val hourlyFrom = p["hourlyFrom"]?.jsonPrimitive?.doubleOrNull
                ?: p["hourly_from"]?.jsonPrimitive?.doubleOrNull
            val basePrice = p["basePrice"]?.jsonPrimitive?.doubleOrNull
                ?: p["base_price"]?.jsonPrimitive?.doubleOrNull
            val unit = p["frequencyLabel"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: p["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: p["pricing_type"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: p["unit"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: standaloneUnit
            val amount = hourlyFrom ?: basePrice
            amount?.let { formatPrice(it, unit) }
        }?.let { return@runCatching it }

        // price as array of pricing objects
        (obj["price"] as? JsonArray)?.firstOrNull()?.jsonObject?.let { p ->
            val amount = p["amount"]?.jsonPrimitive?.doubleOrNull
                ?: p["amount"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val unit = p["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: p["unit"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: standaloneUnit
            amount?.let { formatPrice(it, unit) }
        }?.let { return@runCatching it }

        // price as object { amount, unit | frequency }
        (obj["price"] as? JsonObject)?.let { p ->
            val amount = p["amount"]?.jsonPrimitive?.doubleOrNull
                ?: p["amount"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val unit = p["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: p["unit"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                ?: standaloneUnit
            amount?.let { formatPrice(it, unit) }
        }?.let { return@runCatching it }

        // price as primitive (no shape info — fall back to the standalone unit).
        (obj["price"] as? JsonPrimitive)?.let { p ->
            val amount = p.doubleOrNull ?: p.contentOrNull?.toDoubleOrNull()
            amount?.let { formatPrice(it, standaloneUnit) }
        }
    }.getOrNull()

    /**
     * Strip trailing "($800/month)" / "($800/mo)" / "- $800 / month" noise from a
     * listing title so the price lives in the badge only. Leaves the rest untouched.
     */
    fun stripTrailingPriceFromTitle(title: String): String {
        val pattern = Regex(
            """\s*[-—–]?\s*\(?\s*\$?\s*\d+(?:[.,]\d+)?\s*/\s*(?:hr|hour|hourly|day|daily|week|weekly|wk|month|monthly|mo|night|nightly)\s*\)?\s*$""",
            RegexOption.IGNORE_CASE
        )
        return title.replace(pattern, "").trim()
    }

    // ───── helpers ─────────────────────────────────────────────────────────

    /**
     * Nested dynamic_price sub-object shape. Priority mirrors iOS / BookingFormViewModel:
     * monthly wins when it's the only period present, otherwise we prefer the more
     * granular period.
     */
    private fun JsonObject.resolveNestedDynamicPrice(): String? {
        val monthly = this["monthly"]?.priceAmount()
        val daily = this["daily"]?.priceAmount()
        val hourly = this["hourly"]?.priceAmount()
        val weekend = this["weekend"]?.priceAmount()

        return when {
            monthly != null && hourly == null && daily == null ->
                formatPrice(monthly, "month")
            daily != null && hourly == null ->
                formatPrice(daily, "day")
            hourly != null ->
                formatPrice(hourly, "hr")
            weekend != null ->
                formatPrice(weekend, "day")
            monthly != null ->
                formatPrice(monthly, "month")
            else -> null
        }
    }

    private fun JsonElement.priceAmount(): Double? = (this as? JsonObject)?.let {
        val raw = it["price"]?.jsonPrimitive
        (raw?.doubleOrNull ?: raw?.contentOrNull?.toDoubleOrNull())?.takeIf { v -> v > 0 }
    }

    private fun JsonObject.standaloneFrequency(): String? =
        this["pricingUnit"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
            ?: this["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
            ?: (this["pricing"] as? JsonObject)?.let { p ->
                p["pricing_type"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                    ?: p["frequencyLabel"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                    ?: p["frequency"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
                    ?: p["unit"]?.jsonPrimitive?.contentOrNull?.let(::normalizeUnit)
            }

    private fun normalizeUnit(raw: String): String = when (raw.lowercase().trim()) {
        "hourly", "hour", "hr" -> "hr"
        "daily", "day", "night", "nightly" -> "day"
        "weekly", "week", "wk" -> "wk"
        "monthly", "month", "mo" -> "month"
        "once" -> "total"
        else -> raw.lowercase()
    }

    private fun formatPrice(amount: Double, unit: String?): String {
        val formatted = if (amount == amount.toInt().toDouble()) amount.toInt().toString()
        else "%.2f".format(amount).trimEnd('0').trimEnd('.')
        return if (!unit.isNullOrBlank()) "$$formatted/$unit" else "$$formatted"
    }
}
