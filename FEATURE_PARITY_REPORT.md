# PaceDream Feature Parity Report

**Date**: 2026-02-24
**Platforms Analyzed**: Web (pacedream.com), Android (`chaejimmy/pixel`), iOS (`chaejimmy/studious-happiness`)
**Report Version**: 2.0

---

## Important Notes

- **Web repo** (`chaejimmy/pacedream-platform`) returns 404 — not accessible. Web features are inferred from: Android proxy endpoints that call the web frontend, the `UI_UX_COMPARISON.md` doc (which was written by comparing pacedream.com against the Android app), and backend API endpoint contracts.
- **iOS repo** found at `chaejimmy/studious-happiness` (the `bug-free-goggles` directory). Cloned and fully analyzed (393 Swift files).
- **CRITICAL FINDING**: The iOS app is an **earlier-generation "Totel" codebase** with a DIFFERENT backend (`https://utotel.herokuapp.com/v1`), DIFFERENT API endpoints (`user/*` prefix vs `/v1/*` RESTful), DIFFERENT auth flow (phone+OTP + Google Sign-In, NOT Auth0), and largely **hardcoded mock data** for messaging, notifications, and wishlist. The Android app is the **more mature platform** that has already been migrated to the PaceDream backend, Auth0, Stripe checkout, and live API integration.
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

> Source: Full codebase analysis of `chaejimmy/studious-happiness` → `bug-free-goggles/` directory (393 Swift files).
> **App Name in Code**: "Totel" (predecessor to PaceDream)
> **Base URL**: `https://utotel.herokuapp.com/v1` (DIFFERENT from Android/Web)
> **Architecture**: SwiftUI + MVVM with ObservableObject singletons

## C1. Authentication

| Item | Detail |
|------|--------|
| **Screen** | `SignInView` (phone-first), `NotLoggedInView` (email/phone toggle), `VerificationView` (OTP), `SetPasswordView`, `BasicSetUpView` (4-step setup wizard) |
| **User Actions** | Phone+OTP signup, email/password login, phone/password login, Google Sign-In (via GIDSignIn SDK) |
| **API Endpoints** | `POST auth/sendotp`, `POST auth/verify-otp`, `POST auth/signup`, `POST auth/login`, `POST auth/google` (defined but NEVER called) |
| **Auth Required** | No |
| **Key UX** | Phone-first flow ("Let's start with phone"), country picker, 6-digit OTP entry, email/phone toggle. **OTP verification is BYPASSED** (commented out). Google token obtained but never sent to backend. |
| **Token Storage** | `UserDefaults` (NOT Keychain — insecure) |
| **Missing vs Android** | No Auth0, no Apple Sign-In, no token refresh, no 401 handling, no EncryptedSharedPreferences equivalent |
| **Files** | `Views/Onboarding/SignInView.swift`, `Views/Main/Home/NotLoggedInView.swift`, `Views/Onboarding/VerificationView.swift`, `Views/Onboarding/SetPasswordView.swift`, `Views/Onboarding/BasicSetup/BasicSetUpView.swift`, `Views/Onboarding/UserAuthService.swift`, `Services/AuthNetworkService.swift` |

## C2. Home Feed

| Item | Detail |
|------|--------|
| **Screen** | `HomeView` |
| **User Actions** | Search ("Where to?"), browse categories (13 BookedType), view hourly rooms, find roommates |
| **API Endpoints** | `GET /hotel` (all hosts), `GET user/get_all_bookings`, `GET /user/get_all_rooms`, `GET user/get_all_roomates` |
| **Auth Required** | No |
| **Key UX** | Single search bar "Where to?" / "Anywhere • Any week • Add guests", horizontal category scroll with 13 types, filter sheet opens SearchView |
| **Missing vs Web** | NO hero section ("One place to share it all"), NO "Get to know PaceDream" CTA, NO Use/Borrow/Split tabs |
| **Files** | `Views/Main/Home/HomeView.swift`, `ViewModels/HomeViewModel.swift` |

## C3. Search

