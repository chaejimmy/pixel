# PaceDream Feature Parity Report

**Date**: 2026-02-24
**Platforms Analyzed**: Web (pacedream.com), Android (this repo), iOS (reference — no codebase access)
**Report Version**: 1.0

---

## Important Notes

- **Web repo** (`chaejimmy/pacedream-platform`) returns 404 — not accessible. Web features are inferred from: Android proxy endpoints that call the web frontend, the `UI_UX_COMPARISON.md` doc (which was written by comparing pacedream.com against the Android app), and backend API endpoint contracts.
- **iOS repo** is not present in this workspace. The workspace contains the **Android** app. iOS is treated as the "reference" platform since the Android app's README states it was "built to mirror the iOS app."
- **Android repo** is this workspace (`chaejimmy/pixel`). Fully analyzed.

---

# A) WEB FEATURE MAP

> Source of truth: `UI_UX_COMPARISON.md`, Android proxy endpoints to `www.pacedream.com`, backend API contracts, and `ANDROID_IOS_PARITY_PLAN.md`.

## A1. Authentication

| Item | Detail |
|------|--------|
| **Screen** | Login/Signup modal or page |
| **User Actions** | Email/password login, Email/password signup, Auth0 social login (Google, Apple), Forgot password |
| **API Endpoints** | `POST /v1/auth/login/email`, `POST /v1/auth/signup/email`, `POST /v1/auth/auth0/callback`, `POST /v1/auth/refresh-token`, `PUT /v1/auth/forgot-password` |
| **Auth Required** | No (these are unauthenticated endpoints) |
| **Key UX** | Social login via Auth0 Universal Login, token refresh with fallback to `www.pacedream.com/api/proxy/auth/refresh-token` |
| **Evidence** | Android `AuthRepository.kt` mirrors these endpoints; `AppConfig.kt` defines frontend proxy URL |

## A2. Home / Landing Page

| Item | Detail |
|------|--------|
| **Screen** | Homepage with hero, search, and content sections |
| **User Actions** | View hero section, search via multi-field form, browse categories, view hourly spaces / rent gear / split stays, "View All" for each section |
| **API Endpoints** | `GET /v1/properties/filter-rentable-items-by-group/time_based?item_type=room`, `GET /v1/gear-rentals/get/hourly-rental-gear/tech_gear`, `GET /v1/roommate/get/room-stay` |
| **Auth Required** | No |
| **Key UI/UX** | Hero with "One place to share it all" headline, "Get to know PaceDream" CTA button, Use/Borrow/Split tabs, WHAT/WHERE/DATES search fields, category quick-filter chips (Restroom, Nap Pod, Meeting Room, Study Room, Short Stay, Apartment, Luxury Room, Parking, Storage Space), FAQ section with expandable items, "Get in Touch" support section, email subscription form |
| **Evidence** | `UI_UX_COMPARISON.md` lines 13-17 (hero), lines 39-44 (search tabs), lines 67-76 (categories), lines 129-155 (FAQ/support) |

## A3. Search

| Item | Detail |
|------|--------|
| **Screen** | Search page with tabs and results |
| **User Actions** | Switch Use/Borrow/Split tabs, enter WHAT/WHERE/DATES, filter by category, sort results |
| **API Endpoints** | `GET /v1/properties/filter-rentable-items-by-group/time_based?q=...&category=...` (Use tab), `GET /v1/gear-rentals/get/hourly-rental-gear/tech_gear?q=...&category=...` (Borrow tab), `GET /v1/roommate/get/room-stay?q=...&category=...` (Split tab), `GET /v1/search/autocomplete?q=...`, `GET www.pacedream.com/api/search?...` |
| **Auth Required** | No |
| **Key UX** | 3 tabs, multi-field search (WHAT/WHERE/DATES), autocomplete suggestions, category filtering, pagination |
| **Evidence** | Android `SearchViewModel.kt`, `SearchRepository.kt` |

## A4. Listing Detail

| Item | Detail |
|------|--------|
| **Screen** | Property/listing detail page |
| **User Actions** | View images (carousel), read description, see location on map, view pricing, view host info, view amenities, read reviews, toggle wishlist/favorite, reserve/book |
| **API Endpoints** | `GET /v1/listings/{listingId}`, `GET /v1/reviews/property/{listingId}`, `POST /v1/wishlists/add` or `POST /v1/account/wishlist/toggle` |
| **Auth Required** | Listing detail: yes (auth header sent). Reviews: no. Wishlist toggle: yes. |
| **Key UX** | Image carousel, location map with geocoding, pricing display (hourly/nightly), host card (name, avatar, superhost badge), amenity chips, review summary (average + category averages: cleanliness, accuracy, communication, location, check-in, value), individual review cards, cancellation policy display, "Reserve" CTA |
| **Evidence** | Android `ListingDetailViewModel.kt`, `ListingDetailRepository.kt`, `ReviewRepository.kt`, `ListingWishlistRepository.kt` |

## A5. Reviews & Ratings

| Item | Detail |
|------|--------|
| **Screen** | Embedded in listing detail; possibly standalone review page |
| **User Actions** | View review summary, view individual reviews, write a review (rating + comment + category ratings) |
| **API Endpoints** | `GET /v1/reviews/property/{propertyId}`, `POST /v1/reviews`, `GET /v1/reviews/user/{userId}` |
| **Request Fields** | `CreateReviewRequest`: listingId, rating (1-5), comment, categoryRatings (cleanliness, accuracy, communication, location, checkIn, value) |
| **Auth Required** | View: no. Write: yes. |
| **Key UX** | Star rating, category breakdown, rating distribution histogram |
| **Evidence** | Android `ReviewRepository.kt` |

## A6. Bookings / Reservations

| Item | Detail |
|------|--------|
| **Screen** | Checkout flow, My Bookings list, Booking detail |
| **User Actions** | Select dates/times, select guests, reserve, confirm via Stripe, view booking history, view booking detail |
| **API Endpoints** | `POST /v1/properties/bookings/timebased` (time-based → Stripe checkout URL), `POST /v1/gear-rentals/book` (gear → Stripe checkout URL), `GET /v1/properties/bookings/timebased/success/checkout?session_id=...`, `GET /v1/gear-rentals/success/checkout?session_id=...`, `GET /v1/bookings/mine`, `POST /v1/bookings`, `GET /v1/bookings/{bookingId}` |
| **Auth Required** | Yes |
| **Key UX** | Stripe Checkout via redirect (web) or Chrome Custom Tabs (mobile), session persistence for resume, deep links for success/cancel (`/booking-success`, `/booking-cancelled`), booking types: time-based (rooms/spaces) and gear rental |
| **Evidence** | Android `CheckoutViewModel.kt`, `BookingConfirmationViewModel.kt`, `BookingsViewModel.kt`, deep link intent filters in `AndroidManifest.xml` |

