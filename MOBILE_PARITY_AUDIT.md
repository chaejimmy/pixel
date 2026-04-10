# PaceDream Mobile Parity Audit: Android vs iOS

**Date:** 2026-04-10
**Auditor:** Automated code audit
**Scope:** Functional parity across 14 focus areas

---

## A. Top Mobile Parity Gaps (Summary)

| # | Gap | Severity | Flow |
|---|-----|----------|------|
| 1 | iOS has no 3-step OTP signup flow | P0 | Auth/Registration |
| 2 | Android missing push device deregistration on logout | P0 | Push / Logout |
| 3 | Android has no Ably realtime for chat | P1 | Chat/Inbox |
| 4 | iOS deep link handlers are stub-only (print-only) | P1 | Deep Links |
| 5 | Image upload max dimension mismatch (2048 vs 1024) | P1 | Image Uploads |
| 6 | iOS has OTP phone login via AuthFlowSheet; Android routes through legacy feature module | P1 | Auth |
| 7 | Android legacy AuthSession cancellation treated as error | P1 | Auth |
| 8 | Android missing `forgotPassword` in new SessionManager | P1 | Auth |
| 9 | iOS has `accountRestriction` overlay; Android `Restricted` state has no dedicated UI | P1 | Auth/Security |
| 10 | Tab bar order divergence between platforms | P2 | Navigation |

---

## B. Detailed Gap Analysis

### Gap 1: iOS Missing 3-Step OTP Signup Flow
- **Severity:** P0
- **Affected flow:** Auth / Registration
- **Android behavior:** Legacy `AuthSession` (core/network) implements a full 3-step OTP signup:
  1. `initiateEmailSignup` / `initiateMobileSignup` (send OTP)
  2. `verifyEmailSignupOtp` / `verifySmsSignupOtp` (verify code)
  3. `completeEmailSignup` / `completeMobileSignup` (set password & profile)
- **iOS behavior:** Only has direct `POST /v1/auth/signup/email` (single-step signup with name/email/password). No initiate/verify/complete OTP flow. The only OTP code visible in iOS is for phone-based **login** (not signup).
- **Files:**
  - Android: `core/network/.../auth/AuthSession.kt:504-617` (6 methods)
  - iOS: `AuthService.swift` — only `signUp(email:password:firstName:lastName:)`
- **Root cause:** 3-step OTP signup was added to Android for web parity but never ported to iOS.
- **Recommended fix:** Implement `initiateEmailSignup`, `verifyEmailSignupOtp`, `completeEmailSignup` in iOS `AuthService` and wire through `OnboardingViewModel`.

---

### Gap 2: Android Missing Push Device Deregistration on Logout
- **Severity:** P0
- **Affected flow:** Push Notifications / Logout
- **Android behavior:** `SessionManager.signOut()` clears tokens and OneSignal external user ID, but does **not** call `DELETE /v1/push-devices` to deregister the FCM token from the backend. After logout, the backend may still route push notifications to the old device token.
- **iOS behavior:** `AuthSession.signOut()` explicitly calls `PushDeviceRegistrar.shared.deregister(authToken:)` which fires `DELETE /v1/push-devices` before clearing the token. Also calls `OneSignalService.setExternalUserId(nil)`.
- **Files:**
  - iOS: `AuthSession.swift:224-246`, `PushDeviceRegistrar.swift:35-57`
  - Android: `SessionManager.kt:325-337` (no deregister call), `FcmTokenRegistrar.kt` (no `deregister` method)
- **Root cause:** `FcmTokenRegistrar` was built for registration only; deregistration path was never added.
- **Recommended fix:** Add `FcmTokenRegistrar.deregister(authToken)` that fires `DELETE /v1/push-devices`. Call it from `SessionManager.signOut()` before clearing tokens.

---

### Gap 3: Android Has No Ably Realtime for Chat
- **Severity:** P1
- **Affected flow:** Chat / Inbox
- **Android behavior:** Chat uses HTTP polling only. No Ably SDK dependency. `ThreadViewModel` loads messages on init and on manual refresh, with no live push of new messages.
- **iOS behavior:** Has full Ably integration: `AblyManager.swift`, `AblyRealtimeService.swift`, `AblyAuthService.swift`, `AblyPushService.swift`, `AblyClientHelper.swift`. Subscribes to channels for real-time message delivery in chat threads.
- **Files:**
  - iOS: `PaceDream/AblyManager.swift`, `Services/Realtime/AblyRealtimeService.swift`
  - Android: No Ably files exist anywhere in the pixel repo
