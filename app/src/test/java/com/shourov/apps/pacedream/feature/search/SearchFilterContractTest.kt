package com.shourov.apps.pacedream.feature.search

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.config.AppConfig
import com.shourov.apps.pacedream.feature.home.presentation.components.FilterCriteria
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Wire-contract tests for the search filter serialization.
 *
 * These tests assert the EXACT query-param shape this client emits for
 * each [FilterCriteria] field — both on the primary
 * (`/v1/poc/listings` or `/v1/listings`) and the fallback (`/v1/search`)
 * endpoints.  Their job is to fail fast if a future refactor silently
 * regresses the wire contract; they do not verify backend behaviour
 * (that lives in a separate repo and a separate audit — see
 * SEARCH_FILTER_CONTRACT.md).
 *
 * Important: a passing test means the Android client SENDS the keys
 * documented below.  Whether the server applies them is out of scope.
 */
class SearchFilterContractTest {

    private lateinit var repo: SearchRepository

    @Before
    fun setup() {
        val json = Json { ignoreUnknownKeys = true }
        val appConfig = AppConfig()
        val apiClient = ApiClient(
            appConfig = appConfig,
            json = json,
            tokenProvider = object : TokenProvider {
                override fun getAccessToken(): String? = null
                override fun getRefreshToken(): String? = null
            }
        )
        repo = SearchRepository(apiClient = apiClient, appConfig = appConfig, json = json)
    }

    // ── Baseline: empty FilterCriteria emits no structured filter keys ──