| Item | Detail |
|------|--------|
| **Screen** | `SearchView` (modal sheet), `SearchResultView` (inline) |
| **User Actions** | Switch Stay/Hourly/Find Roommate tabs, enter Where (destinations), When (date range), Who (adults/children/infants) |
| **API Endpoints** | None — search UI exists but no search API call implemented |
| **Key UX** | 3 collapsible sections (Where/When/Who), SearchType tabs (Stay, Hourly, Find Roommate — NOT Use/Borrow/Split), date range picker, guest counter, popular destinations list, "Search" button with magnifying glass |
| **Missing vs Android** | No autocomplete, no debounce, no API integration, different tab names |
| **Files** | `Views/Main/Home/SearchView.swift`, `Views/Main/Home/SearchResultView.swift`, `ViewModels/SearchViewModel.swift`, `Models/SearchType.swift` |

## C4. Listing Detail / Hourly Booking

| Item | Detail |
|------|--------|
| **Screen** | `HourlyBookingView`, `BookedDetailsView`, `HotelBookingDetailScreen`, `CleaningClearingView` |
| **User Actions** | View room details, select hour/day duration, view on map, book |
| **API Endpoints** | `POST user/add_booking` (implied) |
| **Key UX** | Hour/day toggle, duration selector, rent rate display, "Drops" toast for success notification, room location map |
| **Files** | `Views/Main/Home/HourlyRoom/HourlyBookingView.swift`, `Views/Main/Home/BookingDetail/BookedDetailsView.swift`, `Views/Main/HotelBookingDetail/HotelBookingDetailScreen.swift` |

## C5. Reviews

| Item | Detail |
|------|--------|
| **Screen** | `ReviewItemView`, `ReviewUploadView`, `ReviewView` (host), `ReviewCard` (host) |
| **User Actions** | View reviews, add review, update review, soft/permanent delete review |
| **API Endpoints** | `GET user/get-reviews-ratings`, `POST user/add_review`, `PUT user/update_review`, `PUT user/soft_del_review`, `DELETE user/perm_del_review` |
| **Key UX** | iOS has **full review CRUD** including update and both soft/permanent delete — Android is missing edit/delete |
| **Files** | `Services/ReviewNetworkService.swift`, `ViewModels/ReviewViewModel.swift`, `Views/Main/Profile/ReviewItemView.swift` |

## C6. Bookings

| Item | Detail |
|------|--------|
| **Screen** | `BookingView` (list), `BookedDetailsView`, `ReScheduleView`, `NewTimeReschedulingView` |
| **User Actions** | View all bookings, view detail, reschedule booking (time picker) |
| **API Endpoints** | `GET user/get_all_bookings`, `GET user/get_user_bookings`, `POST user/add_booking`, `PUT user/update_booking`, `DELETE user/delete_booking` |
| **Key UX** | Booking list with cards, reschedule with time picker. **No Stripe checkout** — no payment integration at all |
| **Missing vs Android** | No Stripe, no deep links for booking-success/cancelled, no session persistence |
| **Files** | `Views/Main/Bookings/BookingView.swift`, `Views/Main/Home/BookingDetail/BookedDetailsView.swift`, `ViewModels/BookingsViewModel.swift`, `Services/BookingNetworkService.swift` |

## C7. Messaging / Inbox

| Item | Detail |
|------|--------|
| **Screen** | `InboxView` (segmented: Notifications/Messages), `MessagesScreen`, `ChatView`, `NotificationView` |
| **User Actions** | View notifications, view messages, chat |
| **API Endpoints** | NONE wired — **ALL data is hardcoded mock** |
| **Key UX** | Segmented picker (Notifications/Messages), notification items grouped by date (Today/This Week/Earlier), message thread list with unread badges, chat bubbles. All static data. |
| **Missing vs Android** | No API integration whatsoever. Android has full REST inbox with threads, send, archive, unread counts. |
| **Files** | `Views/Main/Inbox/InboxView.swift`, `Views/Main/Inbox/Messages/MessagesScreen.swift`, `Views/Main/Inbox/Messages/ChatView.swift`, `Views/Main/Inbox/Notification/NotificationView.swift`, `ViewModels/InboxViewModel.swift` |

## C8. Wishlist