- **Root cause:** Ably SDK was integrated on iOS but never ported to Android.
- **Recommended fix:** Either add Ably SDK to Android (feature/chat module), or implement a WebSocket/SSE fallback. Alternatively, if push notifications handle new-message delivery, ensure Android re-fetches messages when a chat push arrives while the thread is open.

---

### Gap 4: iOS Deep Link Handlers Are Stub-Only
- **Severity:** P1
- **Affected flow:** Deep Links
- **Android behavior:** `DeepLinkHandler.kt` fully routes deep links to `BookingSuccess`, `BookingCancelled`, `ListingDetail`, `GearDetail`, and `StripeConnectReturn` with actual navigation.
- **iOS behavior:** `DeepLinkManager.swift` **parses** 10 deep link types (listing, booking, message, profile, identity, payment, search, host, timeBasedDetail, stripeConnectReturn) but all specific handlers (`handleListingDeepLink`, `handleBookingDeepLink`, `handleMessageDeepLink`, etc.) are **print-only stubs** — they just log to console in DEBUG builds and do nothing.
- **Files:**
  - iOS: `Utilities/DeepLinkManager.swift:235-295` (all handlers are `#if DEBUG print(...)`)
  - Android: `feature/webflow/DeepLinkHandler.kt:36-91` (functional routing)
- **Root cause:** iOS deep link parsing was built but navigation was never wired.
- **Recommended fix:** Wire each deep link case to actual navigation: set `currentDeepLink` and observe it in `ContentView` / `AppRouter` to push the appropriate screen.

---

### Gap 5: Image Upload Max Dimension Mismatch
- **Severity:** P1
- **Affected flow:** Image Uploads / Create Listing
- **Android behavior:** `ImageUploadService.kt` downscales to **1024px** max (retry at 800px), JPEG quality 70% (retry at 50%). Max bytes target: ~450KB.
- **iOS behavior:** `CloudinaryUploader.swift` downscales to **2048px** max, JPEG quality 82% (retry at 60%). No explicit max bytes target (just retry at lower quality).
- **Impact:** Same image uploaded from iOS will be ~4x larger in pixel count. This causes: (a) slower uploads on iOS cellular, (b) higher Cloudinary storage, (c) visible quality difference in listing photos between platforms.
- **Files:**
  - iOS: `Services/CloudinaryUploader.swift:49-63`
  - Android: `feature/host/data/ImageUploadService.kt:237-243`
- **Root cause:** Each platform set dimensions independently without a shared spec.
- **Recommended fix:** Align on 1280px max / 75% JPEG quality on both platforms.

---

### Gap 6: OTP Phone Login Path Divergence
- **Severity:** P1
- **Affected flow:** Auth / OTP Login
- **Android behavior:** OTP phone login flows through the legacy `feature/signin` module (`PhoneEntryScreen.kt`, `OtpVerificationScreen.kt`, `PhoneEntryViewModel.kt`). These are full standalone screens in the onboarding navigation graph.
- **iOS behavior:** OTP phone login is embedded **inside** `AuthFlowSheet` as a modal sheet (`showOTPLogin` state). The `OTPLoginView` and `OTPVerificationView` are presented as sub-views within the auth bottom sheet, not as standalone screens.
- **Impact:** UX feels different — Android navigates to new screens, iOS presents within a sheet.
- **Files:**
  - iOS: `Views/Authentication/AuthFlowSheet.swift`, `Views/Authentication/OTPLoginView.swift`
  - Android: `feature/signin/.../otp/PhoneEntryScreen.kt`, `feature/signin/.../otp/OtpVerificationScreen.kt`
- **Root cause:** Different UI architecture decisions per platform.
- **Recommended fix:** Low priority — this is partially a platform-native pattern. Ensure both reach the same API endpoints and produce the same auth tokens.

---

### Gap 7: Android Legacy Auth0 Cancellation Treated as Error
- **Severity:** P1
- **Affected flow:** Auth / Login
- **Android behavior:** In the **legacy** `AuthSession.loginWithAuth0()`, when the user cancels Auth0 login, `error.isCanceled` returns `Result.failure(Exception("Login cancelled"))`. Callers may show an error toast/snackbar.
- **iOS behavior:** `AuthFlowSheet` handles Auth0 cancellation silently — no error is surfaced.
- **The new `SessionManager`** handles this correctly with `AuthActionResult.Cancelled`, but the legacy `AuthSession` (still used by some screens) does not.
- **Files:**
  - Android legacy: `core/network/.../auth/AuthSession.kt:418-424`
  - Android new: `app/.../auth/AuthSession.kt:224-229` (correct)
  - iOS: `Views/Authentication/AuthFlowSheet.swift` (silently dismissed)
