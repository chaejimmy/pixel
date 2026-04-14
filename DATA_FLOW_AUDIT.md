# Data Flow Audit: UI Driven by Real API Data

**Date:** 2026-04-14
**Scope:** Android (Kotlin/Jetpack Compose) PaceDream codebase
**Methodology:** Trace View -> ViewModel -> Service/Repository -> API for every major feature

---

## Executive Summary

The codebase has a solid MVVM architecture with Retrofit-backed API services and Hilt DI.
Most features correctly flow data from API responses through repositories and ViewModels
to the UI. However, there are **critical**, **high**, and **medium** severity issues where
hardcoded data bypasses the API layer entirely, and where API responses are fetched but
discarded.

| Severity | Count | Description |
|----------|-------|-------------|
| CRITICAL | 2 | API response fetched then discarded; hardcoded empty/null returned |
| HIGH     | 5 | Hardcoded content data rendered directly in production UI |
| MEDIUM   | 6 | Hardcoded fallback values that mask API failures or schema drift |
| LOW      | 5 | Static UI option lists that should ideally come from an API |

---

## CRITICAL Severity

### C1. PropertyRepository: API responses fetched and discarded

**File:** `core/data/src/main/kotlin/com/shourov/apps/pacedream/core/data/repository/PropertyRepository.kt`

- **Line 91-117** `searchPropertiesRemote()` — calls `apiService.searchProperties(...)`, captures the response in `val response`, then **ignores it** and returns `Result.Success(emptyList())` on line 112.
- **Line 119-129** `getPropertyByIdRemote()` — calls `apiService.getPropertyById(...)`, captures the response, then **ignores it** and returns `Result.Success(null)` on line 124.
- Both methods have `// This would need to be implemented` comments (lines 110-111, 122-123).

**Impact:** Any feature calling these remote methods gets zero data. The local Room DB
methods work, but the remote search and detail fetch return nothing. The user never
sees remote search results or freshly loaded property details.

### C2. PropertyRepository: `refreshProperties()` saves nothing

**File:** `core/data/src/main/kotlin/com/shourov/apps/pacedream/core/data/repository/PropertyRepository.kt`

- **Line 79-89** `refreshProperties()` — calls `apiService.getAllProperties()`, captures the response, but **never parses or saves it to Room**. Comment on line 82: `// Handle the response and save to database`. Returns `Result.Success(Unit)` regardless.

**Impact:** Room database is never populated by API refresh. Any feature relying on
`getAllProperties()`, `getAvailableProperties()`, or `getTopRatedProperties()` will show
empty results unless data was seeded by another code path.

---

## HIGH Severity

### H1. HomeScreen: Fully hardcoded "Trending Destinations"

