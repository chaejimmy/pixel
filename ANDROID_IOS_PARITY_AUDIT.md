# PaceDream Android vs iOS Parity Audit Report

**Date**: 2026-03-18
**iOS Benchmark**: `/home/user/iOS26` (SwiftUI, 5-tab Guest / 5-tab Host)
**Android App**: `/home/user/pixel` (Jetpack Compose, 6-tab bottom nav)
**Platform API**: `https://pacedream-backend.onrender.com/v1` (shared backend)

---

## Executive Summary

The Android app has a solid architectural foundation (MVVM + Clean Architecture, Jetpack Compose, Hilt DI, Retrofit, Room) and covers most core flows. However, **23 gaps** were identified across authentication, search, booking management, payments, messaging, and polish. The iOS app is notably ahead in search UX, booking lifecycle management, payment options, chat capabilities, and host earnings visibility.

**Breakdown**: 6 P0 (must fix) | 10 P1 (fix before launch) | 7 P2 (polish)

---

## Table of Contents

1. [Authentication & Onboarding](#1-authentication--onboarding)
2. [Home / Discovery](#2-home--discovery)
3. [Search](#3-search)
4. [Listing Detail](#4-listing-detail)
5. [Booking Flow](#5-booking-flow)
6. [Booking Management](#6-booking-management)
7. [Payment](#7-payment)
8. [Messaging / Chat](#8-messaging--chat)
9. [Notifications](#9-notifications)
10. [Profile](#10-profile)
11. [Settings](#11-settings)
12. [Reviews](#12-reviews)
13. [Wishlist / Favorites](#13-wishlist--favorites)
14. [Host Mode](#14-host-mode)
15. [Other Features](#15-other-features)
16. [Implementation Order](#16-recommended-implementation-order)
17. [External Dependencies](#17-external-dependencies)

---

## 1. Authentication & Onboarding

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Splash screen | 2s with gradient + logo | Not found | **GAP** |
| Onboarding carousel | 3-page TabView with skip/next | Auto-rotating carousel | Parity |
| Email/password login | Auth0 + direct login | Auth0 + direct login | Parity |
| Phone + OTP login | PhoneNumberKit E.164 validation, 6-digit OTP, country picker (8 countries), masked phone display, paste support, auto-submit on 6th digit, countdown timer | 6-digit OTP, 60s countdown, country picker | Parity |
| Google OAuth | Auth0 google-oauth2 | Auth0 google-oauth2 | Parity |
| Apple Sign-In | Auth0 apple connection | N/A | OK (not required on Android) |
| Facebook OAuth | Auth0 facebook connection (button present) | Not found | **GAP** |
| Terms acceptance checkbox | Required for email signup | Not verified | **GAP** |
| Forgot password | Link on login screen | ForgotPasswordScreen exists | Parity |
| Basic setup (name) | First/last name > 3 chars | First/last name validation | Parity |
| Basic setup (DOB/gender) | DatePicker sheet + gender chips | DatePicker + gender selection | Parity |
| Basic setup (profile photo) | Camera + gallery picker, 164x164 circle | Profile picture upload | Parity |
| Basic setup (hobbies) | Wrapping grid, max 5 selections | Hobbies selection | Parity |
| Profile completion redirect | `incomplete_profile` flag → BasicSetUp | Account creation multi-step | Parity |
| Error messages for OTP | Network-specific (500, 400, 429, 503, timeout, no connection) | Generic error display | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| AUTH-1 | Missing Facebook OAuth option | P2 | iOS has "Continue with Facebook" button via Auth0. Low priority since Google + email + phone cover most users. |
| AUTH-2 | OTP error messages not network-specific | P1 | iOS maps HTTP status codes to user-friendly messages (429→"Too many requests", 503→"Service temporarily unavailable"). Android shows generic errors. |
| AUTH-3 | No explicit terms acceptance checkbox on email signup | P1 | iOS requires explicit `termsAccepted` checkbox before signup. Android should have this for legal compliance. |

---

## 2. Home / Discovery

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Dashboard header + greeting | Hero header with profile button | DashboardHeader with greeting + search bar | Parity |
| Search bar (prominent) | ProminentSearchBar (56px, rounded 999px, shadow) | EnhancedSearchBar | Parity |
| Quick filter chips | 5 chips: Rooms, Parking, Gear, Experiences, Nearby (colored icons) | Quick category chips (horizontal) | Parity |
| Category cards | Category browse with icons + descriptions | CategoryCard components | Parity |
| Featured deals | Deal cards with image, price, location | DealsCard with image, price, location | Parity |
| Destinations | Destination cards | DestinationCard | Parity |
| Recent searches | Recent search display | RecentSearchCard | Parity |
| Rented gear section | Gear rental section | RentedGearModel section | Parity |
| Time-based deals | Hourly/daily/weekly/monthly pricing | Time-based deals | Parity |
| Split stays | Roommate finder integration | SplitStayModel section | Parity |
| Explore by Type | 5 types: Romantic, Adventure, Nature, Wildlife, Solo | Categories/explore by type section | Parity |
| Trending Destinations | Grand Canyon, Utah, Maui, etc. in grid | "Browse by Destination" (simpler) | **GAP** |
| Last-Minute Deals | Horizontal scroll with discount badges | LastMinuteDeals section exists | Parity |
| Three Steps CTA | "Create Listing → Receive Booking → Earn Income" | Not found | **GAP** |
| Not-logged-in CTA | Login/Signup buttons with social options | Locked state display | Partial |
| Pull-to-refresh | Yes | Yes | Parity |
| Loading skeletons | Shimmer placeholders | DealCardShimmer | Parity |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| HOME-1 | Missing Three Steps CTA section | P2 | iOS has a conversion-driving section encouraging hosting: "Create a Listing → Receive Booking → Earn Extra Income". Android should add this to drive host sign-ups. |
| HOME-2 | Trending Destinations section is simpler | P2 | iOS shows curated trending destinations in a grid format with specific names. Android has "Browse by Destination" but lacks the curated/trending feel. |

---

## 3. Search

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Tab-based search (Share/Borrow/Split) | Full implementation with tab picker | SearchTab enum with tab selection | Parity |
| Expandable search cards (What/Where/When/Who) | Airbnb-style collapsible cards with auto-suggestions, location permissions, date pickers, guest stepper | Not implemented | **GAP** |
| Basic text search | Query + suggestions | Query + clear button | Parity |
| Category filter chips | Type-based filtering | Category pill chips | Parity |
| Search results grid | LazyVGrid 2-column | 2-column grid | Parity |
| Result cards | Image, rating badge, title, location, price | Image, title, price, location, rating | Parity |
| Advanced filters sheet | Price range, amenities (6), availability, sort | Not found | **GAP** |
| Property type filter | 10 types (Apartment, House, Studio, etc.) | Not found | **GAP** |
| Booking options filter | Instant Book, Pet Friendly, Wheelchair Accessible | Not found | **GAP** |
| Distance/location filter | Slider (1-200 km) with presets | Not found | **GAP** |
| Sort options | Relevance, Price Low/High, Rating | Not found | **GAP** |
| Map view for results | MapKit integration with pins | Not found | **GAP** |
| Category search prep | Location + dates + guests form before search | Not found | **GAP** |
| Infinite scroll/pagination | loadNextPageIfNeeded | Not verified | **GAP** |
| Empty state | "No matches" + "Clear filters" button | Empty state exists | Parity |
| Loading state | "Searching..." text | Loading indicator | Parity |
| Error state | "Couldn't load results" + Retry button | Error state exists | Parity |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| SEARCH-1 | Missing expandable search cards (What/Where/When/Who) | P0 | iOS has a rich Airbnb-style search sheet with 4 collapsible sections: What (with auto-suggestions), Where (with location permissions and map pin suggestions), When (date pickers), Who (guest stepper). This is the primary search UX and a major gap. |
| SEARCH-2 | Missing advanced search filters | P0 | iOS has a comprehensive filter sheet: price range, 6 amenity toggles, availability filter, sort options, 10 property types, 3 booking option toggles (Instant Book, Pet Friendly, Wheelchair Accessible), and a distance slider. Android has only basic category chips. |
| SEARCH-3 | Missing map view for search results | P1 | iOS integrates MapKit to show results on a map with pins. Android should use Google Maps. |
| SEARCH-4 | Missing category search prep view | P1 | iOS has a pre-search form (location + dates + guests) before showing category results. Provides a more guided search experience. |
| SEARCH-5 | Missing sort options | P1 | iOS supports sorting by Relevance, Price (Low/High), and Rating. Android has no sort capability. |

---

## 4. Listing Detail

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Image gallery with page indicators | TabView + dot indicators + "Show all photos" | Hero image + gallery viewer | Parity |
| Title, rating, location | Full display | Full display | Parity |
| Host card | Avatar + name + Superhost badge + years hosting + response time | Host info with avatar | Partial |
| Description/About | Full section | Description section | Parity |
| Amenities | Grid of badges + horizontal scroll chips | Amenities with icons | Parity |
| Reviews section | Rating summary + sample reviews | Review section with submit | Parity |
| Location section ("Where you'll stay") | Address with map instructions | Open in maps | Partial |
| House Rules | Dedicated section | Rules and policies section | Parity |
| Cancellation Policy | Dedicated section with details | Host cancellation policy | Parity |
| FAQ section | Not found in detail view | FAQ section present | Android ahead |
| Bottom booking bar | Price + date/guest summary + "Book Now" | Reserve flow with bottom sheet | Parity |
| Favorite toggle | Heart icon in nav bar | Heart toggle | Parity |
| Share | square.and.arrow.up in nav bar | Share listing | Parity |
| Report/Flag | Flag icon → ReportBlockSheet | Not found on detail | **GAP** |
| Contact host | Navigate to inbox | Navigate to inbox | Parity |
| Guest details requirements | Listed | Listed | Parity |
| Guest notes | Present | Present | Parity |
| Check-in/check-out times | Present | Present | Parity |
| Type-specific detail views | 4 variants: generic, meeting room, rental gear, parking spot | 1 generic view | **GAP** |
| Time selection sheet | Hourly booking with date + duration + guest selection | Date picker in bottom sheet | Partial |
| Image gallery full-screen | "Show all photos" opens gallery | Gallery viewer | Parity |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| DETAIL-1 | Missing type-specific listing detail views | P1 | iOS has 4 specialized detail views: `MeetingRoomDetailView` (capacity, equipment, hourly booking), `RentalGearDetailView` (what's included, deposit, daily booking), `ParkingSpotDetailView` (max height, vehicle size, security features), and a generic `ListingDetailView`. Android uses one generic view for all types. Type-specific views improve UX by showing relevant info prominently. |
| DETAIL-2 | Missing Report/Flag on listing detail | P1 | iOS has a flag icon in the nav bar that opens a `ReportBlockSheet` for reporting inappropriate listings. Android should add this for trust & safety compliance. |
| DETAIL-3 | Host card missing Superhost badge and response time | P2 | iOS host card shows Superhost badge, years hosting, and response time. Android shows basic host info. |

---

## 5. Booking Flow

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Date selection (check-in/out) | Calendar date picker | Date picker | Parity |
| Guest count selector | Plus/minus with max validation | Guest count selector | Parity |
| Special requests input | Text input (up to 6 lines) | Special requests input | Parity |
| Pricing breakdown | Base × nights + cleaning + service + security + taxes | Total price calculation | **GAP** |
| Auth check before checkout | AuthFlowSheet if not logged in | Auth required flow | Parity |
| Stripe checkout | WebView (StripeCheckoutWebView) | Chrome Custom Tabs | Parity |
| Booking confirmation | Rich view: ID, dates, guests, requests, total, next steps | Success message with booking ID | **GAP** |
| Hourly booking flow | CalendarTimeSheet with time slots + duration | Date picker in bottom sheet | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| BOOK-1 | Missing detailed pricing breakdown | P0 | iOS shows itemized pricing: base price × nights, cleaning fee, service fee, security deposit, and taxes before checkout. Android shows only a total price. Users need transparency on costs before paying. |
| BOOK-2 | Booking confirmation screen is minimal | P1 | iOS confirmation shows: booking ID, dates, guests, special requests, total paid, and "Next Steps" (email confirmation timing, add to calendar, contact host, get directions). Android shows a basic success message. |
| BOOK-3 | Missing hourly/time-based booking UX | P1 | iOS has a dedicated `CalendarTimeSheet` for hourly bookings: date picker + available time slots + duration selection. Android uses a basic date picker for all booking types. |

---

## 6. Booking Management

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Booking list | List with status display | List with status chips | Parity |
| Filter by status | Upcoming / Past / Cancelled / All tabs | No filtering | **GAP** |
| Search bookings | By property title, host, location | No search | **GAP** |
| Booking detail view | Rich: image, ID, dates, location, guests, duration, PIN, status, pricing | Basic: ID, listing, dates, status, price | **GAP** |
| Cancel booking | Modal with reason field + refund policy info | Basic cancel action | **GAP** |
| Rescheduling | Calendar + time picker + proposal submission | Not found | **GAP** |
| Post-booking review | 5-star + text + content moderation | Not found in booking flow | **GAP** |
| Post-booking confirmation sheet | Issue window (48h): "Everything went well" / "Report issue" | Not found | **GAP** |
| Message host from booking | Direct action button | Not found | **GAP** |
| Download receipt | Available | Not found | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| MGMT-1 | Missing booking status filter tabs | P0 | iOS has segmented tabs (Upcoming, Past, Cancelled, All) for quick filtering. Android shows a flat list with no filtering. Essential for users with multiple bookings. |
| MGMT-2 | Cancel booking lacks reason and refund policy | P0 | iOS cancel modal shows: warning, booking summary, optional reason text field, and refund policy ("24+ hours: full refund" / "Within 24 hours: subject to cancellation policy"). Android has a basic cancel action. |
| MGMT-3 | Missing post-booking review submission | P1 | iOS shows a review form (5-star rating + text) for completed bookings with content moderation. Android has reviews screen but no booking-triggered review flow. |
| MGMT-4 | Missing rescheduling flow | P1 | iOS has `ReScheduleView` with calendar/time picker and "Submit a New Proposal" requiring host approval. Android has no reschedule capability. |
| MGMT-5 | Missing post-checkout confirmation sheet | P2 | iOS auto-shows a sheet for completed bookings within the 48h issue window: "Everything went well" or "Report issue". Improves trust and issue resolution. |
| MGMT-6 | Missing receipt download | P2 | iOS allows downloading booking receipts. Android does not. |

---

## 7. Payment

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Stripe hosted checkout | WebView | Chrome Custom Tabs | Parity |
| Apple Pay / Google Pay | Apple Pay via `ApplePayManager` | Not implemented | **GAP** |
| Payment method management | Add/edit/delete saved cards | SettingsPaymentMethodsScreen | Parity |
| Payment history | Available | Not found | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| PAY-1 | Missing Google Pay integration | P0 | iOS has native Apple Pay as default payment with fallback to card. Android should implement Google Pay for equivalent frictionless checkout. The iOS implementation uses `ApplePayManager.processPayment(amount:currency:description:bookingId:)`. |
| PAY-2 | Missing payment history view | P2 | iOS has a payment history section. Android settings has payment methods but no history. |

---

## 8. Messaging / Chat

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Thread list with unread badges | Yes | Yes | Parity |
| Message history with pagination | Yes | Yes (via API) | Parity |
| Send text messages | Yes | Yes | Parity |
| Guest/Host mode toggle | Yes | Yes | Parity |
| Photo/image attachments | Up to 10 images, gallery picker | Photo picker (PickMultipleVisualMedia) | Parity |
| Camera capture | Camera integration for direct photos | Not implemented | **GAP** |
| Photo preview tray | Preview before sending with remove | Preview tray with remove | Parity |
| Image compression | 1280px max, JPEG 85%, 10MB limit | Not implemented | **GAP** |
| Full-screen image viewer | Zoom/pinch gestures | Swipe-to-dismiss viewer | Partial |
| Photo grid layout | 1-4+ layout with "+N more" | PhotoGrid implementation (1-4+) | Parity |
| Message status indicators | Sending, failed, delivered | Status indicators | Parity |
| Failed message retry | Retry mechanism | Not verified | **GAP** |
| Report/flag messages | Report capability | ReportBlockSheet | Parity |
| Block banners | Block indicator when conversation blocked | Not verified | **GAP** |
| Moderation warnings | Content flagging and warnings | Not found | **GAP** |
| Upload progress bar | Visual progress | Upload handling | Partial |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| CHAT-1 | Missing camera capture for chat photos | P1 | iOS integrates camera for direct photo capture in chat. Android only has gallery picker. |
| CHAT-2 | Missing image compression before upload | P1 | iOS compresses images to 1280px max dimension, JPEG 85% quality, 10MB limit. Without this, Android may upload unnecessarily large files, consuming bandwidth and storage. |
| CHAT-3 | Missing content moderation warnings | P2 | iOS has `ContentModerationService` that can block, warn, or allow message content. Android doesn't show moderation warnings. |
| CHAT-4 | Missing failed message retry and block banners | P2 | iOS has explicit retry for failed messages and shows banners when a conversation is blocked. |

---

## 9. Notifications

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Push notification routing | Screen-based routing | Screen-based routing (NotificationRouter) | Parity |
| In-app notification center | NotificationView with grouping | Notification center | Partial |
| Two-tab layout (Notifications + Messages) | Inbox has Notifications tab + Messages tab | Separate notification and inbox screens | **GAP** |
| Grouped by time | Today, This Week, This Month, Earlier | Not grouped | **GAP** |
| Mark as read | Per-notification marking | Not verified | **GAP** |
| Pagination | 50 per page | Not verified | **GAP** |
| Unread badge | Bell icon with badge count | Unread badges | Parity |
| Notification preferences | Full UI with category toggles | Full settings with toggles | Parity |
| OneSignal integration | OneSignal + Ably push | Firebase Messaging | Different but OK |
| Quiet hours | Do-not-disturb scheduling | Quiet hours setting | Parity |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| NOTIF-1 | Notifications not grouped by time period | P1 | iOS groups notifications into "Today", "This Week", "This Month", "Earlier" sections for better scanability. Android shows a flat list. |
| NOTIF-2 | Missing mark-as-read functionality | P2 | iOS allows marking individual notifications as read. |

---

## 10. Profile

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Avatar + name + email | Yes | Yes | Parity |
| Profile completion percentage | Not found | Yes | Android ahead |
| Guest/Host mode toggle | Yes | Yes | Parity |
| Verification status badge | Yes | Yes | Parity |
| Edit profile | Yes | Yes | Parity |
| Statistics pills (Bookings count, Wishlist count) | Yes | Not found | **GAP** |
| Quick action chips (Edit, Bookings, Wishlist, Host Mode, My Bids) | Yes | Navigation links | **GAP** |
| Create listing CTA | Gradient button with shadow | Not found | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| PROF-1 | Missing statistics pills on profile | P2 | iOS shows booking count and wishlist count as pills on the profile screen. Quick at-a-glance info. |
| PROF-2 | Missing quick action chips | P2 | iOS has styled action chips (Edit, Bookings, Wishlist, Host Mode, My Bids) for fast navigation. Android uses plain navigation links. |

---

## 11. Settings

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Personal info | Yes | Yes | Parity |
| Login & Security | Yes (password, 2FA, sessions, deactivation) | Yes | Parity |
| Notifications | Category-based toggles | Category-based toggles | Parity |
| Preferences (theme, language, privacy) | Yes | Yes | Parity |
| Payment methods | Add/edit/delete/set primary | Add/delete/set primary | Parity |
| Help & Support | Help Center, Contact, Report | FAQ, Contact | Parity |
| Terms of Service / Privacy Policy | Links | About Us page | Partial |
| Sign out with confirmation | Confirmation alert | Sign out | Partial |
| Version info | App version + build number | Not verified | **GAP** |
| Account deletion | Available | Available (under Login & Security) | Parity |

### Findings

No critical gaps. Minor polish differences only.

---

## 12. Reviews

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Standalone reviews screen | Yes | Yes (ReviewsScreen, 652 lines) | Parity |
| Filter by rating | All, 5-1 stars | ALL / POSITIVE / NEGATIVE / WITH_PHOTOS | Parity (Android has different but equivalent filters) |
| Rating breakdown bars | Count + percentage bars | RatingBreakdownItem with count/percentage | Parity |
| Category ratings | Not found | Cleanliness, accuracy, communication, location, value | Android ahead |
| Search reviews | By text or reviewer name | Not verified | **GAP** |
| Host reply display | Gray box below review | Host response display | Parity |
| Helpful reaction | Thumbs up + count | Helpful count + report | Parity |
| Review submission with photos | Not found | Available | Android ahead |

### Findings

Reviews implementation is at parity or better on Android. The only gap is search within reviews, which is P2.

---

## 13. Wishlist / Favorites

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| View favorited listings | Yes | Yes | Parity |
| Filter by type | Custom filter buckets | WishlistFilter enum | Parity |
| Create custom lists | "Create New List" button | Not found | **GAP** |
| Search within wishlist | By title, location, category | Not found | **GAP** |
| Item detail sheet | Image carousel + full info | Card click → listing detail | Parity |
| Remove from wishlist | Heart button | Optimistic remove with restore on failure | Parity |
| Share | Share capabilities | Not found | **GAP** |
| Toast notifications | Success/error auto-dismiss | Toast notifications | Parity |
| "Book Now" button | Not found on wishlist card | Book Now buttons | Android ahead |
| Empty/locked states | Yes | Yes | Parity |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| WISH-1 | Missing create custom wishlist lists | P2 | iOS allows creating named wishlist collections. Android has only the default favorites list. |
| WISH-2 | Missing search within wishlist | P2 | iOS can search saved items by title, location, category. |

---

## 14. Host Mode

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Host Dashboard | Greeting, KPIs, quick actions, upcoming bookings, listings, history | HostDashboardScreen | Parity |
| KPI chips | Active listings, under review, upcoming bookings, pending requests, monthly earnings | Present | Parity |
| Quick actions | Create listing, view listings, manage payouts | Present | Parity |
| Host bookings | Segmented tabs (Booked/History/Calendar) | HostBookingsScreen | Parity |
| Host listings | List with status badges (active/under review/rejected), edit | HostListingsScreen | Parity |
| Host earnings | Rich dashboard: balance cards, bank deposits, booking earnings, fee breakdowns | HostEarningsScreen (957 lines) | Parity |
| Stripe Connect onboarding | Create onboarding link | StripeConnectOnboardingScreen | Parity |
| "How you get paid" banner | Connection state-dependent messaging | Not verified | **GAP** |
| Listing creation wizard | 8-step flow (address → type → title → amenities → details → photos → availability → checkout options) | Create listing flow | Parity |
| Calendar/availability management | FSCalendar integration with visual booking blocks + day/hour availability picker | Calendar/availability management | Parity |
| Switch mode / sign out | Toggle + sign out | Toggle + sign out | Parity |
| Event history log | Color-coded events (booking request, payment, payout, hold) | Not verified | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| HOST-1 | Missing "How you get paid" explanatory banner | P2 | iOS has a connection-state-dependent banner explaining payout mechanics (Connected: "Automatic payouts active, 100% of Stripe net", Pending: "Finish setup", Not connected: "How you get paid"). Helps host understanding. |

---

## 15. Other Features

### Feature Comparison

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Bidding system | Create bid, view bids, status tracking | BiddingScreen with full flow | Parity |
| Blog | List + detail with categories, author, read time | BlogScreen with categories, author, read time | Parity |
| Split booking | Complete: invite view, participant grid, hold window, status tracking | SplitBookingScreen with full models | Parity |
| Roommate finder | Multi-screen flow with cards, profiles, chat | RoommateFinderScreen | Parity |
| Trip planner | Basic planning interface | TripPlannerScreen | Parity |
| Identity verification | Stripe Identity + phone verification | ID + phone verification screens | Parity |
| Collections/Lists | Not found | CollectionsScreen | Android ahead |
| Deep linking | Booking success/cancel, listing detail, split | Booking success/cancel deep links | Parity |
| Localization | 7 languages (en, ar, es, fr, ja, ko, zh-Hans) | English only | **GAP** |

### Findings

| ID | Finding | Priority | Detail |
|----|---------|----------|--------|
| OTHER-1 | No localization beyond English | P1 | iOS supports 7 languages. Android has only English strings. For international launch, Android needs at minimum the same language support. |

---

## Consolidated Findings by Priority

### P0 — Must Fix (6 items)

These are blocking issues that prevent launch or create significantly broken user experiences.

| ID | Area | Finding | Effort |
|----|------|---------|--------|
| SEARCH-1 | Search | Missing expandable search cards (What/Where/When/Who) | Large (2-3 weeks) |
| SEARCH-2 | Search | Missing advanced search filters (price, amenities, property type, booking options, distance) | Large (1-2 weeks) |
| BOOK-1 | Booking | Missing detailed pricing breakdown before checkout | Medium (3-5 days) |
| MGMT-1 | Booking Mgmt | Missing booking status filter tabs (Upcoming/Past/Cancelled/All) | Medium (3-5 days) |
| MGMT-2 | Booking Mgmt | Cancel booking lacks reason field and refund policy display | Medium (3-5 days) |
| PAY-1 | Payment | Missing Google Pay integration | Large (1-2 weeks) |

### P1 — Should Fix Before Launch (10 items)

These are important for UX quality and feature completeness.

| ID | Area | Finding | Effort |
|----|------|---------|--------|
| SEARCH-3 | Search | Missing map view for search results | Medium (1 week) |
| SEARCH-4 | Search | Missing category search prep view | Small (2-3 days) |
| SEARCH-5 | Search | Missing sort options | Small (1-2 days) |
| DETAIL-1 | Listing Detail | Missing type-specific detail views (meeting room, gear, parking) | Large (2-3 weeks) |
| DETAIL-2 | Listing Detail | Missing Report/Flag action on listing detail | Small (1-2 days) |
| BOOK-2 | Booking | Booking confirmation screen is minimal | Small (2-3 days) |
| BOOK-3 | Booking | Missing hourly/time-based booking UX | Medium (1 week) |
| MGMT-3 | Booking Mgmt | Missing post-booking review submission flow | Medium (3-5 days) |
| MGMT-4 | Booking Mgmt | Missing rescheduling flow | Medium (1 week) |
| AUTH-2 | Auth | OTP error messages not network-specific | Small (1 day) |
| AUTH-3 | Auth | No explicit terms acceptance on signup | Small (1 day) |
| CHAT-1 | Chat | Missing camera capture for chat photos | Small (1-2 days) |
| CHAT-2 | Chat | Missing image compression before upload | Small (1-2 days) |
| NOTIF-1 | Notifications | Notifications not grouped by time period | Small (2-3 days) |
| OTHER-1 | Localization | No localization beyond English | Large (depends on translation pipeline) |

### P2 — Polish Improvements (7 items)

These are nice-to-haves that improve perceived quality.

| ID | Area | Finding | Effort |
|----|------|---------|--------|
| HOME-1 | Home | Missing Three Steps CTA section | Small (1 day) |
| HOME-2 | Home | Trending Destinations is simpler | Small (1-2 days) |
| DETAIL-3 | Listing Detail | Host card missing Superhost badge and response time | Small (1 day) |
| PROF-1 | Profile | Missing statistics pills | Small (1 day) |
| PROF-2 | Profile | Missing quick action chips | Small (1-2 days) |
| MGMT-5 | Booking Mgmt | Missing post-checkout confirmation sheet (48h issue window) | Small (2-3 days) |
| MGMT-6 | Booking Mgmt | Missing receipt download | Small (2-3 days) |
| PAY-2 | Payment | Missing payment history view | Small (2-3 days) |
| CHAT-3 | Chat | Missing content moderation warnings | Small (2-3 days) |
| CHAT-4 | Chat | Missing failed message retry and block banners | Small (1-2 days) |
| NOTIF-2 | Notifications | Missing mark-as-read for notifications | Small (1 day) |
| WISH-1 | Wishlist | Missing create custom lists | Small (2-3 days) |
| WISH-2 | Wishlist | Missing search within wishlist | Small (1 day) |
| HOST-1 | Host | Missing "How you get paid" banner | Small (1 day) |
| AUTH-1 | Auth | Missing Facebook OAuth | Small (1-2 days) |

---

## 16. Recommended Implementation Order

Work is ordered by: user impact → booking/payment flow → launch readiness.

### Sprint 1: Booking & Payment Critical Path (Weeks 1-2)

1. **BOOK-1**: Add detailed pricing breakdown to booking flow
2. **MGMT-1**: Add booking status filter tabs (Upcoming/Past/Cancelled/All)
3. **MGMT-2**: Enhance cancel booking with reason + refund policy modal
4. **AUTH-3**: Add terms acceptance checkbox to email signup
5. **AUTH-2**: Map OTP error codes to user-friendly messages

### Sprint 2: Search Foundation (Weeks 3-5)

6. **SEARCH-2**: Build advanced search filters sheet (price, amenities, property type, booking options, distance)
7. **SEARCH-1**: Build expandable search cards (What/Where/When/Who)
8. **SEARCH-5**: Add sort options to search results

### Sprint 3: Payment & Booking UX (Weeks 5-7)

9. **PAY-1**: Integrate Google Pay for native checkout
10. **BOOK-2**: Enrich booking confirmation screen (next steps, share, etc.)
11. **BOOK-3**: Build hourly/time-based booking UX with time slot selection
12. **DETAIL-2**: Add Report/Flag action on listing detail

### Sprint 4: Search & Detail Polish (Weeks 7-9)

13. **SEARCH-3**: Add map view for search results (Google Maps)
14. **SEARCH-4**: Build category search prep view
15. **DETAIL-1**: Create type-specific listing detail views (meeting room, gear, parking)

### Sprint 5: Booking Lifecycle & Chat (Weeks 9-11)

16. **MGMT-3**: Build post-booking review submission flow
17. **MGMT-4**: Build rescheduling flow with proposal
18. **CHAT-1**: Add camera capture in chat
19. **CHAT-2**: Add image compression before upload
20. **NOTIF-1**: Group notifications by time period

### Sprint 6: Localization & Polish (Weeks 11-13)

21. **OTHER-1**: Add localization for supported languages
22. All remaining P2 items as capacity allows

---

## 17. External Dependencies

These items require work **outside** the Android repository:

| Dependency | Required For | Type |
|------------|-------------|------|
| Google Pay merchant setup | PAY-1 | Google Pay API credentials + Stripe backend config |
| Stripe PaymentIntent API for Google Pay | PAY-1 | Backend endpoint to create PaymentIntents (vs checkout sessions) |
| Search filter API parameters | SEARCH-2 | Backend must accept filter query params (propertyType, amenities, distance, etc.) |
| Sort parameter in search API | SEARCH-5 | Backend must support `sort` query parameter |
| Booking cancellation reason API field | MGMT-2 | Backend must accept and store cancellation reason |
| Rescheduling / proposal API | MGMT-4 | Backend endpoint for submitting reschedule proposals |
| Content moderation API | CHAT-3, MGMT-3 | Backend moderation check endpoint |
| Receipt generation API | MGMT-6 | Backend endpoint to generate downloadable receipt PDF/data |
| Translation files | OTHER-1 | Translated strings for ar, es, fr, ja, ko, zh-Hans |
| Google Maps API key | SEARCH-3 | Google Maps SDK for Android (may already be configured) |
| Algolia search integration | SEARCH-1, SEARCH-2 | Verify Algolia index supports required filter facets |

---

## Areas Where Android Is Ahead of iOS

For completeness, these are areas where the Android app has features not found in iOS:

| Feature | Android | iOS |
|---------|---------|-----|
| Profile completion percentage | Shown on profile | Not found |
| Category ratings in reviews | Cleanliness, accuracy, communication, location, value | Not found |
| Review submission with photos | Supported | Not found |
| Collections/Lists | CollectionsScreen | Not found |
| FAQ section on listing detail | Present | Not found |
| "Book Now" on wishlist cards | Present | Not found |
| Notification quiet hours settings | Full UI | Basic |
| 6-tab bottom navigation | Bookings + Inbox as separate tabs (faster access) | 5-tab (Inbox under Messages tab) |

---

## Files Audited

### iOS (source of truth)
- `PaceDream/Views/Authentication/` — Auth flow, OTP, phone entry
- `PaceDream/Views/Onboarding/` — Onboarding, basic setup
- `PaceDream/Views/Main/Home/` — Home, search, listings
- `PaceDream/Views/Main/Search/` — Category search
- `PaceDream/Views/Main/Bookings/` — Booking management
- `PaceDream/Views/Main/Inbox/` — Messaging, notifications
- `PaceDream/Views/Main/Profile/` — Profile, wishlist, reviews, partners
- `PaceDream/Views/Booking/` — Booking creation, confirmation
- `PaceDream/Views/Payment/` — Payment, Apple Pay
- `PaceDream/Views/DetailPages/` — Listing detail variants
- `PaceDream/Views/HostMode/` — Host dashboard, earnings, listings
- `PaceDream/Views/HostSettings/` — Host bookings, business
- `PaceDream/Views/Hosting/` — Listing creation wizard
- `PaceDream/Views/Settings/` — Settings, security
- `PaceDream/Views/Chat/` — Chat system
- `PaceDream/Services/` — Bookings, Host, Payouts, Messaging, Notifications, Realtime
- `PaceDream/ViewModels/` — Bookings, Host, Messaging
- `Shared/view/` — Login, register, OTP, settings, search, filter

### Android (audited)
- `feature/auth/`, `feature/signin/`, `feature/create-account/` — Authentication
- `feature/home/` — Home, discovery
- `feature/search/` — Search
- `feature/booking/`, `feature/webflow/` — Booking, checkout
- `feature/chat/`, `feature/inbox/` — Messaging
- `feature/notification/` — Notifications
- `feature/wishlist/` — Favorites
- `feature/host/` — Host mode
- `feature/payment/` — Payment
- `app/src/main/kotlin/com/pacedream/app/feature/` — Listing detail, reviews, bidding, blog, split booking, settings, profile
- `core/network/`, `core/data/`, `core/database/`, `core/ui/` — Core infrastructure
- `common/` — Design system, theme

---

*End of audit report.*