## A7. Chat / Messaging (Inbox)

| Item | Detail |
|------|--------|
| **Screen** | Inbox (thread list), Thread view |
| **User Actions** | View threads, switch guest/host mode, view messages, send message, archive thread |
| **API Endpoints** | `GET /v1/inbox/threads?limit=20&cursor=...&mode=guest\|host`, `GET /v1/inbox/unread-counts`, `GET /v1/inbox/threads/{id}/messages`, `POST /v1/inbox/threads/{id}/messages`, `POST /v1/inbox/threads/{id}/archive` |
| **Auth Required** | Yes |
| **Key UX** | Cursor-based pagination (20 per page), unread badge, guest/host mode toggle, relative time formatting, REST-based (no realtime websockets observed) |
| **Evidence** | Android `InboxViewModel.kt`, `ThreadViewModel.kt` |

## A8. Wishlist / Favorites

| Item | Detail |
|------|--------|
| **Screen** | Wishlist tab (bottom nav) |
| **User Actions** | View saved listings, filter by All/Spaces/Gear, remove from wishlist, tap to view detail |
| **API Endpoints** | `GET /v1/account/wishlist`, `POST /v1/account/wishlist/toggle` |
| **Auth Required** | Yes (locked/empty state for guests) |
| **Key UX** | Filter tabs (All, Spaces, Gear), optimistic remove with rollback on failure, routing to correct detail screen by item type |
| **Evidence** | Android `WishlistViewModel.kt` |

## A9. Collections (User-Created Lists)

| Item | Detail |
|------|--------|
| **Screen** | Collections list, Collection detail |
| **User Actions** | View collections, create collection, add listing to collection, remove from collection, delete collection |
| **API Endpoints** | `GET /v1/collections`, `POST /v1/collections`, `GET /v1/collections/{id}`, `POST /v1/collections/{id}/items`, `DELETE /v1/collections/{id}/items/{itemId}`, `DELETE /v1/collections/{id}` |
| **Auth Required** | Yes |
| **Key UX** | Public/private toggle, cover image from first item |
| **Evidence** | Android `CollectionRepository.kt`, `CollectionsViewModel.kt` |

## A10. Payments / Payment Methods

| Item | Detail |
|------|--------|
| **Screen** | Settings > Payment Methods |
| **User Actions** | View saved cards, add new card (Stripe SetupIntent), set default, delete card |
| **API Endpoints** | `GET www.pacedream.com/api/proxy/account/payment-methods`, `POST www.pacedream.com/api/proxy/account/payment-methods/create-setup-intent`, `POST www.pacedream.com/api/proxy/account/payment-methods/default`, `DELETE www.pacedream.com/api/proxy/account/payment-methods/{id}` |
| **Auth Required** | Yes |
| **Key UX** | Stripe card display (brand, last4, expiry), default card badge, SetupIntent for adding new cards |
| **Evidence** | Android `PaymentMethodsRepository.kt`, `PaymentMethodsViewModel.kt` |

## A11. Notifications

| Item | Detail |
|------|--------|
| **Screen** | Notification list (inferred from endpoints) |
| **User Actions** | View notifications, mark as read, mark all as read, register push token |
| **API Endpoints** | `GET /v1/notifications/user/{userId}`, `PUT /v1/notifications/{notificationId}/read`, `PUT /v1/notifications/user/{userId}/read-all`, `POST /v1/notifications/register-device` |
| **Auth Required** | Yes |
| **Key UX** | Push notifications via FCM (Android) / APNs (iOS), notification preferences (email general, push general, messages, booking updates, booking alerts, marketing) |
| **Evidence** | Android `PaceDreamFirebaseMessagingService.kt`, `SettingsNotificationsScreen.kt`, `ApiEndPoints.kt` |

## A12. Host Dashboard

| Item | Detail |
|------|--------|
| **Screen** | Host dashboard, Host listings, Host bookings, Host earnings, Host analytics |
| **User Actions** | View dashboard overview, manage listings (create/edit/delete), manage bookings (accept/decline), view earnings + request withdrawal, view analytics |
| **API Endpoints** | `GET /v1/host/dashboard`, `GET /v1/host/listings`, `POST /v1/host/listings`, `PUT /v1/host/listings/{id}`, `DELETE /v1/host/listings/{id}`, `GET /v1/host/bookings`, `POST /v1/host/bookings/{id}/accept`, `POST /v1/host/bookings/{id}/decline`, `GET /v1/host/earnings`, `POST /v1/host/earnings/withdraw`, `GET /v1/host/analytics` |
| **Auth Required** | Yes |
| **Key UX** | Guest/Host mode toggle in profile, host-specific navigation, analytics (views, bookings, revenue, occupancy rate, average rating, conversion rate) |
| **Evidence** | Android `HostApiService.kt`, `HostRepository.kt`, `HostDashboardViewModel.kt` |

## A13. Account Settings

| Item | Detail |
|------|--------|
| **Screen** | Settings root, Personal Info, Login & Security, Notifications, Preferences, Payment Methods, Help & Support |
| **User Actions** | Edit name/email/phone, change password, deactivate account, toggle notification preferences, set language/currency/timezone, manage payment methods |
| **API Endpoints** | Via frontend proxy: `GET/PUT www.pacedream.com/api/proxy/account/me`, `PUT .../account/profile`, `PUT .../account/password`, `POST .../account/deactivate`, `GET/PUT .../account/notifications`, `GET/PUT .../account/preferences` |
| **Auth Required** | Yes |
| **Key UX** | Nested settings navigation, form validation |
| **Evidence** | Android `AccountSettingsRepository.kt`, `Settings*.kt` screens |

## A14. Verification

| Item | Detail |
|------|--------|
| **Screen** | Identity verification hub, Phone verification, ID verification |
| **User Actions** | View verification status, send phone OTP, verify phone, upload ID documents, submit verification |
| **API Endpoints** | `GET /v1/users/verification`, `POST /v1/users/verification/phone/send-code`, `POST /v1/users/verification/phone/confirm`, `POST /v1/users/verification/upload-urls`, `POST /v1/users/verification/submit` |
| **Auth Required** | Yes |
| **Key UX** | OTP auto-retrieval (Android SMS Retriever API), multi-step verification flow |
| **Evidence** | Android `PhoneVerificationViewModel.kt`, `IDVerificationViewModel.kt`, `IdentityVerificationScreen.kt` |

