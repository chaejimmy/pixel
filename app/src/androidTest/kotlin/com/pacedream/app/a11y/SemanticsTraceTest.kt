package com.pacedream.app.a11y

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.printToString
import com.pacedream.app.feature.listingdetail.ListingDetailModel
import com.pacedream.app.feature.listingdetail.ListingDetailScreen
import com.pacedream.app.feature.listingdetail.ListingDetailUiState
import com.pacedream.app.feature.listingdetail.ListingHost
import com.pacedream.app.feature.listingdetail.ListingLocation
import com.pacedream.app.feature.listingdetail.ListingPricing
import com.pacedream.common.composables.theme.PaceDreamTheme
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented "TalkBack trace" test.
 *
 * Renders each of the four screens called out in
 * `A11Y_AUDIT_HOME_SEARCH_LISTING_BOOKINGS.md` (Home, Search, Listing
 * Detail, Bookings), then dumps the full Compose semantics tree to
 * logcat via `onRoot().printToLog("SEMANTICS")`. Reviewers grep the
 * output for each flagged label from the audit document to verify
 * whether a proposed fix actually changes the announced semantics on
 * a real device.
 *
 * ## What runs today vs. what's stubbed
 *
 * `ListingDetailScreen` is **directly testable** because it takes
 * its `ListingDetailUiState` as a parameter — no Hilt injection
 * required. The `listingDetail_dumpSemanticsTree` test below is
 * runnable end-to-end on any Pixel 6 / API 34 emulator with no
 * additional setup. It also asserts a small set of negative
 * invariants (no raw wire-format type codes, dynamic favourite
 * label present).
 *
 * The other three screens use `hiltViewModel()` defaults that
 * require a fully-wired Hilt graph + fakes. They are scaffolded
 * here as `@Ignore`-d stubs so a future PR can add the Hilt test
 * harness (`@HiltAndroidTest`, a `HiltTestActivity`, fake repos)
 * without redesigning the test class itself. The TODO blocks
 * inside each stub describe exactly what needs to land first.
 *
 * ## Running
 *
 * ```
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=\
 * com.pacedream.app.a11y.SemanticsTraceTest
 * adb logcat -s SEMANTICS:V > semantics-trace.log
 * ```
 *
 * Each test prints a banner of the form `===== <screen-name> =====`
 * before its dump, so the resulting log can be split per screen
 * with a simple awk.
 */
class SemanticsTraceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────
    // ListingDetail — runnable today
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun listingDetail_dumpSemanticsTree() {
        val state = ListingDetailUiState(
            isLoading = false,
            listing = fakeListingDetail(),
            isFavorite = false,
            currentUserId = "test-user-1",
        )

        composeTestRule.setContent {
            PaceDreamTheme {
                ListingDetailScreen(
                    uiState = state,
                    onBackClick = {},
                    onRetry = {},
                    onToggleFavorite = {},
                    onShare = {},
                    onContactHost = {},
                    onOpenInMaps = {},
                    onConfirmReserve = { _ -> },
                )
            }
        }

        // ── Dump the full semantics tree to logcat ──
        println("===== listingDetail =====")
        composeTestRule.onRoot().printToLog("SEMANTICS")

        // ── Negative invariants from A11Y_AUDIT_HOME_SEARCH_LISTING_BOOKINGS.md ──
        val tree = composeTestRule.onRoot().printToString(maxDepth = Int.MAX_VALUE)

        // L-2: dynamic favourite label present (Phase 4 fix).
        assertTrue(
            "favorite IconButton must announce the stateful label, got tree without 'Add to favorites'",
            tree.contains("Add to favorites") || tree.contains("Remove from favorites")
        )

        // L-2: back / share labelled.
        assertTrue("back button missing 'Back' label", tree.contains("Back"))
        assertTrue("share button missing 'Share' label", tree.contains("Share"))

        // H-7-style guard: no raw wire-format strings should leak as
        // contentDescription. Add new wire types here if they ever
        // accidentally surface in the tree.
        listOf("time_based", "hourly_rental_gear", "split_stay").forEach { wireType ->
            assertTrue(
                "raw wire-format type '$wireType' should never appear in announced semantics; tree contained it",
                !tree.contains(wireType)
            )
        }

        // L-6 anchor (not yet fixed — this assertion documents the
        // current behaviour; flip the `!` once the price-bearing
        // Reserve announcement lands).
        val priceLabel = state.listing?.pricing?.displayPrimary
        if (priceLabel != null) {
            // Currently the Reserve button only announces "Reserve" —
            // not "$priceLabel". This assertion intentionally documents
            // the absence so the audit's L-6 fix can flip it positive.
            val priceAnnounced = tree.contains("Reserve · $priceLabel")
            // Note: not asserting !priceAnnounced; we just print a
            // diagnostic line. Once the audit fix is in, change this
            // to assertTrue(priceAnnounced, ...).
            android.util.Log.i(
                "SEMANTICS",
                "[L-6 follow-up] Reserve announcement includes price? $priceAnnounced"
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Hilt-bound screens — stubs awaiting test-harness setup
    // ─────────────────────────────────────────────────────────────────

    @Test
    @Ignore("TODO: enable once a Hilt test activity + fake repos land. See class kdoc.")
    fun homeScreen_dumpSemanticsTree_pendingHiltSetup() {
        // Steps required to lift the @Ignore:
        //
        //  1. Re-enable the `:ui-test-hilt-manifest` Gradle module by
        //     uncommenting it in settings.gradle.kts:38 and depending
        //     on it from `:app` under `androidTestImplementation`.
        //  2. Add `androidTestImplementation(libs.hilt.android.testing)`
        //     and the Hilt test compiler under `kspAndroidTest` in
        //     `app/build.gradle.kts`.
        //  3. Annotate this class with `@HiltAndroidTest` and inject
        //     a `HiltAndroidRule(this)` before `composeTestRule`.
        //  4. Provide fake `HomeFeedRepository` / `WishlistRepository`
        //     bindings via a `@TestInstallIn(replaces = …)` module.
        //  5. Replace the `composeTestRule.setContent { HomeScreen(...) }`
        //     call so the screen receives a `HomeViewModel` built from
        //     the fake graph — either via `hiltViewModel()` resolving
        //     in a `HiltTestActivity`, or by passing an explicit VM.
        //  6. Use the same `printToLog("SEMANTICS")` + `printToString`
        //     pattern as `listingDetail_dumpSemanticsTree`.
        //  7. Add assertions for the H-1 … H-7 fixes from
        //     A11Y_AUDIT_HOME_SEARCH_LISTING_BOOKINGS.md.
        error("not implemented — see class kdoc and step list above")
    }

    @Test
    @Ignore("TODO: enable once a Hilt test activity + fake repos land. See class kdoc.")
    fun searchScreen_dumpSemanticsTree_pendingHiltSetup() {
        // Same setup as homeScreen_dumpSemanticsTree_pendingHiltSetup.
        // Additional assertions to add once running:
        //  - S-1: "Clear search query" appears when the query field is
        //    non-empty (set `uiState.query = "loft"` via fake VM).
        //  - S-2: each tab Surface has `role = Tab` + a `selected`
        //    semantics property matching the active tab.
        error("not implemented")
    }

    @Test
    @Ignore("TODO: enable once a Hilt test activity + fake repos land. See class kdoc.")
    fun bookingsScreen_dumpSemanticsTree_pendingHiltSetup() {
        // Same setup. Additional assertions to add once running:
        //  - B-1: tab pills carry `role = Tab` and the active tab has
        //    `selected = true`.
        //  - B-2: card AsyncImage has `contentDescription = null` once
        //    the H-5 / B-2 fix is applied (currently announces the
        //    title which double-reads with the caption below).
        //  - B-3: status pill includes "Booking status" prefix or a
        //    `stateDescription`.
        error("not implemented")
    }

    // ─────────────────────────────────────────────────────────────────
    // Fakes
    // ─────────────────────────────────────────────────────────────────

    private fun fakeListingDetail(): ListingDetailModel = ListingDetailModel(
        id = "lst_fake_a11y_test",
        title = "Sunlit Co-Working Loft",
        description = "A bright, quiet workspace in the centre of town. Test fixture for the a11y semantics trace — not a real listing.",
        imageUrls = listOf(
            "https://example.invalid/photo1.jpg",
            "https://example.invalid/photo2.jpg",
            "https://example.invalid/photo3.jpg",
        ),
        location = ListingLocation(
            address = "100 Test Street",
            city = "San Francisco",
            country = "USA",
            latitude = 37.7749,
            longitude = -122.4194,
        ),
        pricing = ListingPricing(
            currency = "USD",
            basePrice = 85.0,
            hourlyFrom = 12.0,
            frequencyLabel = "hour",
        ),
        host = ListingHost(
            id = "host_fake_1",
            name = "Alex Test",
            avatarUrl = null,
            isSuperhost = true,
            isVerified = true,
            joinedDate = "2022",
            responseTime = "Within 2 hours",
        ),
        amenities = listOf("WiFi", "Kitchen", "Parking"),
        rating = 4.85,
        reviewCount = 142,
        isFavorite = false,
        category = "Workspace",
        propertyType = "Co-working space",
        maxGuests = 4,
        available = true,
        instantBook = true,
    )
}