| Item | Detail |
|------|--------|
| **Screen** | `WishListView` |
| **User Actions** | View wishlist items in grid |
| **API Endpoints** | `GET user/get_wishlist`, `POST user/add-wishlist`, `POST user/delete-wishlist` (defined but WishListView uses **hardcoded mock data**) |
| **Key UX** | Grid layout with ListingCardView, "No wishlists yet" empty state. WishlistNetworkService exists and is wired to API but the view uses static `Home` array. |
| **Missing vs Android** | No optimistic remove, no filter tabs, no type-based routing, view uses mock data |
| **Files** | `Views/Main/Bookings/WishList/WishListView.swift`, `Services/WishlistNetworkService.swift` |

## C9. Notifications (In-App List)

| Item | Detail |
|------|--------|
| **Screen** | `NotificationView` (inside Inbox tab), `NotificationItemView` |
| **User Actions** | View categorized notifications (Today/This Week/Month/Earlier) |
| **API Endpoints** | `GET user/get_user_notifications` (defined but view uses **mock data from ViewModel**) |
| **Key UX** | Notification items grouped by time period, tap to navigate to detail. iOS HAS an in-app notification list UI — Android does NOT. |
| **Files** | `Views/Main/Inbox/Notification/NotificationView.swift`, `Views/Main/Inbox/Notification/NotificationItemView.swift`, `ViewModels/InboxViewModel.swift` |

## C10. Host Dashboard

| Item | Detail |
|------|--------|
| **Screen** | `HostSettingView`, `HostTab`, `HostingTabView` |
| **User Actions** | Switch to host mode, manage bookings, manage inbox, manage space, view business/earnings, post space, logout |
| **API Endpoints** | None specific to host — reuses same booking/room endpoints |
| **Key UX** | Guest/Host mode toggle (via `AppState.hostMode`), host sections: Bookings, Inbox, Space, Business. "Post your space" button opens full-screen `PostHostingView`. |
| **Files** | `Views/HostSettings/HostSettingView.swift`, `App/Tabs/HostTab.swift`, `Views/HostingMode/HostingTabView.swift`, `ViewModels/AppState.swift` |

## C11. Listing Creation (Host)

| Item | Detail |
|------|--------|
| **Screen** | Multi-step hosting flow: `GiveATitleView` → `UploadImageView` → `HostAddressSelectionView` → `AddressDescribeView` → `AmenitiesDetailView` → `SharableRoomDetailsView` → `GiveYourAvailability` → `GiveYourPricingView` → `SelectCheckoutOffers` |
| **User Actions** | Title, photos, address (map), describe, amenities, room details, availability, pricing, early/late checkout |
| **API Endpoints** | `POST /v1/add_room` (implied, via RoomEndpoint) |
| **Key UX** | Full multi-step wizard with map-based address selection, amenity grid, stepper for room details (beds, baths, guests), day/hour availability picker, pricing per day. **iOS has a COMPLETE listing creation flow — Android is missing this.** |
| **Files** | `Views/Hosting/GiveATitle/GiveATitleView.swift`, `Views/Hosting/UploadImages/UploadImageView.swift`, `Views/Hosting/AddressSelection/HostAddressSelectionView.swift`, `Views/Hosting/AmenitiesDetails/AmenitiesDetailView.swift`, `Views/Hosting/SharableRoomDetails/SharableRoomDetailsView.swift`, `Views/Hosting/GiveYourAvailability/GiveYourAvailability.swift`, `Views/Hosting/GiveYourPricingView/GiveYourPricingView.swift`, `Views/Hosting/HostingViewModel.swift` |

## C12. Settings

| Item | Detail |
|------|--------|
| **Screen** | `SettingView`, `ProfileSettingView`, `NotificationSettingView`, `SecuritySettingView`, `ChangePasswordView`, `EditProfileView` |
| **User Actions** | Edit profile, payment info, notification prefs, security, currency, help, version, terms, privacy, switch to host mode, sign out |
| **Settings Items** | My Account (Profile, Payment Information, Notification, Security, Currency), Information (Help, Version, Terms of Service, Privacy Policy) |
| **Key UX** | Switch host mode button, version update sheet ("Using Now V 1.2.3.0"), sign out confirmation sheet. **Sign out does NOT clear tokens or session state — this is a bug.** |
| **Files** | `Views/Main/Profile/Setting/SettingView.swift`, `Views/Main/Profile/Setting/ProfileSettingView.swift`, `Views/Main/Profile/Setting/NotificationSettingView.swift` |

## C13. Roommate Finder