**File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeScreen.kt`

- **Lines 1829-1836** `getTrendingDestinations()` — returns a static `listOf(...)` with 6 hardcoded destinations (Grand Canyon, Utah, Maui, Glacier, Honolulu, Sedona) with hardcoded listing counts (42, 38, 55, 29, 67, 31) and Unsplash image URLs.
- **Line 1853** — used directly in the TrendingDestinationsSection composable on the main home screen.

**Impact:** Every user sees the same 6 destinations with fabricated listing counts regardless of their location, preferences, or what actually exists in the database. This is a non-personalized, static section on the most visible screen of the app.

### H2. DashboardContent: Hardcoded destinations list

**File:** `feature/home/presentation/src/main/java/com/shourov/apps/pacedream/feature/home/presentation/components/DashboardContent.kt`

- **Lines 119-124** — hardcoded `destinations = mutableListOf(...)` with 4 cities (London, New York, Tokyo, Toronto) using local drawable resources.

**Impact:** The "Browse by Destination" section on the legacy dashboard shows the same 4 cities for all users with no API backing.

### H3. DashboardContent: Hardcoded categories list

**File:** `feature/home/presentation/src/main/java/com/shourov/apps/pacedream/feature/home/presentation/components/DashboardContent.kt`

- **Lines 76-117** — hardcoded `categories = mutableListOf(...)` with 8 category items (Rest Room, Nap Pod, Meeting Room, Study Room, Short Stay, Apartment, Parking, Storage Space).

**Impact:** Categories are not fetched from the API. If the backend adds or removes a category, the app will not reflect it.

### H4. FilterScreen: Hardcoded property types and amenities

**File:** `feature/home/presentation/src/main/java/com/shourov/apps/pacedream/feature/home/presentation/components/FilterScreen.kt`

- **Line 78** — `propertyTypes = listOf("Apartment", "House", "Villa", "Condo", "Studio")`
- **Lines 132-135** — `amenities = listOf("WiFi", "Parking", "Pool", "Gym", "Kitchen", "AC", "TV", "Pet Friendly", "Balcony", "Garden")`

**Impact:** Filter options don't match actual available property types or amenities in the database. Users may filter by types that don't exist, or miss types that do.

### H5. BrowseByTypeSection: Hardcoded subcategories

**File:** `feature/home/presentation/src/main/java/com/shourov/apps/pacedream/feature/home/presentation/components/BrowseByTypeSection.kt`

- **Lines 72-98** `HomeBrowseType.subcategories` — three hardcoded lists of 6 subcategories each for SPACES, ITEMS, and SERVICES marketplace pillars.

**Impact:** Subcategory taxonomy is baked into the client. Cannot be updated without a new app release.

---

## MEDIUM Severity

### M1. Hardcoded fallback city images (3 duplicated locations)

**Files:**
- `app/src/main/kotlin/com/pacedream/app/feature/destination/DestinationScreen.kt` lines 196-218
- `feature/home/presentation/.../EnhancedDashboardContent.kt` lines 498-517
- `common/src/main/java/com/pacedream/common/composables/components/PaceDreamComponents.kt` lines 489+

All three files contain the **same** duplicated `when` block mapping ~18 city names to hardcoded Unsplash URLs. These are used as fallback when the API doesn't provide an image.

**Impact:** Maintenance burden from triplication. Cities not in the hardcoded list get a generic landscape photo. Images are from Unsplash (third-party dependency) rather than from owned CDN assets.

### M2. ListingCalendarViewModel: Hardcoded availability defaults

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/ListingCalendarViewModel.kt`

- **Line 92** — `listingTimezone = ... ?: "America/New_York"`
- **Line 93** — `availableStartTime = ... ?: "09:00"`
- **Line 94** — `availableEndTime = ... ?: "17:00"`
- **Line 95** — `availableDays = ... ?: listOf(1, 2, 3, 4, 5)`

**Impact:** If the API returns null availability fields (schema drift, new listing, or API bug), the calendar silently shows Mon-Fri 9-5 ET defaults rather than surfacing an error.

### M3. ProfileTabViewModel: Silent API failure masking

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/profile/presentation/ProfileTabViewModel.kt`

- **Lines 127-129** — `fetchBookingsCount()` catches all exceptions and returns `0`
- **Line 166** — `fetchVerificationStatus()` catches all exceptions and returns empty `VerificationStatus()`
- **Line 188** — member-since fetch catches all exceptions and returns `""`

**Impact:** User profile shows 0 bookings, unverified status, and no join date when API fails, with no error indication. User may think they have no bookings.

### M4. SearchViewModel: Silent favorite-IDs failure

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchViewModel.kt`

- **Lines 90-99** — catches all exceptions when loading favorite IDs, keeps last known state with no error indication.

**Impact:** Wishlist hearts may be stale/incorrect with no user-visible feedback.

### M5. HomeSectionListViewModel: Fallback title "Listing"

**File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeSectionListViewModel.kt`

- **Line 138** — `?: "Listing"` fallback when API response has no name/title field.

**Impact:** Cards may display generic "Listing" text instead of real listing names if API schema changes.

### M6. SettingsNotificationsViewModel: Hardcoded notification defaults

**File:** `app/src/main/kotlin/com/pacedream/app/feature/settings/notifications/SettingsNotificationsViewModel.kt`

- **Lines 34-50** — UI state initializes with `emailGeneral = true`, `pushGeneral = true`, `quietHoursStart = "22:00"`, etc.

**Impact:** Brief flash of default values before API response loads. If API call fails, user sees defaults that may not match their actual preferences.

---

## LOW Severity

### L1. SettingsPreferencesScreen: Hardcoded language/currency/timezone options

**File:** `app/src/main/kotlin/com/pacedream/app/feature/settings/preferences/SettingsPreferencesScreen.kt`

- **Lines 67-69** — `languageOptions = listOf("English", "Spanish", "French")`, `currencyOptions = listOf("USD", "EUR", "GBP")`, `timezoneOptions = listOf("UTC", "America/Los_Angeles", "Europe/London")`
- Has `// TODO: Fetch available options from the backend` comment.

