package com.shourov.apps.pacedream.feature.host

import com.shourov.apps.pacedream.feature.host.presentation.ListingCategory
import com.shourov.apps.pacedream.feature.host.presentation.SubcategorySchema
import com.shourov.apps.pacedream.model.PricingUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the safe-fallback contract on
 * [SubcategorySchema.defaultPricingUnit]: a misconfigured schema
 * with an empty [SubcategorySchema.allowedPricingUnits] list must
 * not throw at construction time.
 */
class SubcategorySchemaDefaultsTest {

    @Test
    fun `default pricing unit follows the first allowed unit`() {
        val schema = SubcategorySchema(
            id = "stub",
            category = ListingCategory.SPACE,
            displayLabel = "Stub",
            fields = emptySet(),
            allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK),
        )
        assertEquals(PricingUnit.DAY, schema.defaultPricingUnit)
    }

    @Test
    fun `empty allowed units fall back to HOUR instead of crashing`() {
        val schema = SubcategorySchema(
            id = "stub",
            category = ListingCategory.SERVICE,
            displayLabel = "Stub",
            fields = emptySet(),
            allowedPricingUnits = emptyList(),
        )
        assertEquals(PricingUnit.HOUR, schema.defaultPricingUnit)
    }

    @Test
    fun `explicit defaultPricingUnit override is respected`() {
        val schema = SubcategorySchema(
            id = "stub",
            category = ListingCategory.ITEM,
            displayLabel = "Stub",
            fields = emptySet(),
            allowedPricingUnits = listOf(PricingUnit.DAY, PricingUnit.WEEK),
            defaultPricingUnit = PricingUnit.WEEK,
        )
        assertEquals(PricingUnit.WEEK, schema.defaultPricingUnit)
    }
}
