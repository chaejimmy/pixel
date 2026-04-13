# Backend Data Integrity Audit

**Date:** 2026-04-13
**Scope:** Android codebase — verify all UI-facing data originates from real backend APIs, not hardcoded/mock/local sources.
**Methodology:** Traced every data path: UI -> ViewModel -> Repository -> API. Flagged any break in the chain.

---

## Executive Summary

The app **does use real backend APIs** for all primary data flows (listings, bookings, payments, messaging, auth). However, the audit uncovered **17 findings** across 3 severity levels where static data, silent fallbacks, or local overrides can cause the UI to diverge from the backend's source of truth.

| Severity | Count | Description |
|----------|-------|-------------|
| **HIGH** | 4 | Hardcoded data displayed to users with no API fetch; API fields overridden locally |
| **MEDIUM** | 8 | Silent fallbacks mask API failures; optimistic caches may desync; stale data persists |
| **LOW** | 5 | Acceptable static content, client-side classification, timezone edge cases |

---

## HIGH Severity Findings

### H1. Hero Image Hardcoded — Never Fetched from API

**File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt:66-70`

```kotlin
_uiState.update {
    it.copy(
        heroImageUrl = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80"
    )
}
```

**Problem:** The home screen hero image is a hardcoded Unsplash URL set in `init {}`. No API endpoint is called to fetch it. The comment says "can be fetched from API/config later" — but it never is.
**Impact:** Hero banner is static across all users and cannot be updated from the backend. A/B testing, seasonal campaigns, or personalized hero images are impossible.

---

### H2. Booking Status Overridden by Local Date Logic

**File:** `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingsViewModel.kt:234-293`

```kotlin
private fun resolveStatusConfig(item: BookingListItem): BookingStatusConfig {
    val endDate = parseIsoDate(item.checkOutDate)
    if (endDate != null && endDate.before(now)) {
        if (status in upcomingStatuses) {
            return BookingStatusConfig("Completed", BookingFilterCategory.PAST, "green")
        }
    }
    // ... further local overrides for pending, confirmed, cancelled ...
}
```

**Problem:** The API returns a `status` field, but the ViewModel ignores it in favor of local date-based computation. If the checkout date has passed and the API status is "confirmed", the app forcibly displays "Completed" regardless of the actual backend state (e.g., the booking could be disputed, refunded, or extended).
**Impact:** Users see incorrect booking statuses. Backend status transitions (disputes, extensions, manual overrides by support) are invisible on Android.

---

### H3. Hardcoded Cancellation Policy Fallback

**File:** `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingDetailViewModel.kt:162`

```kotlin
cancellationPolicy = "Free cancellation up to 24 hours before check-in. After that, the first night is non-refundable.",
```

**Problem:** When building a `BookingDetail` from cached navigation data (or when the API response omits the field), a hardcoded generic cancellation policy string is displayed. This policy may not match the actual listing's terms.
**Impact:** Users could rely on incorrect cancellation terms, creating legal/financial risk. Different listings may have strict, moderate, or flexible policies that are all masked by this single hardcoded string.

---

### H4. Destination City Images — 20 Hardcoded Unsplash URLs

**File:** `app/src/main/kotlin/com/pacedream/app/feature/destination/DestinationScreen.kt:196-219`

```kotlin
private fun fallbackCityImage(name: String): String {
    return when (name.lowercase().trim()) {
        "new york", "manhattan", "brooklyn" -> "https://images.unsplash.com/photo-..."
        "los angeles" -> "https://images.unsplash.com/photo-..."
        // ... 18 more cities ...
        else -> "https://images.unsplash.com/photo-..."
    }
}
```

**Problem:** 20 city-specific fallback images are hardcoded as Unsplash URLs. Used when the API `image` field is blank/null. The `else` branch ensures even unknown cities get a hardcoded image.
**Impact:** Depends on an external service (Unsplash) that could rate-limit, change URLs, or go down. New cities added on the backend will always show the generic fallback. Backend team cannot control destination imagery on Android.

---

## MEDIUM Severity Findings

### M1. Listing Detail Seeded with Stale Navigation Data

**File:** `app/src/main/kotlin/com/pacedream/app/feature/listingdetail/ListingDetailViewModel.kt:66-86`

```kotlin
fun load(listingId: String, listingType: String = "", initialListing: ListingCardModel?) {
    if (initialListing != null) {
        _uiState.update {
            it.copy(listing = ListingDetailModel(
                id = initialListing.id,
                title = initialListing.title,
                imageUrls = listOfNotNull(initialListing.imageUrl),
                rating = initialListing.rating
            ), isFavorite = false)
        }
    }
    refresh()
}
```

**Problem:** The detail screen is immediately populated with partial data from the navigation source (list card). If `refresh()` fails silently, the user sees outdated title, single image, and stale rating with no error indicator.
**Impact:** Price changes, title updates, or new photos on the backend may not appear if the network call fails. The `isFavorite` is also hardcoded to `false` until the API responds.

---

### M2. Wishlist Fetch Silently Falls Back to Empty

**File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt:124-150`

