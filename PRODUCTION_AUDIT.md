# Android Production Readiness Audit

**Date**: 2026-04-13
**App**: PaceDream (`com.shourov.apps.pacedream`)
**Scope**: 90+ screens, 60+ ViewModels, 29 repositories, 30+ navigation routes

---

## Executive Summary

The app is **~95% production-ready**. Core flows (search, booking, checkout, inbox, profile, host management) are solid with comprehensive state handling. There are **2 broken host navigation routes** and **1 stubbed screen** that block production. Everything else has proper loading/error/empty states, crash-resilient patterns, and tolerant API parsing.

---

## Must Fix Before Production

### 1. CRITICAL: Broken Navigation Routes in Host Mode

**`booking_details/{bookingId}` route missing from HostNavigationGraph**
- `PaceDreamApp.kt` navigates to this route from `HostModeScreen`, but no composable is defined.
- **Impact**: App will crash when a host taps a booking.
- **Fix**: Add composable in `HostNavigationGraph.kt` pointing to existing `BookingDetailScreen`, or redirect via the existing `NavRoutes.BOOKING_DETAIL` route.

**`withdraw_earnings` route missing**
- Navigated to from `PaceDreamApp.kt`, but no composable definition exists.
- `HostNavigationDestinations` has the constant but it's never composed.
- **Impact**: App will crash when a host taps withdraw.
- **Fix**: Add composable screen or remove the navigation call and disable the button.

### 2. CRITICAL: Hide RoommateFinderScreen

- Intentional stub waiting for roommate API.
- Contains 2 TODO comments for core functionality.
- Filter chips and search bar are non-functional.
- Comment in code: *"Will be replaced with real API-driven listing cards when the roommate API is available."*
- **Fix**: Remove from all navigation entry points or hide behind a feature flag.

### 3. MEDIUM: Replace Hardcoded Hero Image

- `HomeViewModel.kt` (~line 68): Hardcoded Unsplash URL with comment *"fetched from API/config later"*.
- **Fix**: Source from API or remote config.

---

## Should Fix (High Priority)

| Issue | Location | Fix |
|-------|----------|-----|
| Dead route: `WISHLIST_ITEM_DETAIL` | `NavRoutes.kt` | Remove constant or add detail screen |
| Dead route: `COLLECTIONS` | `NavRoutes.kt` | Wire to navigation graph or remove |
| Dead route: `HOST_HOME` / `HOST_LISTINGS` path mismatch | `NavRoutes.kt` | Align paths with actual routes |
| Inconsistent route naming (3 systems) | Navigation layer | Consolidate to one pattern |
| Missing deep link `<intent-filter>` in AndroidManifest | `AndroidManifest.xml` | Add entries for supported deep links |
| Commented-out `START_EMAIL_PHONE_ROUTE` | `SignInNavigation.kt` ~line 30 | Uncomment or remove |

---

## Screen-by-Screen Results

### Fully Production Ready (88 screens)

All screens below have proper **loading**, **error**, and **empty** state handling where applicable:

- **Home**: HomeScreen, HomeSectionListScreen
- **Search**: SearchScreen
- **Listing Detail**: ListingDetailScreen (10+ sections, modals, reviews)
- **Bookings**: BookingsScreen (tab filtering), BookingDetailScreen (cached data pattern)
- **Checkout**: CheckoutScreen (Stripe, process-death recovery), ConfirmationScreen
- **Inbox**: InboxScreen (guest/host toggle, pagination), ThreadScreen (attachments, block/report)
- **Profile**: ProfileScreen, EditProfileScreen (photo compress + upload)
- **Wishlist**: WishlistScreen (optimistic remove)
- **Collections**: CollectionsScreen (CRUD + modals)
- **Settings**: All 7 screens (root, personal info, security, notifications, preferences, payment, help)
- **Verification**: All 3 screens (identity, phone, ID)
- **Reviews**: ReviewsScreen, WriteReviewScreen (eligibility check)
- **Host Mode**: All 13 screens (dashboard, listings, bookings, earnings, analytics, profile, settings, create/edit listing, calendar, Stripe Connect)
- **Special Features**: BiddingScreen, BlogScreen (pagination), TripPlannerScreen, SplitBookingScreen
- **Info**: FAQScreen, AboutUsScreen
- **Auth**: All 11 sign-in/onboarding screens
- **Destination**: DestinationScreen

### Incomplete (1 screen)

| Screen | Issue |
|--------|-------|
| **RoommateFinderScreen** | Stub - no API, 2 TODOs, non-functional filters |

### Screens with Minor Placeholder Content

| Screen | Placeholder | Severity |
|--------|-------------|----------|
| HomeViewModel | 1 hardcoded Unsplash hero URL | Medium |
| DestinationScreen | 20+ hardcoded city fallback URLs | Low |
| SearchScreen | Hardcoded category list | Low |
| SettingsPreferencesScreen | Hardcoded dropdown options (TODO for backend) | Low |

---

## State Handling Coverage

| State | Coverage |
|-------|----------|
| Loading (spinners, skeletons, shimmer) | **100%** of async screens |
| Error (retry, banner, snackbar) | **100%** of async screens |
| Empty (contextual messages, CTAs) | **100%** where applicable |
| Pull-to-refresh | Implemented on list screens |
| Offline / cached data | BookingDetail, Wishlist, Bookings |
| Process-death recovery | CheckoutScreen (PendingPaymentStore) |
| Optimistic updates | Favorites, wishlist remove, message send |

---

## Navigation Audit

### Broken Routes (2)

1. `booking_details/{bookingId}` - navigated from Host mode, no composable
2. `withdraw_earnings` - navigated from Host mode, no composable

### Dead Routes (4)

1. `NavRoutes.ROOMMATE_FINDER` - screen is a stub
2. `NavRoutes.WISHLIST_ITEM_DETAIL` - no screen exists
3. `NavRoutes.COLLECTIONS` - not wired to nav graph
4. `NavRoutes.HOST_HOME` / `HOST_LISTINGS` - path mismatch with actual routes

### Inconsistencies

- Three route naming systems coexist (NavRoutes strings, enum-based, hardcoded)
- Several dashboard routes use inline strings without NavRoutes constants
- No AndroidManifest intent-filters for deep links

---

## Nice to Have

- Move SearchScreen categories to remote config
- Move DestinationScreen fallback images to remote config
- Add backend endpoint for preferences dropdowns
- Audit 19 `contentDescription = null` instances for accessibility
- Add unit tests for critical ViewModels