## A15. FAQ

| Item | Detail |
|------|--------|
| **Screen** | FAQ page with expandable items |
| **User Actions** | Expand/collapse FAQ items |
| **Questions** | How does hourly booking work?, What types of spaces can I book?, Is there a cancellation policy?, How do I contact support?, Are there any membership benefits?, How do I become a host? |
| **Auth Required** | No |
| **Key UX** | Expandable accordion items |
| **Evidence** | `UI_UX_COMPARISON.md` lines 129-136 |

## A16. Support / Contact

| Item | Detail |
|------|--------|
| **Screen** | "Get in Touch" / Help section |
| **User Actions** | Message support, send direct email |
| **Key UX** | "Need Help?" heading, "Message Support" button, "Shoot a Direct Mail" button |
| **Evidence** | `UI_UX_COMPARISON.md` lines 137-142 |

---

# B) ANDROID FEATURE MAP

> Source: Full codebase analysis of `/home/user/pixel`

## B1. Authentication

| Item | Detail |
|------|--------|
| **Screen** | `AuthBottomSheet` (modal overlay, not a route) with modes: Chooser / SignIn / SignUp |
| **User Actions** | Email/password login, email/password signup, Auth0 social login (Google, Apple), forgot password |
| **API Endpoints** | `POST /v1/auth/login/email`, `POST /v1/auth/signup/email`, `POST /v1/auth/auth0/callback`, `POST /v1/auth/refresh-token` (+ fallback to frontend proxy) |
| **Auth Required** | No |
| **Key UX** | Bottom sheet modal (not full-screen route), Auth0 WebAuthProvider, token storage via EncryptedSharedPreferences, immediate auth on launch if tokens exist, graceful degradation on non-401 errors |
| **Files** | `app/.../ui/components/AuthBottomSheet.kt`, `app/.../ui/components/AuthFlowSheetViewModel.kt`, `app/.../core/auth/AuthRepository.kt`, `app/.../core/auth/Auth0Connection.kt`, `core/network/.../auth/TokenStorage.kt` |

## B2. Home Feed

| Item | Detail |
|------|--------|
| **Screen** | `HomeScreen` (bottom tab), `HomeSectionListScreen` ("View All") |
| **User Actions** | View hero section, browse 3 sections (Hourly Spaces, Rent Gear, Split Stays), pull to refresh, "See All" navigation |
| **API Endpoints** | `GET /v1/properties/filter-rentable-items-by-group/time_based?item_type=room`, `GET /v1/gear-rentals/get/hourly-rental-gear/tech_gear`, `GET /v1/roommate/get/room-stay` |
| **Auth Required** | No |
| **Key UX** | 3 parallel API calls, sections that fail are hidden (non-blocking), warning banner on partial failure, pull-to-refresh, shimmer loading placeholders |
| **Files** | `app/.../feature/home/HomeViewModel.kt`, `app/.../feature/home/HomeScreen.kt`, `feature/home/.../HomeScreenViewModel.kt`, `feature/home/.../DashboardScreen.kt`, `feature/home/.../EnhancedDashboardScreen.kt` |
| **Gaps vs Web** | Hero text mismatch ("Share it. Borrow it. Split it." vs "One place to share it all"), missing "Get to know PaceDream" CTA button, placeholder gradient instead of hero image, missing Use/Borrow/Split tabs on home search, missing 4 category filters (Study Room, Short Stay, Apartment, Luxury Room) |

## B3. Search

| Item | Detail |
|------|--------|
| **Screen** | `SearchScreen` (bottom tab) |
| **User Actions** | Type query, switch USE/BORROW/SPLIT tabs, filter by category |
| **API Endpoints** | Same as web (per-tab endpoints), `GET /v1/search/autocomplete?q=...`, `GET www.pacedream.com/api/search?...` with backend fallback |
| **Auth Required** | No |
| **Key UX** | 3 tabs (Use, Borrow, Split) in ViewModel but search UI is simplified single-bar ("Where to?"), 400ms debounce, category filtering, tolerant response parsing |
| **Files** | `app/.../feature/search/SearchViewModel.kt`, `app/.../feature/search/SearchRepository.kt`, `feature/search/.../SearchScreen.kt` |
| **Gaps vs Web** | Single search bar instead of WHAT/WHERE/DATES multi-field, no date picker, no location picker with "Use my location" |

## B4. Listing Detail

| Item | Detail |
|------|--------|
| **Screen** | `ListingDetailScreen` |
| **User Actions** | View images, read description, view map, view pricing, view host info, view amenities, read reviews, toggle favorite, reserve |
| **API Endpoints** | `GET /v1/listings/{listingId}`, `GET /v1/reviews/property/{listingId}`, `POST /v1/wishlists/add` / `POST /v1/account/wishlist/toggle`, `DELETE /v1/wishlists/{id}` |
| **Auth Required** | Detail: yes. Reviews view: no. Wishlist: yes. |
| **Key UX** | Image carousel, geocoding for map, pricing display, host card, amenity list, review summary + individual reviews, optimistic wishlist toggle, cancellation policy display |
| **Files** | `app/.../feature/listingdetail/ListingDetailViewModel.kt`, `app/.../feature/listingdetail/ListingDetailRepository.kt`, `app/.../feature/listingdetail/ListingWishlistRepository.kt`, `app/.../feature/listingdetail/ReviewRepository.kt` |

## B5. Reviews & Ratings

| Item | Detail |
|------|--------|
| **Screen** | Embedded in `ListingDetailScreen` |
| **User Actions** | View summary (average, category averages, distribution), view individual reviews, write review |
| **API Endpoints** | `GET /v1/reviews/property/{propertyId}`, `POST /v1/reviews` |
| **Request Model** | `CreateReviewRequest(listingId, rating, comment, categoryRatings(cleanliness, accuracy, communication, location, checkIn, value))` |
| **Auth Required** | View: no. Write: yes. |
| **Files** | `app/.../feature/listingdetail/ReviewRepository.kt`, `ListingDetailViewModel.kt` |
| **Gaps vs Web** | Edit/delete reviews not implemented (endpoints exist: `GET /v1/reviews/user/{userId}` but no edit/delete) |

## B6. Bookings