- **Root cause:** Legacy auth system not fully retired.
- **Recommended fix:** Either migrate all screens to `SessionManager` or patch legacy `AuthSession.loginWithAuth0` to distinguish cancellation from error.

---

### Gap 8: Android Missing `forgotPassword` in New SessionManager
- **Severity:** P1
- **Affected flow:** Auth / Password Reset
- **Android behavior:** `forgotPassword` only exists in legacy `AuthSession` (`core/network`). The new `SessionManager` (`app/core/auth`) has no `forgotPassword` method. Screens using the new auth system cannot trigger password reset.
- **iOS behavior:** `AuthService.forgotPassword(email:)` is available and wired through `AuthFlowSheet` with a dedicated `.forgotPassword` mode.
- **Files:**
  - iOS: `Services/AuthService.swift:273-295`, `Views/Authentication/AuthFlowSheet.swift` (mode: .forgotPassword)
  - Android new SessionManager: not present
  - Android legacy AuthSession: `core/network/.../auth/AuthSession.kt:261-288`
- **Root cause:** `SessionManager` was built as a replacement but forgot-password wasn't ported.
- **Recommended fix:** Add `SessionManager.forgotPassword(email)` delegating to `authRepository.forgotPassword()`.

---

### Gap 9: Account Restriction UI Missing on Android
- **Severity:** P1
- **Affected flow:** Auth / Security
- **Android behavior:** `AuthState.Restricted` exists with fields (`message`, `requiresLogout`, `requiresVerification`). But there is **no dedicated `AccountRestrictionView`** composable that observes this state and shows an overlay.
- **iOS behavior:** `AuthSession.accountRestriction` is a `@Published` property. `AccountRestrictionView.swift` observes it and shows a full-screen overlay with the restriction message and a support contact button.
- **Files:**
  - iOS: `ViewModels/AuthSession.swift:36-37`, `Views/Authentication/AccountRestrictionView.swift`
  - Android: `core/network/.../auth/AuthSession.kt:816-822` (state defined), no UI
- **Root cause:** Backend error handling was added to the network layer but UI was never built.
- **Recommended fix:** Create `AccountRestrictionScreen.kt` composable and observe `authState` for `Restricted` in the main navigation host.

---

### Gap 10: Tab Bar Order Divergence
- **Severity:** P2
- **Affected flow:** Navigation
- **Android behavior (guest mode):** Home, Search, Favorites, Bookings, Inbox, Profile (6 tabs in `DashboardDestination`)
- **iOS behavior (guest mode):** Home, Favorites, Bookings, Messages, Profile (5 tabs — no Search tab)
- **Android behavior (host mode):** Separate host navigation graph with Dashboard, Listings, Bookings, Inbox, Earnings, Profile
- **iOS behavior (host mode):** Dashboard, Earnings, Post, Inbox, Profile (5 tabs)
- **Impact:** Users switching between devices see different tab counts and positions. The "Search" tab on Android exists as a standalone tab; on iOS, search is accessible from Home.
- **Files:**
  - iOS: `App/Tabs.swift:17-40`
  - Android: `navigation/UserStartTopLevelDestination.kt:11-17`
- **Root cause:** Independent UI design decisions.
- **Recommended fix:** Align on 5 guest tabs (Home, Search, Bookings, Inbox, Profile) or (Home, Favorites, Bookings, Inbox, Profile) on both platforms.

---

## Additional Parity Observations

### Search Filters
- **iOS** has `minPrice`, `maxPrice`, `amenities`, `availableOnly`, `sort` (recommended/price/rating) filters with 3 tabs (Share/Book/Split).
- **Android** has filter support via `FilterScreen.kt` in the home presentation module with similar price range and category filters.
- **Gap (P2):** iOS exposes a `sort` parameter (`price_asc`, `rating_desc`); Android search doesn't expose a sort selector in UI.