**Problem:** `loadFavorites()` tries a primary endpoint (`/wishlists`), then a legacy fallback (`/account/wishlist`). If both fail, the error is only logged via Timber — no UI error is shown. The favorites set stays empty.
**Impact:** A user's entire wishlist can silently disappear from the UI due to a transient network error. No retry mechanism or error banner exists.

---

### M3. Host Bookings Silently Omitted on API Failure

**File:** `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingsViewModel.kt:324-335`

```kotlin
is ApiResult.Failure -> {
    // Host bookings failure is non-fatal
    BookingsResult.Success(emptyList())
}
```

**Problem:** If the `/bookings/host` endpoint fails, the error is swallowed and an empty list is returned as a "success". The user's host bookings simply don't appear.
**Impact:** Hosts could miss incoming bookings entirely with no indication that data failed to load.

---

### M4. Optimistic Favorite Toggle with Offline Queue May Desync

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/homefeed/HomeFeedViewModel.kt:155-188`

**Problem:** When a user toggles a favorite, the change is applied to local cache immediately (optimistic update). On network failure, the toggle is queued for retry. If the app is force-closed before sync completes, the local state diverges from the server.
**Impact:** Hearts shown as favorited locally may not be saved on the server. The discrepancy resolves on next successful sync, but can confuse users in the interim.

---

### M5. Booking Detail Uses Cached SavedStateHandle Data Indefinitely

**File:** `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingDetailViewModel.kt:135-172`

**Problem:** The entire `BookingDetail` can be constructed from `SavedStateHandle` cached values without any API call. If the API fetch subsequently fails, this stale cached data persists with no staleness indicator.
**Impact:** A booking that was cancelled or modified on the backend could still show its previous state on the detail screen.

---

### M6. Checkout Crash Recovery Adopts Webhook-Created Bookings

**File:** `app/src/main/kotlin/com/pacedream/app/feature/checkout/CheckoutViewModel.kt:472-509`

**Problem:** After exhausting confirm retries, the ViewModel sends a failure report. If the backend's webhook already created the booking, the client adopts that booking ID and navigates to confirmation — even though the explicit confirm call never succeeded from the client's perspective.
**Impact:** Edge case only (crash during payment confirmation), but the adopted booking may have different metadata than what the client intended. Low probability, but high consequence if it occurs.

---

### M7. FAQ Content Entirely Hardcoded

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/help/FaqScreen.kt:60-97`

**Problem:** 6 FAQ items (questions + answers) are hardcoded as a static `listOf(...)`. No API endpoint is called.
**Impact:** FAQ content cannot be updated without an app release. Support team cannot add/modify FAQs dynamically.

---

### M8. Listing Subcategories and Pricing Rules Hardcoded

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/CreateListingScreen.kt:162-245`

**Problem:** 27 subcategories (Spaces: 9, Items: 9, Services: 8) and their allowed pricing units are hardcoded. Not fetched from an API.
**Impact:** Adding a new subcategory or changing pricing rules requires an app update. Backend and iOS could drift out of sync with Android.

---

## LOW Severity Findings

### L1. Client-Side Service/Space Classification

**File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt:209-227`

**Problem:** Listings from `shareType=USE` are split into "Spaces" vs "Services" client-side using hardcoded category IDs. If the backend adds a new service category, the app won't recognize it until a code update.

---

### L2. Booking Status Date Comparison — Timezone Risk

**File:** `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingsViewModel.kt:240`

**Problem:** ISO 8601 checkout dates are compared with `Date()` (device local time). Incorrect device timezone or daylight saving mismatches could cause bookings to be prematurely marked as "Completed".

---

### L3. Profile Counts Default to Zero on Failure