### L2. IDVerificationScreen: Hardcoded ID type options

**File:** `app/src/main/kotlin/com/pacedream/app/feature/verification/IDVerificationScreen.kt`

- **Lines 146-150** — `idTypes = listOf("Driver's License", "Passport", "National ID")`

### L3. RoommateFinderScreen: Hardcoded filter options

**File:** `app/src/main/kotlin/com/pacedream/app/feature/roommate/RoommateFinderScreen.kt`

- **Line 37** — `filterOptions = listOf("All", "Looking", "Offering", "Near Me")`

### L4. SearchScreen: Hardcoded category filter chips

**File:** `app/src/main/kotlin/com/pacedream/app/feature/search/SearchScreen.kt`

- **Lines 246-249** — `categories = listOf("Studio", "Meeting Room", "Podcast Studio", "Photo Studio", "Music Studio", "Event Space", "Camera", "Lighting", "Audio")`

### L5. ListingDetailScreen: Hardcoded report reasons and duration options

**File:** `app/src/main/kotlin/com/pacedream/app/feature/listingdetail/ListingDetailScreen.kt`

- **Lines 2988-2996** — 7 hardcoded report reasons
- **Line 608** — `durationOptions = listOf(30, 60, 90, 120)`

---

## Data Flow Summary by Feature

| Feature | View | ViewModel | Repository | API | Status |
|---------|------|-----------|------------|-----|--------|
| Home - Listings | HomeScreen | HomeViewModel | HomeFeedRepository | YES | OK - real API data |
| Home - Categories | DashboardContent | N/A | N/A | NO | **HARDCODED (H3)** |
| Home - Destinations | DashboardContent | N/A | N/A | NO | **HARDCODED (H2)** |
| Home - Trending | HomeScreen | N/A | N/A | NO | **HARDCODED (H1)** |
| Home - Time-based | DashboardContent | HomeScreenVM | HomeRepository | YES | OK - real API data |
| Home - Rented Gear | DashboardContent | HomeScreenVM | HomeRepository | YES | OK - real API data |
| Search | SearchScreen | SearchViewModel | SearchRepository | YES | OK - real API data |
| Search - Filters | FilterScreen | N/A | N/A | NO | **HARDCODED (H4)** |
| Property Detail | ListingDetailScreen | ListingDetailVM | ListingDetailRepo | YES | OK - real API data |
| Property Search (remote) | N/A | N/A | PropertyRepository | DISCARDED | **BROKEN (C1)** |
| Bookings | BookingScreen | BookingViewModel | BookingTabRepository | YES | OK - real API data |
| Chat/Inbox | ChatScreen | ChatViewModel | MessageRepository | YES | OK - real API data |
| Wishlist | WishlistScreen | WishlistViewModel | WishlistRepository | YES | OK - real API data |
| Host Calendar | CalendarScreen | CalendarVM | HostRepository | YES | OK (with hardcoded defaults M2) |
| Profile | ProfileTab | ProfileTabVM | UserRepository | YES | OK (with silent failures M3) |
| Notifications | NotificationScreen | NotificationVM | NotificationRepo | YES | OK - real API data |
| Settings - Preferences | PreferencesScreen | PreferencesVM | API | YES | OK (with hardcoded options L1) |
| Destinations | DestinationScreen | DestinationVM | DestinationRepo | YES | OK (with fallback images M1) |

---

## Recommendations (Priority Order)

1. **Fix C1/C2 immediately:** Implement response parsing in `PropertyRepository.searchPropertiesRemote()`, `getPropertyByIdRemote()`, and `refreshProperties()`. These methods make API calls but throw away the responses.

2. **Replace H1 with API call:** `getTrendingDestinations()` in HomeScreen should fetch from a `/destinations/trending` or equivalent endpoint. Remove hardcoded listing counts.

3. **Fetch H3/H4 categories and filters from API:** Categories, property types, and amenities should come from a backend taxonomy endpoint so the app stays in sync without releases.

4. **De-duplicate M1 fallback images:** Extract the city-to-image mapping into a single utility. Better: serve destination images from the API/CDN.

5. **Add error states for M3/M4:** Replace silent `catch -> return default` patterns with explicit error state propagation so the UI can show retry options.

6. **Mark L1-L5 as tech debt:** These are acceptable for MVP but should be migrated to API-driven options as the backend matures.