### Listing Detail
- Both platforms render listings with photos, description, amenities, host info, reviews, pricing, and booking CTA.
- **Gap (P2):** iOS has specialized detail views for sub-types: `MeetingRoomDetailView`, `ParkingSpotDetailView`, `RentalGearDetailView`. Android has generic `PropertyDetailScreen` and `ListingDetailScreen` without sub-type specialization.

### Calendar/Availability
- Both platforms use `GET /v1/host/listings/:id/calendar?month=&year=` as source of truth.
- Both support blocking/unblocking time ranges via the backend.
- **Parity: Good** — calendar is functionally equivalent.

### Chat/Inbox
- Both use `GET /v1/inbox/threads`, `GET /v1/inbox/unread-counts`, `POST /v1/inbox/threads/:id/messages`.
- Both have tolerant JSON parsing for varying backend response shapes.
- Both support host/guest mode filtering.
- **Gap (P1):** Android has `ContentModerationCheck.kt` for pre-send message validation; iOS has `ContentModerationService.swift`. Both exist — parity good.
- **Gap (P1):** Android has `ActiveChatTracker` for suppressing chat notifications; iOS does the same via `MessagesView.swift`. Parity good.
- **Major gap:** Real-time (see Gap 3 above).

### Host Dashboard
- Both load bookings, listings, and payout status concurrently with partial-success handling.
- Both show inline error banners on partial failure.
- **Parity: Good** — functionally equivalent.

### Stripe Connect / Payouts
- Both platforms have Stripe Connect onboarding screens (`StripeConnectOnboardingView.swift` / `StripeConnectOnboardingScreen.kt`).
- Both handle `stripe-connect-return` deep links.
- **Parity: Good**.

### Payment / Checkout
- iOS uses `ApplePayManager`, `StripePaymentManager`, and web-based Stripe Checkout.
- Android uses `NativePaymentRepository` with Stripe PaymentSheet and web-based Stripe Checkout via `CheckoutLauncher`.
- Both support the quote-based checkout flow and post-payment navigation.
- **Parity: Good** — platform-specific payment methods are expected.

### Loading / Empty / Error States
- iOS: `PDLoadStateView` (generic wrapper), `PDLoadingStateView`, `PDErrorStateView`, `PDEmptyStateView`, `ShimmerSkeleton`.
- Android: `FullScreenLoading`, `FullScreenError`, `FullScreenEmpty`, `AuthRequiredScreen`, shimmer via `shimmerEffect.kt`, `InlineErrorBanner`.
- **Gap (P2):** Android has an explicit `AuthRequiredScreen` composable for unauthenticated states; iOS doesn't have an equivalent reusable component (handles auth gates inline).

### Push Notifications
- Both use OneSignal + native push (APNs/FCM).
- Both register device tokens with backend via `POST /v1/push-devices`.
- Both have notification routing (`NotificationRouter`) with screen-based and thread-based dispatch.
- Both suppress chat notifications when the thread is active.
- **Gap (P0):** Deregistration on logout (see Gap 2).
- **Gap (P2):** Android `FcmTokenRegistrar` has retry logic with exponential backoff (3 retries); iOS `PushDeviceRegistrar` does not retry on failure.

---

## C. Platform Blockers

### Android-Only Blockers
1. **P0 — Push device deregistration missing on logout.** Users who log out may still receive push notifications meant for the previous session. Trust/privacy issue.
2. **P1 — No Ably realtime for chat.** Android users won't see new messages in real-time while a thread is open; they must manually refresh or rely on push.
3. **P1 — Account restriction UI not built.** Restricted accounts on Android see no explanation — the state is captured but never displayed.
4. **P1 — `forgotPassword` missing in new SessionManager.** Screens migrated to the new auth system cannot offer password reset.

### iOS-Only Blockers
1. **P0 — No 3-step OTP signup flow.** iOS users cannot register through the verified OTP path that Android and web support. This breaks signup parity with the web.
2. **P1 — Deep link handlers are stubs.** Tapping a shared link or push notification that navigates via deep link will parse correctly but do nothing on iOS.
3. **P1 — Image uploads are 2x oversized.** 2048px max vs Android's 1024px leads to slower uploads and potential timeouts on cellular.

### Shared Mobile Blockers
1. **P2 — Tab bar layout inconsistency.** Different tab count and order between platforms creates confusion for cross-platform users.
2. **P2 — Sort options missing on Android search.** iOS exposes sorting by price/rating; Android does not.
3. **P2 — No push registration retry on iOS.** If the initial `POST /push-devices` fails, iOS never retries. Android retries 3x.