**File:** `app/src/main/kotlin/com/pacedream/app/feature/profile/ProfileViewModel.kt:111,132`

**Problem:** `fetchWishlistCount()` and `fetchBookingsCount()` both default to `0` on API failure. No error is shown.

---

### L4. Phone Number Placeholder in Verification Screen

**File:** `app/src/main/kotlin/com/pacedream/app/feature/verification/PhoneVerificationScreen.kt`

**Problem:** Input field uses `placeholder = { Text("+1234567890") }`. This is cosmetic only (placeholder text, not a default value) and does not affect data flow.

---

### L5. Settings Preferences — TODO for Backend Options

**File:** `app/src/main/kotlin/com/pacedream/app/feature/settings/preferences/SettingsPreferencesScreen.kt`

**Problem:** Contains a TODO comment: "Fetch available options from the backend if a /preferences/options endpoint is added." Currently uses locally defined options.

---

## Credentials & Configuration Notes

**File:** `secrets.defaults.properties`

The file contains development/test credentials (Auth0 domain, OneSignal App ID, Cloudinary preset, Google Maps API key) used as build fallbacks. The file header documents this is intentional — real credentials go in `secrets.properties` (gitignored). This is a standard Android secrets pattern and not a data integrity issue, but the following should be verified:

- Auth0 `dev-` domain should not be reachable in production builds
- Google Maps API key should be restricted by package name in Google Cloud Console
- Stripe publishable key is intentionally blank in defaults (payment features degrade gracefully)

---

## Summary Table

| ID | File | Component | Issue | Severity |
|----|------|-----------|-------|----------|
| H1 | `HomeViewModel.kt:66` | Home hero banner | Hardcoded Unsplash URL, never fetched from API | **HIGH** |
| H2 | `BookingsViewModel.kt:234` | Booking list | API `status` overridden by local date logic | **HIGH** |
| H3 | `BookingDetailViewModel.kt:162` | Booking detail | Hardcoded fallback cancellation policy | **HIGH** |
| H4 | `DestinationScreen.kt:196` | Destinations | 20 hardcoded city image URLs | **HIGH** |
| M1 | `ListingDetailViewModel.kt:66` | Listing detail | Stale navigation data persists on API failure | MEDIUM |
| M2 | `HomeViewModel.kt:124` | Wishlist | Silent fallback to empty set on double failure | MEDIUM |
| M3 | `BookingsViewModel.kt:324` | Host bookings | API failure silently returns empty list | MEDIUM |
| M4 | `HomeFeedViewModel.kt:155` | Favorites | Optimistic cache may desync with server | MEDIUM |
| M5 | `BookingDetailViewModel.kt:135` | Booking detail | Cached data persists indefinitely on API failure | MEDIUM |
| M6 | `CheckoutViewModel.kt:472` | Checkout | Adopts webhook-created booking after confirm failure | MEDIUM |
| M7 | `FaqScreen.kt:60` | FAQ | 6 FAQ items fully hardcoded | MEDIUM |
| M8 | `CreateListingScreen.kt:162` | Host: Create listing | 27 subcategories + pricing rules hardcoded | MEDIUM |
| L1 | `HomeViewModel.kt:209` | Home feed | Client-side service/space classification | LOW |
| L2 | `BookingsViewModel.kt:240` | Bookings | Timezone risk in date comparison | LOW |
| L3 | `ProfileViewModel.kt:111` | Profile | Counts default to 0 on API failure | LOW |
| L4 | `PhoneVerificationScreen.kt` | Verification | Cosmetic placeholder phone number | LOW |
| L5 | `SettingsPreferencesScreen.kt` | Settings | TODO: backend options not yet implemented | LOW |

---

## Recommendations

1. **H1/H4:** Serve hero image and destination images from a backend endpoint or CDN config. Add cache headers for performance.
2. **H2:** Trust the API `status` field. If date-based promotion is needed, do it server-side so all clients see the same state.
3. **H3:** Require `cancellationPolicy` from the API response. Show a "loading" or "see listing for details" placeholder instead of a fake policy.
4. **M2/M3:** Surface errors to the user (inline banner or snackbar) when wishlist or host booking fetches fail. Don't silently hide missing data.
5. **M7/M8:** Consider a `/config` or `/metadata` endpoint that serves FAQ content, subcategories, and pricing rules to enable dynamic updates.