| Item | Detail |
|------|--------|
| **Screen** | `RoommateListView`, `PersonPagesView`, `PersonCard`, `DestinationView`, `LocationView`, `LookForView`, `WalletView` |
| **User Actions** | Browse roommate listings, view person card carousel, find roommates by destination/location |
| **API Endpoints** | `GET user/get_all_roomates`, `POST user/add_roomate` |
| **Files** | `Views/FindRoommate/*.swift`, `Views/Main/Home/FindRoommate/RoommateListView.swift` |

## C14. Onboarding

| Item | Detail |
|------|--------|
| **Screen** | `OnboardingView` (3-page carousel), `BasicSetUpView` (4-step wizard) |
| **User Actions** | Skip onboarding, complete 4-step profile setup (Name → DOB/Gender → Photo → Hobbies) |
| **Key UX** | 3 onboarding slides ("Shared living space", "Find places around you"), skip button, 11 hobby options for profile, gender selection (Male/Female/Prefer not to say) |
| **Files** | `Views/Onboarding/OnboardingView.swift`, `Views/Onboarding/BasicSetup/BasicSetUpView.swift`, `ViewModels/OnboardingViewModel.swift`, `ViewModels/BasicSetUpViewModel.swift` |

## C15. Design System

| Item | Detail |
|------|--------|
| **Primary Color** | `appPrimary` = `#0057FF` (BLUE — NOT purple like PaceDream's `#5527D7`) |
| **Accent Color** | `darkIndigo` = `#574EFA` |
| **Error Color** | `appRed` = `#FD5E49` |
| **Success Color** | `darkGreen` = `#2D8A39` |
| **Grays** | primaryGray `#787880`, secondaryGray `#3C3C43`, darkGray `#5F6D7E`, gray700 `#272D37` |
| **Surfaces** | darkWhite `#F2F2F7`, darkWhite1 `#F5F8FE`, darkWhite2 `#F7F7F8` |
| **Custom Fonts** | Circular Std (Black, Bold, Book, Light, Medium), Inter |
| **Corner Radii** | 10dp (search), 12dp (cards, inputs), 14dp (search button), 20dp (settings card), 25-30dp (capsule buttons) |
| **Files** | `Extensions/Colors.swift`, `Extensions/Font+Inter.swift` |

## C16. Navigation

| Item | Detail |
|------|--------|
| **Tab Bar (logged out)** | Home, Profile |
| **Tab Bar (logged in)** | Home, Bookings, Inbox, Profile |
| **Tab Bar (host mode)** | Home, Bookings, Host, Inbox, Profile |
| **Router** | `RouterPath` with `Route` enum, `NavigationStack`-based |
| **Files** | `App/Tabs.swift`, `Views/Navigation/Route.swift`, `App/Router.swift`, `App/AppRouter.swift` |

## C17. Networking Layer

| Item | Detail |
|------|--------|
| **HTTP Client** | `NetworkUtility` singleton wrapping `URLSession.shared` |
| **Retry Logic** | NONE |
| **HTML Hardening** | NONE |
| **Token Refresh** | NONE (no 401 handling) |
| **Request Deduplication** | NONE |
| **Auth Interceptor** | NONE — token is stored but NEVER sent on API requests |
| **Timeout Config** | Default URLSession (no customization) |
| **Files** | `Utilities/NetworkUtility.swift`, `Models/Networking/Endpoints/Endpoint.swift` |

## iOS Critical Bugs Found

1. **Bearer token never sent** — token stored after login but no service method passes it in Headers
2. **OTP verification bypassed** — `vm.verifyOtp()` commented out, directly sets `isLoggedIn = true`
3. **Sign-out doesn't clear session** — navigates away but never resets `isLoggedIn` or clears stored tokens
4. **Google Sign-In token discarded** — `auth/google` endpoint exists but token is never sent to backend
5. **RoomEndpoint path bugs** — leading `/v1/` or `/` causes double-prefix URLs (`/v1/v1/add_room`)
6. **Tokens in UserDefaults** — insecure storage, should use Keychain

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

# D-iOS) GAP REPORT — iOS vs Android / Web

> The iOS app ("Totel" at `chaejimmy/studious-happiness`) is a fundamentally **earlier generation** than the Android app. The gaps below are ordered by severity.

## iOS-P0: Architecture-Level Gaps (Must Migrate Before Feature Work)

### iOS-P0-1. Backend Migration (Totel → PaceDream)

| Field | Detail |
|-------|--------|
| **Gap Type** | Infrastructure — different backend entirely |
| **iOS** | Base URL: `https://utotel.herokuapp.com/v1` (Heroku) |
| **Android/Web** | Base URL: `https://pacedream-backend.onrender.com/v1` (Render) |
| **Impact** | iOS is talking to a completely different backend. All data is isolated. |
| **Fix** | Change `Endpoint.baseURL` to `https://pacedream-backend.onrender.com/v1`, migrate all endpoint paths to new `/v1/*` RESTful format |

### iOS-P0-2. API Contract Migration (user/* → /v1/*)

| Field | Detail |
|-------|--------|
| **Gap Type** | All endpoint paths differ |
| **iOS Endpoints** | `user/get_all_bookings`, `user/add_booking`, `user/get_wishlist`, `user/add-wishlist`, `user/get-reviews-ratings`, `auth/login`, `auth/signup`, `auth/sendotp` |
| **Android Endpoints** | `/v1/bookings/mine`, `/v1/properties/bookings/timebased`, `/v1/account/wishlist`, `/v1/account/wishlist/toggle`, `/v1/reviews/property/{id}`, `/v1/auth/login/email`, `/v1/auth/signup/email`, `/v1/auth/auth0/callback` |
| **Impact** | Every single API call in iOS points to the wrong endpoint |
| **Fix** | Rewrite all Endpoint enums to match Android/Web contract. Estimated: 8 endpoint files, ~40 endpoint cases. |

### iOS-P0-3. Auth System Migration (Phone+OTP → Auth0)

| Field | Detail |
|-------|--------|
| **Gap Type** | Completely different auth architecture |
| **iOS** | Phone+OTP (OTP currently BYPASSED), email/password, Google Sign-In via GIDSignIn SDK (token NEVER sent to backend) |
| **Android** | Auth0 Universal Login (Google, Apple), email/password, AuthFlowSheet chooser modal |
| **Impact** | iOS auth is non-functional: OTP bypassed, Google token discarded, no Apple Sign-In |
| **Fix** | Replace GIDSignIn with Auth0.swift SDK, implement AuthFlowSheet equivalent, add Apple Sign-In, replace phone+OTP with Auth0 hosted login |

### iOS-P0-4. Networking Layer Overhaul

| Field | Detail |
|-------|--------|
| **Gap Type** | Missing critical networking features |
| **iOS** | Basic `URLSession.shared` wrapper, no retry, no 401 handling, no HTML hardening, no auth interceptor, **Bearer token never sent on requests** |
| **Android** | Full OkHttp pipeline: auth interceptor, 401→refresh→retry, GET-only retry (2x, 0.4s/0.8s backoff), HTML hardening, request dedup, 30s/60s timeouts |
| **Impact** | iOS will break on any auth-required endpoint, cannot recover from token expiry, vulnerable to HTML error responses |
| **Fix** | Implement auth interceptor that reads stored token, add 401→refresh→retry, add retry for GET, add HTML detection, add request dedup |

### iOS-P0-5. Token Security

| Field | Detail |
|-------|--------|
| **Gap Type** | Insecure token storage |
| **iOS** | Tokens stored in `UserDefaults` (unencrypted, accessible via device backup) |
| **Android** | `EncryptedSharedPreferences` with MasterKey |
| **Fix** | Migrate to Keychain with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` |

## iOS-P1: Feature Gaps (iOS Missing Features Android Has)

### iOS-P1-1. Stripe Checkout

| Field | Detail |
|-------|--------|
| **iOS** | NO payment integration at all |
| **Android** | Full Stripe checkout via Chrome Custom Tabs, deep links for success/cancel, session persistence |
| **Fix** | Implement Stripe checkout via SFSafariViewController, add Universal Links for booking-success/cancelled |

### iOS-P1-2. Live Messaging/Inbox

| Field | Detail |
|-------|--------|
| **iOS** | ALL inbox/message data is hardcoded mock (static arrays in ViewModel) |
| **Android** | Full REST integration: threads, messages, send, archive, unread counts, cursor pagination |
| **Fix** | Wire InboxViewModel to new `/v1/inbox/*` endpoints, replace mock data |

### iOS-P1-3. Collections

| Field | Detail |
|-------|--------|
| **iOS** | Not implemented at all |
| **Android** | Full CRUD: create collection, add/remove items, public/private toggle |
| **Fix** | Create CollectionsView, CollectionsViewModel, wire to `/v1/collections/*` endpoints |

### iOS-P1-4. Identity Verification

| Field | Detail |
|-------|--------|
| **iOS** | Not implemented |
| **Android** | Phone OTP verification, ID upload, multi-step verification flow |
| **Fix** | Create verification flow views, wire to `/v1/users/verification/*` endpoints |

### iOS-P1-5. Payment Methods Management

| Field | Detail |
|-------|--------|
| **iOS** | "Payment Information" exists as settings menu item but no implementation |
| **Android** | Full Stripe SetupIntent flow: view cards, add new, set default, delete |
| **Fix** | Create PaymentMethodsView, wire to `www.pacedream.com/api/proxy/account/payment-methods/*` |

### iOS-P1-6. Push Notifications (FCM/APNs)

| Field | Detail |
|-------|--------|
| **iOS** | No push notification integration (no Firebase, no APNs setup) |
| **Android** | Full FCM integration with device registration, notification preferences |
| **Fix** | Add Firebase/APNs, implement device registration via `/v1/notifications/register-device` |

### iOS-P1-7. Deep Links

| Field | Detail |
|-------|--------|
| **iOS** | No Universal Links / deep link handling |
| **Android** | Handles `booking-success`, `booking-cancelled` deep links |
| **Fix** | Add Universal Links for PaceDream domain, handle booking flow callbacks |

### iOS-P1-8. Sign-Out Bug

| Field | Detail |
|-------|--------|
| **iOS** | Sign-out navigates away but DOES NOT clear `isLoggedIn`, tokens, or call backend logout |
| **Android** | Proper sign-out: clears tokens, resets auth state |
| **Fix** | Clear `@AppStorage("isLoggedIn")`, remove `UserAuth` from UserDefaults, call `GIDSignIn.sharedInstance.signOut()` |

## iOS-P2: UI/UX Alignment Gaps

### iOS-P2-1. Branding (Totel → PaceDream)

| Field | Detail |
|-------|--------|
| **iOS** | App still references "Totel" in Terms text, uses blue primary `#0057FF` |
| **Android/Web** | PaceDream branding, purple primary `#5527D7` |
| **Fix** | Update all "Totel" references, change primary color to `#5527D7`, update app icon and assets |

### iOS-P2-2. Category System

| Field | Detail |
|-------|--------|
| **iOS** | 13 categories: Hourly Room, Travel Roommate, Event Spaces, Parking, Storage, Education, Business, Electric Charging, Pet Care, Daytime Room, Personal Fitness, Gaming Arcades, Nighttime Room |
| **Android/Web** | 9 categories: Restroom, Nap Pod, Meeting Room, Study Room, Short Stay, Apartment, Luxury Room, Parking, Storage Space |
| **Fix** | Replace BookedType enum with PaceDream categories |

### iOS-P2-3. Search Tab Naming

| Field | Detail |
|-------|--------|
| **iOS** | Stay / Hourly / Find Roommate |
| **Web** | Use / Borrow / Split |
| **Fix** | Rename SearchType to match web naming |

### iOS-P2-4. Auth UI (Phone-First → Chooser Sheet)

| Field | Detail |
|-------|--------|
| **iOS** | Phone number entry as primary auth, full-screen navigation |
| **Android** | AuthFlowSheet modal: Chooser → SignIn/SignUp with Sign in, Create account, Google, Apple, Not now |
| **Fix** | Replace SignInView with AuthFlowSheet equivalent |

## iOS Feature Parity Summary

| Feature | iOS Status | Android Status | Gap Severity |
|---------|-----------|---------------|-------------|
| Auth (Auth0) | Missing | Complete | P0 |
| Backend (PaceDream) | Wrong backend | Complete | P0 |
| Networking (retry/refresh) | Missing | Complete | P0 |
| Token Security | UserDefaults | Encrypted | P0 |
| Stripe Checkout | Missing | Complete | P1 |
| Live Messaging | Mock data | Complete | P1 |
| Collections | Missing | Complete | P1 |
| Verification | Missing | Complete | P1 |
| Payment Methods | Missing | Complete | P1 |
| Push Notifications | Missing | Complete | P1 |
| Deep Links | Missing | Complete | P1 |
| Listing Creation | **EXISTS** | Missing | iOS ahead |
| Review Edit/Delete | **EXISTS** | Missing | iOS ahead |
| Notification List UI | **EXISTS** (mock) | Missing | iOS ahead |
| Roommate Finder | Complete | Complete | Parity |
| Host Mode Toggle | Complete | Complete | Parity |

---

# E) IMPLEMENTATION PLAN — P0 & P1 (Android)

> The plan below covers Android. For iOS implementation plan, see Section F.

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

## F) iOS IMPLEMENTATION PLAN

> Source: Full analysis of `chaejimmy/studious-happiness` (bug-free-goggles). The iOS app requires a ground-up migration from the legacy Totel backend to PaceDream.

### Phase 1: Infrastructure (Backend + Networking + Auth)

#### PR iOS-1: Backend Migration + Networking Overhaul

**Scope**: Switch base URL, rewrite all endpoints, add auth interceptor, retry, HTML hardening.

**Files to Modify**:
- `Models/Networking/Endpoints/Endpoint.swift` — Change `baseURL` to `https://pacedream-backend.onrender.com/v1`
- `Models/Networking/Endpoints/AuthEndpoint.swift` — Rewrite: `auth/login` → `auth/login/email`, `auth/signup` → `auth/signup/email`, add `auth/auth0/callback`, `auth/refresh-token`
- `Models/Networking/Endpoints/BookingEndpoint.swift` — Rewrite: `user/get_all_bookings` → `bookings/mine`, `user/add_booking` → `properties/bookings/timebased`, etc.
- `Models/Networking/Endpoints/WishlistEndpoint.swift` — Rewrite: `user/get_wishlist` → `account/wishlist`, `user/add-wishlist` → `account/wishlist/toggle`
- `Models/Networking/Endpoints/ReviewEndpoint.swift` — Rewrite: `user/get-reviews-ratings` → `reviews/property/{id}`, `user/add_review` → `reviews`
- `Models/Networking/Endpoints/RoomEndpoint.swift` — Rewrite all paths, fix leading `/` bugs
- `Models/Networking/Endpoints/NotificationEndpoint.swift` — Rewrite to `/v1/notifications/*`
- `Models/Networking/Endpoints/UserEndpoint.swift` — Rewrite to `/v1/account/*`
- `Utilities/NetworkUtility.swift` — Add: auth interceptor (read token from Keychain, add Bearer header), 401→refresh→retry, GET retry (2x, 0.4s/0.8s), HTML response detection, request timeout (30s request, 60s resource)

**Files to Create**:
- `Services/TokenManager.swift` — Keychain-based token storage (access + refresh), token validation (JWT 3-segment check)
- `Services/SessionManager.swift` — Bootstrap, refresh, sign-out logic. StateFlow equivalent via `@Published`

#### PR iOS-2: Auth0 Migration

**Scope**: Replace phone+OTP + GIDSignIn with Auth0 Universal Login.

**Files to Modify**:
- `Views/Onboarding/SignInView.swift` — Replace with AuthFlowSheet equivalent (Chooser/SignIn/SignUp modes)
- `Views/Onboarding/UserAuthService.swift` — Replace GIDSignIn with Auth0.swift SDK
- `Views/Main/Home/NotLoggedInView.swift` — Replace with AuthFlowSheet presentation
- `ViewModels/OnboardingViewModel.swift` — Rewrite auth state management, remove OTP logic

**Files to Create**:
- `Views/Auth/AuthFlowSheet.swift` — Modal sheet with Chooser → Sign In / Sign Up
- `Views/Auth/AuthViewModel.swift` — AuthUiState (mode, email, password, loading, error), drive Auth0 + email flows
- `Services/Auth0Service.swift` — Auth0 WebAuth for Google (`google-oauth2`) and Apple (`apple`) connections

**Files to Delete**:
- `Views/Onboarding/VerificationView.swift` (OTP is not used in PaceDream)
- `Models/Networking/Requests/SendOtpRequest.swift`
- `Models/Networking/Requests/VerifyOtpRequest.swift`

#### PR iOS-3: Token Security + Session Bootstrap

**Scope**: Migrate from UserDefaults to Keychain, fix sign-out.

**Files to Modify**:
- `Utilities/UserDefaultsCodable.swift` — Replace with KeychainCodable using Security framework
- `Views/Main/Profile/Setting/SignOutSheet.swift` — Actually clear `@AppStorage("isLoggedIn")`, clear Keychain tokens, call Auth0 logout
- `Views/ContentView.swift` — Add bootstrap: validate token on launch, fetch profile from `/v1/account/me`

### Phase 2: Feature Completion (Wire Mock Data to API)

#### PR iOS-4: Live Messaging/Inbox

**Scope**: Replace hardcoded mock data with API calls.

**Files to Modify**:
- `ViewModels/InboxViewModel.swift` — Replace static arrays with API calls to `/v1/inbox/threads`, `/v1/inbox/unread-counts`
- `Views/Main/Inbox/Messages/MessagesScreen.swift` — Wire to `/v1/inbox/threads/{id}/messages`
- `Views/Main/Inbox/Messages/ChatView.swift` — Add send message via `POST /v1/inbox/threads/{id}/messages`

**Files to Create**:
- `Services/InboxNetworkService.swift` — Thread list, messages, send, archive
- `Models/Networking/Endpoints/InboxEndpoint.swift` — All `/v1/inbox/*` endpoints

#### PR iOS-5: Live Wishlist

**Scope**: Replace mock Home array with API calls.

**Files to Modify**:
- `Views/Main/Bookings/WishList/WishListView.swift` — Wire to `GET /v1/account/wishlist`, add optimistic remove
- `Services/WishlistNetworkService.swift` — Update to new endpoints

#### PR iOS-6: Stripe Checkout + Deep Links

**Scope**: Add payment flow.

**Files to Create**:
- `Views/Checkout/CheckoutView.swift` — Open Stripe checkout URL in SFSafariViewController
- `Views/Checkout/BookingConfirmationView.swift` — Handle success callback
- `Services/CheckoutService.swift` — `POST /v1/properties/bookings/timebased`, `GET /v1/.../success/checkout?session_id=...`

**Files to Modify**:
- `Info.plist` — Add Universal Links for `www.pacedream.com`
- `App/bug_free_gogglesApp.swift` — Handle incoming deep links

### Phase 3: New Features

#### PR iOS-7: Collections, Verification, Payment Methods

**Files to Create**:
- `Views/Collections/CollectionsView.swift`, `CollectionDetailView.swift`
- `Views/Verification/PhoneVerificationView.swift`, `IDVerificationView.swift`
- `Views/Settings/PaymentMethodsView.swift`
- `Services/CollectionsNetworkService.swift`, `VerificationNetworkService.swift`, `PaymentMethodsNetworkService.swift`

### Phase 4: UI/UX Alignment

#### PR iOS-8: PaceDream Branding

**Files to Modify**:
- `Extensions/Colors.swift` — Change `appPrimary` from `#0057FF` (blue) to `#5527D7` (purple)
- `Models/BookedType.swift` — Replace 13 Totel categories with 9 PaceDream categories
- `Models/SearchType.swift` — Rename Stay/Hourly/FindRoommate → Use/Borrow/Split
- All "Totel" string references → "PaceDream"

### iOS Implementation Summary

| Phase | PRs | Scope | Priority |
|-------|-----|-------|----------|
| Phase 1 | iOS-1, iOS-2, iOS-3 | Infrastructure | P0 — Must complete first |
| Phase 2 | iOS-4, iOS-5, iOS-6 | Wire to live API | P0/P1 |
| Phase 3 | iOS-7 | New features | P1 |
| Phase 4 | iOS-8 | Branding/UI | P1 |

### Key Insight: iOS Has Features Android Lacks

While iOS is behind on infrastructure, it has three features Android should port:

1. **Listing Creation Flow** (multi-step wizard in `Views/Hosting/`) — Android's `HostApiService.kt` has endpoints but NO create/edit UI
2. **Review Edit/Delete** (full CRUD in `ReviewNetworkService.swift`) — Android only has create/view
3. **In-App Notification List** (`NotificationView.swift`) — Android has endpoints but no UI screen

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