| Item | Detail |
|------|--------|
| **Screen** | `BookingsScreen` (list), `BookingDetailScreen`, `CheckoutScreen`, `ConfirmationScreen` |
| **User Actions** | View my bookings, view booking detail, checkout via Stripe, confirm booking |
| **API Endpoints** | `GET /v1/bookings/mine`, `POST /v1/properties/bookings/timebased`, `POST /v1/gear-rentals/book`, `GET /v1/properties/bookings/timebased/success/checkout?session_id=...`, `GET /v1/gear-rentals/success/checkout?session_id=...` |
| **Auth Required** | Yes |
| **Key UX** | Stripe checkout via Chrome Custom Tabs, session persistence for resume, deep link handling (booking-success / booking-cancelled), booking types: time-based + gear |
| **Files** | `app/.../feature/bookings/BookingsViewModel.kt`, `app/.../feature/bookings/BookingsScreen.kt`, `app/.../feature/bookings/BookingDetailScreen.kt`, `app/.../feature/checkout/CheckoutViewModel.kt`, `feature/webflow/.../BookingConfirmationViewModel.kt`, `feature/webflow/.../CheckoutLauncher.kt` |

## B7. Chat / Messaging

| Item | Detail |
|------|--------|
| **Screen** | `InboxScreen` (bottom tab), `ThreadScreen` |
| **User Actions** | View thread list, switch guest/host mode, view messages, send message, archive thread |
| **API Endpoints** | `GET /v1/inbox/threads?limit=20&cursor=...&mode=guest\|host`, `GET /v1/inbox/unread-counts`, `GET /v1/inbox/threads/{id}/messages`, `POST /v1/inbox/threads/{id}/messages`, `POST /v1/inbox/threads/{id}/archive` |
| **Auth Required** | Yes |
| **Key UX** | Cursor-based pagination, unread count badge, guest/host mode, tolerant JSON parsing, relative time formatting |
| **Files** | `app/.../feature/inbox/InboxViewModel.kt`, `app/.../feature/inbox/InboxScreen.kt`, `app/.../feature/inbox/ThreadViewModel.kt`, `app/.../feature/inbox/ThreadScreen.kt` |

## B8. Wishlist

| Item | Detail |
|------|--------|
| **Screen** | `WishlistScreen` (bottom tab) |
| **User Actions** | View favorites, filter (All/Spaces/Gear), remove item, navigate to detail |
| **API Endpoints** | `GET /v1/account/wishlist`, `POST /v1/account/wishlist/toggle` |
| **Auth Required** | Yes (locked state for unauthenticated) |
| **Key UX** | 3 filter tabs, optimistic remove with rollback, type-based routing to detail |
| **Files** | `app/.../feature/wishlist/WishlistViewModel.kt`, `app/.../feature/wishlist/WishlistScreen.kt` |

## B9. Collections

| Item | Detail |
|------|--------|
| **Screen** | `CollectionsScreen`, Collection detail |
| **User Actions** | View collections, create, add/remove items, delete |
| **API Endpoints** | Full CRUD: `GET/POST /v1/collections`, `GET/DELETE /v1/collections/{id}`, `POST /v1/collections/{id}/items`, `DELETE /v1/collections/{id}/items/{itemId}` |
| **Auth Required** | Yes |
| **Files** | `app/.../feature/collections/CollectionRepository.kt`, `app/.../feature/collections/CollectionsViewModel.kt`, `app/.../feature/collections/CollectionsScreen.kt` |

## B10. Payments

| Item | Detail |
|------|--------|
| **Screen** | `SettingsPaymentMethodsScreen` |
| **User Actions** | View saved cards, add new (Stripe SetupIntent), set default, delete |
| **API Endpoints** | Via frontend proxy: `GET/POST/DELETE www.pacedream.com/api/proxy/account/payment-methods/*` |
| **Auth Required** | Yes |
| **Files** | `app/.../feature/settings/payment/PaymentMethodsRepository.kt`, `app/.../feature/settings/payment/PaymentMethodsViewModel.kt`, `app/.../feature/settings/SettingsPaymentMethodsScreen.kt` |

## B11. Notifications

| Item | Detail |
|------|--------|
| **Screen** | FCM push notifications, notification settings in `SettingsNotificationsScreen` |
| **User Actions** | Receive push notifications, toggle notification preferences |
| **API Endpoints** | `POST /v1/notifications/register-device`, `GET/PUT www.pacedream.com/api/proxy/account/notifications` |
| **Auth Required** | Yes |
| **Key UX** | FCM integration, 6 notification preference toggles |
| **Files** | `app/.../notification/PaceDreamFirebaseMessagingService.kt`, `app/.../notification/PaceDreamNotificationService.kt`, `app/.../feature/settings/SettingsNotificationsScreen.kt` |
| **Gaps** | No in-app notification list screen (only push + settings). Endpoints exist (`GET /v1/notifications/user/{userId}`) but no UI for viewing notification history. |

## B12. Host Dashboard

| Item | Detail |
|------|--------|
| **Screen** | `HostDashboardScreen`, `HostListingsScreen`, `HostBookingsScreen`, `HostEarningsScreen` |
| **User Actions** | View dashboard, manage listings (CRUD), manage bookings (accept/decline), view earnings, request withdrawal, view analytics |
| **API Endpoints** | Full host API: `GET /v1/host/dashboard`, `GET/POST/PUT/DELETE /v1/host/listings/*`, `GET /v1/host/bookings`, `POST /v1/host/bookings/{id}/accept\|decline`, `GET /v1/host/earnings`, `POST /v1/host/earnings/withdraw`, `GET /v1/host/analytics` |
| **Auth Required** | Yes |
| **Files** | `app/.../feature/host/data/HostApiService.kt`, `app/.../feature/host/data/HostRepository.kt`, `app/.../feature/host/presentation/HostDashboardViewModel.kt`, `feature/host/` module |

## B13. Account Settings

| Item | Detail |
|------|--------|
| **Screen** | `SettingsRootScreen` + 6 sub-screens (Personal Info, Login & Security, Notifications, Preferences, Payment Methods, Help & Support) |
| **User Actions** | Edit profile, change password, deactivate account, toggle notifications, set preferences, manage payment methods |
| **API Endpoints** | Via frontend proxy: `GET/PUT www.pacedream.com/api/proxy/account/*` |
| **Auth Required** | Yes |
| **Files** | `app/.../feature/settings/AccountSettingsRepository.kt`, `app/.../feature/settings/Settings*.kt` |

## B14. Verification

