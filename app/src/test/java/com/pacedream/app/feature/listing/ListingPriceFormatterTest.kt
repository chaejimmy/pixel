package com.pacedream.app.feature.listing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the price-badge rules called out in the monthly-listings fix:
 *   monthly → "$800/month"
 *   daily   → "$6/day"
 *   hourly  → "$10/hr"
 *   missing → "$10" (safe fallback)
 *
 * Also verifies that a trailing "($800/month)" gets stripped from the title so the
 * price only lives in the badge.
 */
class ListingPriceFormatterTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(src: String): String? =
        ListingPriceFormatter.parseListingPrice(json.parseToJsonElement(src) as JsonObject)

    @Test
    fun `nested dynamic_price with monthly only renders month badge`() {
        val price = parse(
            """
            {
              "dynamic_price": [{
                "currency": "USD",
                "hourly": null,
                "daily": null,
                "monthly": { "price": 800 }
              }]
            }
            """.trimIndent()
        )
        assertEquals("$800/month", price)
    }

    @Test
    fun `nested dynamic_price with hourly and monthly prefers hourly`() {
        val price = parse(
            """
            {
              "dynamic_price": [{
                "hourly": { "price": 10 },
                "daily":  { "price": 120 },
                "monthly": { "price": 800 }
              }]
            }
            """.trimIndent()
        )
        assertEquals("$10/hr", price)
    }

    @Test
    fun `nested dynamic_price with daily only renders day badge`() {
        val price = parse(
            """
            {
              "dynamic_price": [{
                "hourly": null,
                "daily": { "price": 6 },
                "monthly": null
              }]
            }
            """.trimIndent()
        )
        assertEquals("$6/day", price)
    }

    @Test
    fun `pricing object with monthly frequency renders month badge`() {
        val price = parse(
            """
            { "pricing": { "basePrice": 800, "frequency": "monthly" } }
            """.trimIndent()
        )
        assertEquals("$800/month", price)
    }

    @Test
    fun `pricing object with hourlyFrom + pricingUnit renders hr badge`() {
        val price = parse(
            """
            { "pricingUnit": "hourly", "pricing": { "hourlyFrom": 10 } }
            """.trimIndent()
        )
        assertEquals("$10/hr", price)
    }

    @Test
    fun `pricing with frequencyLabel 'mo' renders month badge`() {
        val price = parse(
            """
            { "pricing": { "basePrice": 950, "frequencyLabel": "mo" } }
            """.trimIndent()
        )
        assertEquals("$950/month", price)
    }

    @Test
    fun `pricing with plain 'price' alias and no unit returns bare dollar amount`() {
        // The RentableItem list endpoint sends `pricing.price` without a
        // frequency — make sure we still surface the amount instead of null.
        val price = parse("""{ "pricing": { "price": "150" } }""")
        assertEquals("$150", price)
    }

    @Test
    fun `price primitive with no unit still returns dollar amount`() {
        val price = parse("""{ "price": 10 }""")
        assertEquals("$10", price)
    }

    @Test
    fun `price primitive with top-level pricingUnit uses that unit`() {
        val price = parse("""{ "price": 800, "pricingUnit": "month" }""")
        assertEquals("$800/month", price)
    }

    @Test
    fun `missing price data returns null without throwing`() {
        val price = parse("""{ "name": "Just a title", "location": "NYC" }""")
        assertNull(price)
    }

    @Test
    fun `stripTrailingPriceFromTitle removes parenthesised monthly suffix`() {
        assertEquals(
            "Sunlit loft desk",
            ListingPriceFormatter.stripTrailingPriceFromTitle("Sunlit loft desk ($800/month)")
        )
    }

    @Test
    fun `stripTrailingPriceFromTitle removes hourly suffix`() {
        assertEquals(
            "Quiet nap capsule",
            ListingPriceFormatter.stripTrailingPriceFromTitle("Quiet nap capsule (\$10/hr)")
        )
    }

    @Test
    fun `stripTrailingPriceFromTitle keeps titles that don't end in a price`() {
        assertEquals(
            "Loft in the Mission",
            ListingPriceFormatter.stripTrailingPriceFromTitle("Loft in the Mission")
        )
    }

    @Test
    fun `stripTrailingPriceFromTitle leaves internal mentions alone`() {
        // Only the trailing token is stripped — an "800/month" in the middle of a title stays put.
        val input = "Rooftop ($800/month) with a view"
        assertEquals(input, ListingPriceFormatter.stripTrailingPriceFromTitle(input))
    }
}