    @Test
    fun `default FilterCriteria emits no Airbnb-parity filter keys on primary`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
        )
        val params = req.queryParams

        // Baseline keys that always exist
        assertEquals("published", params["status"])
        assertEquals("24", params["limit"])
        assertEquals("time_based", params["category"])
        assertEquals("true", params["skip_pagination"])

        // None of the structured filter keys may be present when no
        // filters are applied — empty/zero/null must be dropped.
        assertNull("guests must not appear when unset", params["guests"])
        assertNull("bedrooms must not appear when unset", params["bedrooms"])
        assertNull("beds must not appear when unset", params["beds"])
        assertNull("bathrooms must not appear when unset", params["bathrooms"])
        assertNull("instantBook must not appear when unset", params["instantBook"])
        assertNull("minPrice must not appear when unset", params["minPrice"])
        assertNull("maxPrice must not appear when unset", params["maxPrice"])
        assertNull("amenities must not appear when unset", params["amenities"])
        assertNull("propertyType must not appear when unset", params["propertyType"])
        assertNull("date must not appear when unset", params["date"])
    }

    @Test
    fun `default FilterCriteria emits no Airbnb-parity filter keys on fallback`() {
        val params = repo.buildFallbackQueryParamsForTest()
        assertNull(params["guests"])
        assertNull(params["bedrooms"])
        assertNull(params["beds"])
        assertNull(params["bathrooms"])
        assertNull(params["instantBook"])
        assertNull(params["minPrice"])
        assertNull(params["maxPrice"])
        assertNull(params["amenities"])
        assertNull(params["propertyType"])
    }

    // ── Each FilterCriteria field maps to the documented wire key ───────

    @Test
    fun `guests is serialised as integer string under key 'guests'`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            guests = 4,
        )
        assertEquals("4", req.queryParams["guests"])
    }

    @Test
    fun `guests of zero or null is dropped`() {
        val zero = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            guests = 0,
        ).queryParams
        assertNull(zero["guests"])

        val nul = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            guests = null,
        ).queryParams
        assertNull(nul["guests"])
    }

    @Test
    fun `bedrooms beds bathrooms each map to their own integer keys`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "room-stays",
            usePOCEndpoint = false,
            bedrooms = 2,
            beds = 3,
            bathrooms = 1,
        )
        assertEquals("2", req.queryParams["bedrooms"])
        assertEquals("3", req.queryParams["beds"])
        assertEquals("1", req.queryParams["bathrooms"])
    }

    @Test
    fun `instantBook true emits key, false or null does not`() {
        val on = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            instantBook = true,
        ).queryParams
        assertEquals("true", on["instantBook"])

        val off = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            instantBook = false,
        ).queryParams
        assertNull(
            "instantBook=false must not be sent — backend would treat it as a constraint",
            off["instantBook"]
        )

        val nul = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            instantBook = null,
        ).queryParams
        assertNull(nul["instantBook"])
    }

    @Test
    fun `minPrice and maxPrice serialise to plain integer strings`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            minPrice = 25,
            maxPrice = 150,
        )
        assertEquals("25", req.queryParams["minPrice"])
        assertEquals("150", req.queryParams["maxPrice"])
    }

    @Test
    fun `minPrice of zero is dropped, maxPrice can be sent independently`() {
        val justMax = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            minPrice = 0,
            maxPrice = 200,
        ).queryParams
        assertNull(justMax["minPrice"])
        assertEquals("200", justMax["maxPrice"])
    }

    @Test
    fun `amenities are joined comma-separated, lowercased, with spaces converted to underscore`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            // Use a LinkedHashSet so iteration order is deterministic
            amenities = linkedSetOf("WiFi", "Pet Friendly", "Pool"),
        )
        assertEquals("wifi,pet_friendly,pool", req.queryParams["amenities"])
    }

    @Test
    fun `empty amenities set drops the key entirely`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            amenities = emptySet(),
        )
        assertNull(req.queryParams["amenities"])
    }

    @Test
    fun `propertyType is sent as lowercase`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "room-stays",
            usePOCEndpoint = false,
            propertyType = "Apartment",
        )
        assertEquals("apartment", req.queryParams["propertyType"])
    }

    @Test
    fun `propertyType blank is dropped`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            propertyType = "   ",
        )
        assertNull(req.queryParams["propertyType"])
    }

    // ── Date handling ────────────────────────────────────────────────────

    @Test
    fun `start and end date are joined with a comma under key 'date'`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            startDate = "2026-06-01",
            endDate = "2026-06-04",
        )
        assertEquals("2026-06-01,2026-06-04", req.queryParams["date"])
    }

    @Test
    fun `start date only emits 'date' without a comma`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            startDate = "2026-06-01",
            endDate = null,
        )
        assertEquals("2026-06-01", req.queryParams["date"])
    }

    @Test
    fun `end date only without a start date does not emit any date key`() {
        // Mirrors the existing primary-builder rule: an end-only range
        // is meaningless and should be dropped.
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            startDate = null,
            endDate = "2026-06-04",
        )
        assertNull(req.queryParams["date"])
    }

    // ── Endpoint selection ──────────────────────────────────────────────

    @Test
    fun `time-based uses poc listings path`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
        )
        assertEquals(listOf("poc", "listings"), req.pathSegments)
        assertEquals("time_based", req.queryParams["category"])
        assertNull("shareType is not sent on the POC endpoint", req.queryParams["shareType"])
    }

    @Test
    fun `non-poc category uses listings path with shareType`() {
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "room-stays",
            usePOCEndpoint = false,
            shareType = "SPLIT",
        )
        assertEquals(listOf("listings"), req.pathSegments)
        assertEquals("SPLIT", req.queryParams["shareType"])
        assertNull("category is not sent on the listings endpoint", req.queryParams["category"])
    }

    // ── Fallback parity: structured filters are forwarded too ───────────

    @Test
    fun `fallback endpoint forwards every Airbnb-parity filter`() {
        val params = repo.buildFallbackQueryParamsForTest(
            guests = 2,
            bedrooms = 1,
            beds = 1,
            bathrooms = 1,
            instantBook = true,
            minPrice = 50,
            maxPrice = 300,
            amenities = linkedSetOf("WiFi", "Kitchen"),
            propertyType = "Studio",
        )
        assertEquals("2", params["guests"])
        assertEquals("1", params["bedrooms"])
        assertEquals("1", params["beds"])
        assertEquals("1", params["bathrooms"])
        assertEquals("true", params["instantBook"])
        assertEquals("50", params["minPrice"])
        assertEquals("300", params["maxPrice"])
        assertEquals("wifi,kitchen", params["amenities"])
        assertEquals("studio", params["propertyType"])
    }

    // ── Bounding box must remain all-or-nothing ─────────────────────────

    @Test
    fun `bounding box is sent only when all four corners present`() {
        val full = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            swLat = 37.7,
            swLng = -122.5,
            neLat = 37.8,
            neLng = -122.4,
        ).queryParams
        assertEquals("37.7", full["swLat"])
        assertEquals("-122.5", full["swLng"])
        assertEquals("37.8", full["neLat"])
        assertEquals("-122.4", full["neLng"])

        val partial = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            swLat = 37.7,
            swLng = null,
            neLat = 37.8,
            neLng = -122.4,
        ).queryParams
        assertNull(partial["swLat"])
        assertNull(partial["swLng"])
        assertNull(partial["neLat"])
        assertNull(partial["neLng"])
    }

    // ── End-to-end: a full FilterCriteria round-trips through both
    //     wire builders without losing any field ───────────────────────

    @Test
    fun `every FilterCriteria field reaches the primary wire request`() {
        val criteria = FilterCriteria(
            checkInEpochDay = null, // dates go through SearchViewModel which already converts
            checkOutEpochDay = null,
            adults = 2,
            children = 1,
            infants = 1,
            pets = 1,
            propertyType = "Apartment",
            minPrice = 30,
            maxPrice = 400,
            bedrooms = 2,
            beds = 2,
            bathrooms = 1,
            amenities = linkedSetOf("WiFi", "Pool"),
            instantBookOnly = true,
        )

        // Mirror what SearchViewModel.loadPage does when handing
        // FilterCriteria values to the repository.
        val req = repo.buildPrimaryWireRequestForTest(
            webCategory = "time-based",
            usePOCEndpoint = true,
            guests = (criteria.adults + criteria.children).takeIf { it > 0 },
            bedrooms = criteria.bedrooms,
            beds = criteria.beds,
            bathrooms = criteria.bathrooms,
            instantBook = criteria.instantBookOnly.takeIf { it },
            minPrice = criteria.minPrice,
            maxPrice = criteria.maxPrice,
            amenities = criteria.amenities,
            propertyType = criteria.propertyType,
        ).queryParams

        assertEquals("3", req["guests"])  // adults + children
        assertEquals("2", req["bedrooms"])
        assertEquals("2", req["beds"])
        assertEquals("1", req["bathrooms"])
        assertEquals("true", req["instantBook"])
        assertEquals("30", req["minPrice"])
        assertEquals("400", req["maxPrice"])
        assertEquals("wifi,pool", req["amenities"])
        assertEquals("apartment", req["propertyType"])

        // infants and pets are intentionally NOT forwarded — they're
        // tracked in FilterCriteria for UI parity with Airbnb but the
        // backend has no documented keys for them.  This assertion
        // pins that decision so a future change can't silently send
        // them under a guessed key.
        assertNull("infants must not be sent under any key", req["infants"])
        assertNull("pets must not be sent under any key", req["pets"])
    }

    @Test
    fun `default FilterCriteria isEmpty stays true when nothing is selected`() {
        // Sanity — pins the SearchUiState.activeFilterCount baseline.
        assertTrue(FilterCriteria().isEmpty)
    }

    @Test
    fun `setting any filter flips isEmpty`() {
        assertFalse(FilterCriteria(adults = 1).isEmpty)
        assertFalse(FilterCriteria(instantBookOnly = true).isEmpty)
        assertFalse(FilterCriteria(amenities = setOf("wifi")).isEmpty)
    }
}