| Item | Detail |
|------|--------|
| **Screen** | `IdentityVerificationScreen`, `PhoneVerificationScreen`, `IDVerificationScreen` |
| **User Actions** | Check status, verify phone (OTP), upload ID, submit verification |
| **API Endpoints** | `GET /v1/users/verification`, `POST /v1/users/verification/phone/send-code`, `POST /v1/users/verification/phone/confirm`, `POST /v1/users/verification/upload-urls`, `POST /v1/users/verification/submit` |
| **Auth Required** | Yes |
| **Key UX** | SMS Retriever API for auto-OTP on Android |
| **Files** | `app/.../PhoneVerificationScreen.kt`, `app/.../PhoneVerificationViewModel.kt`, `app/.../IDVerificationScreen.kt`, `app/.../IDVerificationViewModel.kt`, `app/.../IdentityVerificationScreen.kt`, `core/data/.../receiver/OTPReceiver.kt` |

## B15. Other Screens

| Screen | Status | File |
|--------|--------|------|
| `ProfileScreen` | Implemented | `app/.../feature/profile/ProfileScreen.kt` |
| `FAQScreen` | Implemented (basic) | `app/.../FAQScreen.kt` |
| `AboutUsScreen` | Implemented | `app/.../AboutUsScreen.kt` |
| `RoommateFinderScreen` | Implemented | `app/.../RoommateFinderScreen.kt` |

---

# C) iOS FEATURE MAP

> **Status: iOS codebase is NOT accessible in this workspace.**

The Android README states: *"Android port of the PaceDream iOS app, built with Kotlin and Jetpack Compose. This app mirrors the iOS app's UI/UX, backend API behavior, and feature set."*

The `ANDROID_IOS_PARITY_PLAN.md` confirms iOS is the reference platform. The Android app's networking layer is explicitly labeled "iOS Parity" in code comments and documentation.

**What we know about iOS** (from Android code references):
- Uses the same backend endpoints
- Has Auth0 integration
- Has the same token refresh + fallback strategy
- Has the same retry logic (GET-only, 2 retries, backoff)
- Has the same HTML hardening
- Has the same in-flight request deduplication
- Is the visual reference for the Android app's design system
- Uses the same API contracts (tolerant parsing handles both platforms' responses)

**To complete the iOS Feature Map**: Provide the iOS repo URL or path and I will scan it with the same thoroughness.

---

# D) GAP REPORT — Web vs Android

## P0: Launch-Blocking Gaps

### P0-1. Search Interface — Missing Multi-Field Search (WHAT/WHERE/DATES)

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch — missing UI |
| **Web** | 3-field search: WHAT (keywords), WHERE (city/address + "Use my location"), DATES (date picker) |
| **Android** | Single search bar: "Where to?" — no WHAT/WHERE/DATES separation, no date picker, no location picker |
| **Impact** | Users cannot search by date or use structured location search on Android. Core discovery flow is significantly degraded. |
| **Web Evidence** | `UI_UX_COMPARISON.md` lines 39-53 |
| **Android Evidence** | `feature/search/.../SearchScreen.kt` — single `OutlinedTextField` |
| **Fix** | Create `EnhancedSearchBar.kt` with 3 fields, `SearchTabSelector.kt`, date picker, location picker with GPS |

### P0-2. Search Tabs Missing on Home Page

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch — missing UI |
| **Web** | Use/Borrow/Split tabs integrated into home page search card |
| **Android** | Tabs exist in Search screen ViewModel logic but NOT in the home page search card UI |
| **Impact** | Users must navigate to Search tab to switch modes; home page doesn't offer this. |
| **Web Evidence** | `UI_UX_COMPARISON.md` lines 39-41 |
| **Android Evidence** | `feature/home/.../DashboardScreen.kt` — no tab selector in search area |
| **Fix** | Add tab selector to home search card, connect to SearchViewModel tab state |

### P0-3. Notification History Screen Missing

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing screen/flow |
| **Web** | Notification list (assumed from API endpoint existence) |
| **Android** | No `NotificationListScreen`. Endpoints defined (`GET /v1/notifications/user/{userId}`, `PUT .../read`, `PUT .../read-all`) but no UI consumes them. Only FCM push + settings exist. |
| **Impact** | Users cannot view past notifications in-app. |
| **Android Evidence** | `core/network/.../ApiEndPoints.kt` has `GET_USER_NOTIFICATIONS`, `MARK_NOTIFICATION_READ`, `MARK_ALL_NOTIFICATIONS_READ` but no screen/ViewModel uses them |
| **Fix** | Create `NotificationListScreen`, `NotificationListViewModel`, wire to existing endpoints |

### P0-4. Create/Edit Listing for Hosts — Incomplete UI

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing screen — API exists but UI is minimal |
| **Web** | Full host listing creation/edit flow (assumed from endpoint: `POST /v1/host/listings`, `PUT /v1/host/listings/{id}`) |
| **Android** | `HostApiService.kt` defines `createListing()`, `updateListing()`, `deleteListing()` but the actual form screens for creating/editing listings are not evident in the codebase. Only `HostListingsScreen` (list view) exists. |
| **Impact** | Hosts cannot create or edit listings on Android. |
| **Android Evidence** | `app/.../feature/host/data/HostApiService.kt` has endpoints but no `CreateListingScreen.kt` or `EditListingScreen.kt` found |
| **Fix** | Create multi-step listing creation form (title, description, photos, location, pricing, amenities, rules, availability) |

## P1: Important Gaps

### P1-1. Hero Section Text & CTA Mismatch

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch |
| **Web** | Headline: "One place to share it all", CTA: "Get to know PaceDream" button with purple gradient |
| **Android** | Headline: "Share it. Borrow it. Split it.", no CTA button, placeholder gradient instead of hero image |
| **Web Evidence** | `UI_UX_COMPARISON.md` lines 13-17 |
| **Android Evidence** | `feature/home/.../HomeFeedScreen.kt` |
| **Fix** | Update headline text, add CTA button, replace gradient with hero image |

### P1-2. Missing Category Filters

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing data/UI |
| **Web** | 9 categories: Restroom, Nap Pod, Meeting Room, Study Room, Short Stay, Apartment, Luxury Room, Parking, Storage Space |
| **Android** | 6 categories: All, Restroom, Nap Pod, Meeting Room, Storage, Parking |
| **Missing** | Study Room, Short Stay, Apartment, Luxury Room |
| **Web Evidence** | `UI_UX_COMPARISON.md` lines 67-76 |
| **Android Evidence** | `feature/home/.../components/CategoryFilter.kt` |
| **Fix** | Add 4 missing categories with icons, ensure order matches web |

### P1-3. FAQ Section — No Expandable UI

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch |
| **Web** | Expandable FAQ accordion with 6 questions |
| **Android** | `FAQScreen.kt` exists but appears basic (no accordion expand/collapse confirmed) |
| **ANDROID_IOS_PARITY_PLAN.md Evidence** | Lines 101-128 explicitly call out FAQ UI is missing |
| **Fix** | Implement expandable FAQ items with 200ms easeInOut animation |

### P1-4. Support/Contact Section

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing dedicated screen |
| **Web** | "Get in Touch" section: "Need Help?" heading, "Message Support" button, "Shoot a Direct Mail" button |
| **Android** | Help exists only as menu item in Profile > Settings > Help & Support. No dedicated support screen matching web. |
| **Web Evidence** | `UI_UX_COMPARISON.md` lines 137-142 |
| **Fix** | Create `SupportScreen.kt` with messaging and email support buttons |

### P1-5. Review Edit/Delete

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing endpoint calls |
| **Web** | Likely supports edit/delete of own reviews |
| **Android** | Only `GET /v1/reviews/property/{id}` (view) and `POST /v1/reviews` (create). No edit/delete for reviews. |
| **Android Evidence** | `ReviewRepository.kt` — only `fetchReviews()` and `createReview()` |
| **Fix** | Add `PUT /v1/reviews/{id}` and `DELETE /v1/reviews/{id}` if backend supports them |

### P1-6. Booking Availability Check

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing endpoint call |
| **Web** | Date/time availability check before booking |
| **Android** | Endpoint defined (`GET /v1/bookings/availability/{propertyId}` in `ApiEndPoints.kt`) but not called by any ViewModel or Repository in the app module |
| **Android Evidence** | `ApiEndPoints.kt` has `GET_BOOKING_AVAILABILITY` but `CheckoutViewModel.kt` doesn't call it |
| **Fix** | Call availability endpoint in checkout flow before creating booking |

### P1-7. Real-time / Polling for Inbox

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch |
| **Web** | Likely has real-time updates or polling for new messages |
| **Android** | Pure REST — messages only loaded on screen open and manual refresh. No polling or WebSocket. |
| **Android Evidence** | `InboxViewModel.kt`, `ThreadViewModel.kt` — no periodic refresh or WebSocket |
| **Fix** | Add polling interval (e.g., 15s on thread view, 60s on inbox list) or WebSocket if backend supports it |

### P1-8. Inline Error Banners (not Snackbars)

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch |
| **Web/iOS** | Inline error banners (padding 12dp, radius 14dp, error bg @ 10%) |
| **Android** | Mix of Snackbar/Toast and inline errors — not standardized |
| **ANDROID_IOS_PARITY_PLAN.md Evidence** | Lines 157-179 explicitly call this out |
| **Fix** | Create shared `ErrorBanner` composable, replace all Snackbar/Toast usage |

## P2: Later / Nice-to-Have

### P2-1. Email Subscription

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing feature |
| **Web** | "Stay Updated" email subscription form |
| **Android** | Not implemented |
| **Priority** | Low — not essential for mobile app |

### P2-2. Footer Legal Links as Dedicated Section

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch (acceptable) |
| **Web** | Footer with Terms, Privacy, Cookies, Cancellation & Refund links |
| **Android** | Legal links exist in Profile > Settings — acceptable for mobile paradigm |
| **Priority** | Low — current approach is idiomatic for mobile |

### P2-3. Animation Timing Audit

| Field | Detail |
|-------|--------|
| **Gap Type** | UX mismatch (polish) |
| **Web/iOS** | 200ms easeInOut for transitions, 500ms shimmer |
| **Android** | Need systematic audit of all animation timings |
| **ANDROID_IOS_PARITY_PLAN.md Evidence** | Lines 131-153 |
| **Fix** | Create animation constants, audit and update all animations |

### P2-4. Design System Color/Typography Verification

| Field | Detail |
|-------|--------|
| **Gap Type** | Potential visual mismatch |
| **Expected** | PaceDream Primary: `#5527D7`, System Indigo: `#5856D6`, etc. |
| **Android** | Design system exists but hex values not verified against web/iOS |
| **ANDROID_IOS_PARITY_PLAN.md Evidence** | Lines 308-376 |
| **Fix** | Pixel-level comparison of colors, typography, spacing |

### P2-5. Roommate Finder Enhancement

| Field | Detail |
|-------|--------|
| **Gap Type** | Potentially incomplete |
| **Web** | "Split Stays" is a full section with filtering |
| **Android** | `RoommateFinderScreen.kt` exists but depth of implementation unclear |
| **Fix** | Verify feature completeness against web |

### P2-6. Analytics Event Tracking

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing/incomplete tracking |
| **Endpoints** | `POST /v1/analytics/event`, `POST /v1/analytics/property-view` |
| **Android** | Firebase Analytics module exists but unclear if backend analytics events are sent |
| **Fix** | Verify property view tracking and event tracking are wired up |

---

# E) IMPLEMENTATION PLAN — P0 & P1 (Android)

> **Note**: iOS codebase is not available. When provided, a parallel iOS plan will be created. The plan below covers Android only.

---

## Phase 1: P0 Fixes (Critical — Launch Blocking)

### PR 1: Enhanced Search Interface (P0-1 + P0-2)

**Scope**: Multi-field search (WHAT/WHERE/DATES) + Use/Borrow/Split tabs on both Search screen and Home page.

#### API Layer
- No new endpoints needed — search endpoints already exist
- **File**: `app/.../feature/search/SearchRepository.kt` — add `searchWithFilters(query, where, startDate, endDate, tab)` method that maps to existing per-tab endpoints with additional query params

#### Data/Models
- **Create**: `app/.../feature/search/model/SearchFilters.kt`
  ```kotlin
  data class SearchFilters(
      val what: String = "",
      val where: String = "",
      val startDate: LocalDate? = null,
      val endDate: LocalDate? = null,
      val tab: SearchTab = SearchTab.USE
  )
  enum class SearchTab { USE, BORROW, SPLIT }
  ```

#### UI
- **Create**: `app/.../feature/search/components/EnhancedSearchBar.kt` — WHAT/WHERE/DATES fields in a card
- **Create**: `app/.../feature/search/components/SearchTabSelector.kt` — Use/Borrow/Split pill tabs
- **Create**: `app/.../feature/search/components/DatePickerSheet.kt` — Bottom sheet date range picker
- **Create**: `app/.../feature/search/components/LocationPickerField.kt` — WHERE field with "Use my location" button (uses `core/location/LocationService.kt`)
- **Modify**: `feature/search/.../SearchScreen.kt` — Replace single search bar with `EnhancedSearchBar`
- **Modify**: `feature/home/.../DashboardScreen.kt` — Add `SearchTabSelector` to home search card
- **Modify**: `app/.../feature/search/SearchViewModel.kt` — Add `SearchFilters` state, connect tab selector

#### State Management
- Extend `SearchViewModel` with `_filters: MutableStateFlow<SearchFilters>`
- Debounce all filter changes (400ms already exists for query)
- Persist selected tab across navigation

#### Commits
1. `feat(search): add SearchFilters model and SearchTab enum`
2. `feat(search): create EnhancedSearchBar with WHAT/WHERE/DATES fields`
3. `feat(search): create SearchTabSelector composable`
4. `feat(search): create DatePickerSheet bottom sheet`
5. `feat(search): create LocationPickerField with GPS support`
6. `feat(search): integrate enhanced search into SearchScreen`
7. `feat(home): add search tab selector to home dashboard`

---

### PR 2: Notification History Screen (P0-3)

**Scope**: In-app notification list with read/unread state.

#### API Layer
- **Modify**: `app/.../` — Create `NotificationRepository.kt`
  ```kotlin
  class NotificationRepository @Inject constructor(private val apiClient: ApiClient) {
      suspend fun fetchNotifications(userId: String): Result<List<NotificationItem>>
      suspend fun markAsRead(notificationId: String): Result<Unit>
      suspend fun markAllAsRead(userId: String): Result<Unit>
  }
  ```
- Endpoints already defined in `ApiEndPoints.kt`: `GET_USER_NOTIFICATIONS`, `MARK_NOTIFICATION_READ`, `MARK_ALL_NOTIFICATIONS_READ`

#### Data/Models
- **Create**: `app/.../feature/notifications/model/NotificationItem.kt`
  ```kotlin
  data class NotificationItem(
      val id: String,
      val title: String,
      val body: String,
      val type: String, // booking, message, system
      val isRead: Boolean,
      val createdAt: String,
      val actionUrl: String? = null
  )
  ```

#### UI
- **Create**: `app/.../feature/notifications/NotificationListScreen.kt` — LazyColumn of notification items, "Mark all as read" action
- **Create**: `app/.../feature/notifications/NotificationListViewModel.kt`
- **Create**: `app/.../feature/notifications/components/NotificationItemCard.kt` — Card with read/unread visual distinction
- **Modify**: Navigation graph — Add `notifications` route
- **Modify**: `app/.../navigation/PaceDreamNavHost.kt` — Add route
- **Modify**: Home screen or top bar — Add notification bell icon with unread count badge

#### State Management
- ViewModel with `StateFlow<NotificationListState>` (Loading/Success/Error)
- Pull-to-refresh support
- Mark-as-read triggers local state update + API call

#### Commits
1. `feat(notifications): add NotificationItem model and NotificationRepository`
2. `feat(notifications): create NotificationListScreen and ViewModel`
3. `feat(notifications): add notification bell icon to home top bar`
4. `feat(navigation): add notifications route`

---

### PR 3: Host Create/Edit Listing (P0-4)

**Scope**: Multi-step form for hosts to create and edit listings.

#### API Layer
- Endpoints already exist in `HostApiService.kt`: `createListing()`, `updateListing()`
- **Modify**: `app/.../feature/host/data/HostRepository.kt` — Ensure create/update methods are fully wired

#### Data/Models
- **Create**: `app/.../feature/host/model/ListingFormState.kt`
  ```kotlin
  data class ListingFormState(
      val title: String = "",
      val description: String = "",
      val category: String = "",
      val photos: List<Uri> = emptyList(),
      val location: ListingLocation = ListingLocation(),
      val pricing: ListingPricing = ListingPricing(),
      val amenities: List<String> = emptyList(),
      val rules: List<String> = emptyList(),
      val cancellationPolicy: String = "flexible",
      val currentStep: Int = 0
  )
  ```

#### UI
- **Create**: `app/.../feature/host/presentation/CreateListingScreen.kt` — Multi-step form with HorizontalPager or step indicator
- **Create**: `app/.../feature/host/presentation/CreateListingViewModel.kt`
- **Create**: Step composables:
  - `ListingBasicInfoStep.kt` — Title, description, category
  - `ListingPhotosStep.kt` — Photo picker + reorder
  - `ListingLocationStep.kt` — Address input + map
  - `ListingPricingStep.kt` — Hourly/nightly pricing, fees
  - `ListingAmenitiesStep.kt` — Amenity checkbox grid
  - `ListingRulesStep.kt` — House rules
  - `ListingReviewStep.kt` — Preview before submit
- **Modify**: `app/.../feature/host/presentation/HostListingsScreen.kt` — Add "Create Listing" FAB
- **Modify**: Navigation graph — Add `host/listings/create` and `host/listings/{id}/edit` routes

#### State Management
- `CreateListingViewModel` with `MutableStateFlow<ListingFormState>`
- Step validation before advancing
- Draft saving (optional — DataStore)
- For edit: pre-populate form from existing listing data

#### Commits
1. `feat(host): add ListingFormState model`
2. `feat(host): create listing form step composables`
3. `feat(host): create CreateListingScreen with step navigation`
4. `feat(host): create CreateListingViewModel with validation`
5. `feat(host): add edit listing pre-population`
6. `feat(host): add Create Listing FAB and navigation routes`

---

## Phase 2: P1 Fixes (Important)

### PR 4: Hero Section Alignment (P1-1)

#### Changes
- **Modify**: `feature/home/.../HomeFeedScreen.kt`
  - Update headline: `"Share it. Borrow it. Split it."` → `"One place to share it all"`
  - Update subtitle to match web exactly
  - Add "Get to know PaceDream" CTA button (purple gradient + play icon)
  - Replace placeholder gradient with hero background image
- **Add**: Hero background image to `app/src/main/res/drawable/`

#### Commits
1. `fix(home): update hero section text to match web/iOS`
2. `feat(home): add "Get to know PaceDream" CTA button`
3. `feat(home): add hero background image`

---

### PR 5: Complete Category Filters (P1-2)

#### Changes
- **Modify**: `feature/home/.../components/CategoryFilter.kt`
  - Add: Study Room, Short Stay, Apartment, Luxury Room
  - Reorder to match web: Restroom, Nap Pod, Meeting Room, Study Room, Short Stay, Apartment, Luxury Room, Parking, Storage Space
- **Add**: Category icons to `common/.../icons/`

#### Commits
1. `feat(home): add missing category filters (Study Room, Short Stay, Apartment, Luxury Room)`
2. `feat(home): reorder categories to match web`

---

### PR 6: FAQ Expandable UI (P1-3)

#### Changes
- **Modify**: `app/.../FAQScreen.kt` — Implement expandable accordion with `AnimatedVisibility`
- **Create** (if needed): `app/.../feature/faq/FaqItem.kt` — Individual expandable FAQ item composable
- Animation: 200ms easeInOut expand/collapse

#### Commits
1. `feat(faq): implement expandable FAQ items with animation`

---

### PR 7: Support Screen (P1-4)

#### Changes
- **Create**: `app/.../feature/support/SupportScreen.kt` — "Need Help?" heading, "Message Support" button, "Send Email" button
- **Modify**: Navigation graph — Add `support` route
- **Modify**: Profile menu or Settings — Add link to Support screen

#### Commits
1. `feat(support): create dedicated support screen`
2. `feat(navigation): add support route and profile menu link`

---

### PR 8: Booking Availability Check (P1-6)

#### Changes
- **Modify**: `app/.../feature/checkout/CheckoutViewModel.kt` — Call `GET /v1/bookings/availability/{propertyId}` before creating booking
- **Create**: `app/.../feature/checkout/AvailabilityChecker.kt` — Utility to check date/time availability
- **Modify**: `CheckoutScreen.kt` — Show availability status, disable "Reserve" if unavailable

#### Commits
1. `feat(checkout): add booking availability check before reservation`
2. `feat(checkout): show availability status in UI`

---

### PR 9: Inbox Polling (P1-7)

#### Changes
- **Modify**: `app/.../feature/inbox/InboxViewModel.kt` — Add 60s polling timer for thread list refresh
- **Modify**: `app/.../feature/inbox/ThreadViewModel.kt` — Add 15s polling timer for new messages in open thread
- Use `viewModelScope.launch` with `delay()` loop, cancel on `onCleared()`

#### Commits
1. `feat(inbox): add polling for new messages (60s inbox, 15s thread)`

---

### PR 10: Standardize Error Banners (P1-8)

#### Changes
- **Create**: `common/.../components/ErrorBanner.kt` — Shared composable: padding 12dp, radius 14dp, error bg @ 10% alpha
- **Modify**: All screens using Snackbar/Toast for errors → Replace with `ErrorBanner`
- **Add**: Error message sanitization (strip "Server error 200:" prefix)
- Key files to update: `AuthFlowSheetViewModel.kt`, `BookingFormScreen.kt`, `SearchScreen.kt`, `CheckoutScreen.kt`

#### Commits
1. `feat(ui): create shared ErrorBanner composable`
2. `refactor(auth): replace Snackbar with ErrorBanner`
3. `refactor: replace all remaining Snackbar/Toast with ErrorBanner`

---

## Phase 3: P1 Continued — Review Edit/Delete (P1-5)

### PR 11: Review Edit/Delete

#### Changes (dependent on backend confirmation)
- **Modify**: `app/.../feature/listingdetail/ReviewRepository.kt` — Add `editReview(reviewId, updatedReview)` and `deleteReview(reviewId)` if backend supports `PUT /v1/reviews/{id}` and `DELETE /v1/reviews/{id}`
- **Modify**: Review display in `ListingDetailScreen.kt` — Add edit/delete menu for user's own reviews
- **Modify**: `ListingDetailViewModel.kt` — Add edit/delete actions with confirmation dialog

#### Commits
1. `feat(reviews): add edit and delete review functionality`

---

## Summary Table

| PR | Priority | Scope | Key Files | Est. Size |
|----|----------|-------|-----------|-----------|
| PR 1 | P0 | Enhanced Search | SearchScreen, EnhancedSearchBar, DashboardScreen | Large |
| PR 2 | P0 | Notification History | NotificationListScreen (new), NotificationRepository (new) | Medium |
| PR 3 | P0 | Host Create/Edit Listing | CreateListingScreen (new), 7 step composables (new) | Large |
| PR 4 | P1 | Hero Section | HomeFeedScreen | Small |
| PR 5 | P1 | Category Filters | CategoryFilter | Small |
| PR 6 | P1 | FAQ Expandable | FAQScreen | Small |
| PR 7 | P1 | Support Screen | SupportScreen (new) | Small |
| PR 8 | P1 | Availability Check | CheckoutViewModel | Small |
| PR 9 | P1 | Inbox Polling | InboxViewModel, ThreadViewModel | Small |
| PR 10 | P1 | Error Banners | ErrorBanner (new), multiple screens | Medium |
| PR 11 | P1 | Review Edit/Delete | ReviewRepository, ListingDetailScreen | Small |

---

## iOS Implementation Plan

**Status**: Cannot be created — no iOS codebase access.

**What I need to proceed**:
- iOS repo URL (GitHub link) or local path
- Once provided, I will:
  1. Scan the full Swift/SwiftUI codebase
  2. Create an iOS Feature Map in the same format as Android above
  3. Create an iOS-specific Gap Report
  4. Create an iOS Implementation Plan with file-level actions

---

## Appendix: Android Architecture Reference

```
Navigation: Jetpack Compose Navigation (type-safe routes)
DI: Hilt (Dagger)
State: ViewModel + StateFlow
Networking: ApiClient (OkHttp-based, custom) + Retrofit (legacy layer)
Database: Room (SQLite)
Token Storage: EncryptedSharedPreferences
Auth: Auth0 Android SDK
Payments: Stripe (Chrome Custom Tabs)
Push: Firebase Cloud Messaging
Images: Coil
Serialization: Kotlinx Serialization (tolerant)
```

### Bottom Navigation Tabs
| Tab | Route | Screen |
|-----|-------|--------|
| Home | `home` | HomeScreen |
| Search | `search` | SearchScreen |
| Favorites | `favorites` | WishlistScreen |
| Inbox | `inbox` | InboxScreen |
| Profile | `profile` | ProfileScreen |

### API Base URLs
- **Backend**: `https://pacedream-backend.onrender.com/v1`
- **Frontend Proxy**: `https://www.pacedream.com/api/proxy/...`
- **Auth0**: `dev-pacedream.us.auth0.com`
