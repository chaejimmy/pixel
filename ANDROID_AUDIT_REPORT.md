# PaceDream Android Production Readiness Audit

**Date:** 2026-04-10  
**Auditor:** Automated code audit  
**Scope:** Android codebase only (`app/src/main/kotlin/`)  
**Repository:** chaejimmy/pixel

---

## Executive Summary

The PaceDream Android app is a **Jetpack Compose** marketplace app built with Hilt DI, Kotlin coroutines, and a custom OkHttp networking layer. It has a broadly functional architecture, but contains **several severe issues** that will cause user-visible failures and are likely to trigger **Google Play rejection** under the "broken functionality" policy.

The top concerns are:

1. **GlobalExceptionHandler swallows all main-thread crashes**, masking bugs and leaving the UI in a corrupted state without user feedback.
2. **Dual auth system** (legacy `AuthSession` + new `SessionManager`) creates race conditions where one system is authenticated and the other is not.
3. **Multiple feature screens are static UI shells** with no backend data, no ViewModel, and non-functional buttons (Roommate Finder, Trip Planner).
4. **No automatic token refresh on 401** in the `ApiClient` layer — screens that make authenticated calls will fail silently after token expiry.

---

## A. Top 12 Android Blockers

| # | Severity | Finding | Affected Flow |
|---|----------|---------|--------------|
| 1 | **P0 Critical** | GlobalExceptionHandler swallows main-thread crashes | All screens |
| 2 | **P0 Critical** | Dual auth system race condition | Login / Session restore |
| 3 | **P1 High** | No automatic 401 retry/refresh in ApiClient | All authenticated API calls |
| 4 | **P1 High** | Roommate Finder is a static UI shell with dead buttons | Roommate tab/screen |
| 5 | **P1 High** | Trip Planner screen has no backend integration | Trip Planner |
| 6 | **P1 High** | Notification bell on Home screen is a dead tap | Home header |
| 7 | **P1 High** | EncryptedSharedPreferences fallback silently loses tokens | App launch on some devices |
| 8 | **P2 Medium** | Checkout uses Stripe test key detection for Google Pay environment | Payment |
| 9 | **P2 Medium** | Thread screen has no real-time message updates (polling/WebSocket) | Chat |
| 10 | **P2 Medium** | Edit Listing screen has no image reordering or deletion API | Host listing edit |
| 11 | **P2 Medium** | HomeFeed category filter is client-side only | Home feed |
| 12 | **P2 Medium** | `android.util.Log` calls in production code leak debug info | Security |

---

## B. Detailed Findings

---

### Category: Crash / Launch Stability Issues

---

#### Finding 1: GlobalExceptionHandler Swallows Main-Thread Crashes

- **Severity:** P0 Critical
- **Affected screen/flow:** Every screen in the app
- **Repro steps:**
  1. Any unhandled exception on the main thread (NPE, IllegalStateException, Compose crash)
  2. The handler catches and logs it via Timber
  3. The app continues running in a corrupted state
- **User-visible impact:** The app appears frozen, partially rendered, or shows stale data with no crash dialog and no ability to restart. The user sees a broken screen with no explanation. Google Play crash reporting via Crashlytics stops receiving non-fatal main-thread crashes.
- **Likely root cause:** `GlobalExceptionHandler.install()` in `PaceDreamApplication.onCreate()` replaces the default uncaught exception handler and explicitly does NOT forward main-thread exceptions to the system handler (line 43-47).
- **Files/functions:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/stability/GlobalExceptionHandler.kt:22-52`
  - `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:63`
- **Recommended fix:** Remove the main-thread crash swallowing entirely. Forward all main-thread exceptions to the default handler. Only swallow background thread crashes if absolutely necessary, and even then, log to Crashlytics as non-fatal.

---

#### Finding 2: Dual Auth System Race Condition

- **Severity:** P0 Critical
- **Affected screen/flow:** Login, session restore, any screen checking auth state
- **Repro steps:**
  1. User logs in via `SessionManager.loginWithAuth0()` or `loginWithEmailPassword()`
  2. `SessionManager` updates its `authState` to `Authenticated`
  3. `syncLegacyAuthSession()` is called in a fire-and-forget coroutine (line 83-89 in `AuthSession.kt`)
  4. Before the sync completes, screens observing the legacy `AuthSession.authState` still see `Unauthenticated`
  5. These screens may show login prompts or fail to make authenticated API calls
- **User-visible impact:** After login, some screens may briefly show "Sign in" prompts or fail to load data. Home feed, bookings, and profile may show unauthenticated state despite the user having just logged in.
- **Likely root cause:** Two separate auth state management systems (`com.pacedream.app.core.auth.SessionManager` and `com.shourov.apps.pacedream.core.network.auth.AuthSession`) that are synchronized via a background coroutine. The `HomeFeedViewModel` observes the legacy `AuthSession.authState` while the `AuthFlowSheet` uses the new `SessionManager`.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/core/auth/AuthSession.kt:52-58` (SessionManager)
  - `app/src/main/kotlin/com/pacedream/app/core/auth/AuthSession.kt:83-89` (syncLegacyAuthSession)
  - `app/src/main/kotlin/com/shourov/apps/pacedream/feature/homefeed/HomeFeedViewModel.kt:47` (observes legacy authState)
- **Recommended fix:** Consolidate to a single auth state source. Either make the legacy `AuthSession` a thin wrapper that delegates to `SessionManager`, or remove the legacy system entirely and update all consumers.

---

#### Finding 7: EncryptedSharedPreferences Fallback Silently Loses Tokens

- **Severity:** P1 High
- **Affected screen/flow:** App launch, session restore
- **Repro steps:**
  1. App launches on a device where `EncryptedSharedPreferences` initialization is slow (>10s) or fails
  2. `TokenStorage` falls back to `fallbackPrefs` (plain SharedPreferences file `pacedream_prefs_fallback`)
  3. `fallbackPrefs` has no tokens stored (different file from encrypted prefs)
  4. `hasTokens()` returns false, user appears logged out
  5. On the next launch, if encrypted prefs load successfully, the user's session is restored
- **User-visible impact:** Intermittent "ghost logouts" — the user opens the app, appears logged out, closes and reopens, and is logged back in. This is especially common on Samsung devices and emulators.
- **Likely root cause:** The fallback `SharedPreferences` file (`pacedream_prefs_fallback`) is a different file from the encrypted prefs (`pacedream_secure_prefs`). Tokens are never written to the fallback file.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/core/auth/TokenStorage.kt:50-84`
- **Recommended fix:** When falling back to plain prefs on the main thread, also attempt to copy tokens from encrypted prefs once they're available. Or block briefly (2-3 seconds) with a splash screen extension instead of serving empty state.

---

### Category: Broken Functionality Issues

---

#### Finding 4: Roommate Finder is a Static UI Shell

- **Severity:** P1 High
- **Affected screen/flow:** Roommate Finder screen
- **Repro steps:**
  1. Navigate to Roommate Finder (accessible from navigation)
  2. Screen renders with search bar, filter chips, and a "Post Roommate Listing" button
  3. Search does nothing (local state only, no API call)
  4. Filter chips toggle locally but don't filter any data
  5. "Post Roommate Listing" button has `onClick = { /* TODO: Navigate to post roommate listing */ }`
  6. Advanced filters button has `onClick = { /* TODO: Open advanced filters */ }`
- **User-visible impact:** Entire screen is non-functional. User sees a UI that looks interactive but does nothing. This is a Google Play rejection risk under "broken functionality."
- **Likely root cause:** Screen was scaffolded but never wired to a backend or ViewModel. No ViewModel exists, no API calls, no data loading.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/feature/roommate/RoommateFinderScreen.kt:80` (TODO comment)
  - `app/src/main/kotlin/com/pacedream/app/feature/roommate/RoommateFinderScreen.kt:235` (TODO comment)
- **Recommended fix:** Either remove the Roommate Finder from navigation entirely, or implement backend integration. If the feature isn't ready, gate it behind a feature flag or remove the nav entry.

---

#### Finding 5: Trip Planner Has No Backend Integration

- **Severity:** P1 High
- **Affected screen/flow:** Trip Planner screen
- **Repro steps:**
  1. Navigate to Trip Planner
  2. Screen renders with a form UI
  3. No data loads from any API
  4. Submit/save actions have no backend calls
- **User-visible impact:** Non-functional screen presented as if it works. Google Play rejection risk.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/feature/tripplanner/TripPlannerScreen.kt`
- **Recommended fix:** Remove from navigation or implement backend integration.

---

#### Finding 6: Notification Bell on Home Screen is a Dead Tap

- **Severity:** P1 High
- **Affected screen/flow:** Home feed header
- **Repro steps:**
  1. Open the app to the Home tab
  2. Tap the notification bell icon in the top-right of the "Discover" header
  3. Nothing happens — no navigation, no sheet, no feedback
- **User-visible impact:** A prominent UI element that does nothing. Users will tap it expecting notifications and get no response. This damages trust.
- **Likely root cause:** The bell icon is rendered as a `Surface` with no `clickable` modifier or `onClick` handler. It's purely decorative.
- **Files/functions:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/feature/homefeed/HomeFeedScreen.kt:282-295` (Notification icon in DiscoverHeader)
- **Recommended fix:** Either wire the bell to navigate to `NotificationCenterScreen`, or remove the icon. A `NotificationCenterScreen` already exists in the codebase.

---

#### Finding 11: HomeFeed Category Filter is Client-Side Only

- **Severity:** P2 Medium
- **Affected screen/flow:** Home feed category tabs
- **Repro steps:**
  1. Open Home tab
  2. Tap a category filter (e.g., "Restroom", "Nap Pod", "Gym")
  3. Sections filter based on client-side string matching of `subCategory` or `title`
  4. If backend doesn't return `subCategory` fields, or uses different naming, filtering shows 0 results
- **User-visible impact:** Selecting a category may show empty sections for all categories, making the filter appear broken. The filter does not request category-specific data from the backend.
- **Likely root cause:** `HomeFeedViewModel.applyFilter()` matches against `card.subCategory?.lowercase()` and `card.title.lowercase()`. The backend may not populate `subCategory` consistently.
- **Files/functions:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/feature/homefeed/HomeFeedViewModel.kt:103-117`
- **Recommended fix:** Either send category as a query parameter to the backend API, or validate that the backend consistently returns `subCategory` matching the client-side keywords.

---

### Category: Auth / Session Issues

---

#### Finding 3: No Automatic 401 Retry/Refresh in ApiClient

- **Severity:** P1 High
- **Affected screen/flow:** All screens making authenticated API calls after token expiry
- **Repro steps:**
  1. User is logged in with a valid session
  2. Access token expires (typically after 15-60 minutes)
  3. Any screen makes an authenticated API call
  4. Backend returns 401
  5. `ApiClient` maps it to `ApiError.Unauthorized` and returns failure
  6. The screen shows an error — NO automatic token refresh is attempted
- **User-visible impact:** After the token expires, every screen that loads data will show errors. The user must manually log out and log back in. This happens frequently in normal usage.
- **Likely root cause:** The `ApiClient` does not have an OkHttp `Authenticator` or interceptor that catches 401 responses and attempts a token refresh. Token refresh only happens during bootstrap (on app start). The `SessionManager.bootstrap()` handles 401 by refreshing, but this only runs once at launch.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/core/network/ApiClient.kt` (no authenticator)
  - `app/src/main/kotlin/com/pacedream/app/core/auth/AuthSession.kt:128-134` (bootstrap handles 401, but only runs at launch)
- **Recommended fix:** Add an OkHttp `Authenticator` that intercepts 401 responses, calls `AuthRepository.refresh()`, stores new tokens, and retries the original request. This is standard practice for JWT-based apps.

---

### Category: Listing / Host Issues

---

#### Finding 10: Edit Listing Screen Lacks Image Reordering and Deletion

- **Severity:** P2 Medium
- **Affected screen/flow:** Host listing edit
- **Repro steps:**
  1. Navigate to Host mode > My Listings > Edit a listing
  2. Images are displayed but cannot be reordered via drag-and-drop
  3. Image deletion may not persist to backend (local state only)
- **User-visible impact:** Hosts cannot manage their listing photos effectively after initial creation.
- **Files/functions:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/EditListingScreen.kt`
- **Recommended fix:** Implement image reorder/delete with backend PATCH calls.

---

### Category: Chat / Notification Issues

---

#### Finding 9: Chat Thread Has No Real-Time Message Updates

- **Severity:** P2 Medium
- **Affected screen/flow:** Individual chat thread (ThreadScreen)
- **Repro steps:**
  1. Open a conversation with another user
  2. The other user sends a message
  3. The message does NOT appear until the user manually pulls to refresh or leaves and returns to the thread
- **User-visible impact:** Chat feels broken — users won't see new messages in real time. This is a core feature for a marketplace app.
- **Likely root cause:** `ThreadViewModel` fetches messages once via `loadMessages()` and on manual refresh. There is no WebSocket connection, no polling timer, and no Firebase Realtime Database listener for incoming messages.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/feature/inbox/ThreadViewModel.kt:66-71`
- **Recommended fix:** Implement periodic polling (every 5-10 seconds while the thread is visible) or WebSocket-based real-time messaging. At minimum, add a polling mechanism using `LaunchedEffect` with a delay loop.

---

### Category: Payment / Earnings Issues

---

#### Finding 8: Stripe Test Key Detection for Google Pay Environment

- **Severity:** P2 Medium
- **Affected screen/flow:** Checkout / Payment
- **Repro steps:**
  1. Navigate to checkout with a listing
  2. `CheckoutScreen` configures Google Pay environment based on whether the publishable key starts with `pk_test_`
  3. If the backend returns a test key in production, Google Pay will use test environment
  4. If the backend returns a live key in development, Google Pay will attempt live charges
- **User-visible impact:** In production, if the Stripe key changes format or a test key leaks, Google Pay could silently fail or process real charges in test mode.
- **Likely root cause:** Environment detection is done by string prefix matching (`effect.publishableKey.startsWith("pk_test_")`) instead of a dedicated build config flag.
- **Files/functions:**
  - `app/src/main/kotlin/com/pacedream/app/feature/checkout/CheckoutScreen.kt:103-107`
- **Recommended fix:** Use `BuildConfig.DEBUG` or a dedicated build config field to determine the Google Pay environment, not the Stripe key prefix.

---

#### Finding 12: `android.util.Log` Calls in Production Code

- **Severity:** P2 Medium
- **Affected screen/flow:** App startup, push notification initialization
- **Repro steps:**
  1. Build a release APK
  2. `android.util.Log.i("PushInit", ...)` calls in `PaceDreamApplication.onCreate()` log OneSignal subscription IDs, permission status, and partial FCM tokens to logcat
  3. These are visible to any app with `READ_LOGS` permission or via `adb logcat`
- **User-visible impact:** Leaks push notification tokens and subscription IDs in production logcat. Not a direct crash, but a security concern.
- **Likely root cause:** Debug logging was added with `android.util.Log` instead of `Timber` to bypass the Timber-only-in-debug gate.
- **Files/functions:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:85-86` (OneSignal permission, subscriptionId, token)
  - `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:104` (Auth init log)
  - `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:126-129` (User ID, subscriptionId)
- **Recommended fix:** Replace all `android.util.Log` calls with `Timber.d()` so they are no-ops in release builds.

---

## C. Findings by Category

### Crash / Launch Stability Issues
1. **P0** GlobalExceptionHandler swallows main-thread crashes
2. **P1** EncryptedSharedPreferences fallback silently loses tokens

### Broken Functionality Issues
3. **P1** Roommate Finder is a static UI shell with dead buttons
4. **P1** Trip Planner has no backend integration
5. **P1** Notification bell on Home screen is a dead tap
6. **P2** HomeFeed category filter is client-side only

### Auth / Session Issues
7. **P0** Dual auth system race condition
8. **P1** No automatic 401 retry/refresh in ApiClient

### Listing / Host Issues
9. **P2** Edit Listing screen lacks image reordering/deletion API

### Chat / Notification Issues
10. **P2** Chat thread has no real-time message updates

### Payment / Earnings Issues
11. **P2** Stripe test key detection for Google Pay environment
12. **P2** `android.util.Log` calls leak debug info in production

---

## D. Google Play Review Risks & Must-Fix Before Submission

### Likely Google Play Review Risks

| Risk | Policy | Likelihood | Affected Feature |
|------|--------|-----------|-----------------|
| **Broken functionality** — Roommate Finder screen | Functionality (4.2) | **High** | Screen has TODO comments, no API, no working buttons |
| **Broken functionality** — Trip Planner screen | Functionality (4.2) | **High** | Static UI with no backend |
| **Broken functionality** — Dead notification bell | Functionality (4.2) | **Medium** | Prominent UI element does nothing |
| **App stability** — Crash handler masks errors | Stability (8.1) | **Medium** | App may appear frozen/broken without crashing |
| **Misleading claims** — Features that don't work | Deceptive behavior (3.2) | **Medium** | If listed in store description |

### Must Fix Before Next Submission

**P0 (Ship-blocking — fix before ANY submission):**

1. **Remove or fix GlobalExceptionHandler** — Stop swallowing main-thread crashes. Forward them to the default handler. The app should crash cleanly rather than leave users in a corrupted state.
2. **Consolidate dual auth system** — Eliminate the race condition between `SessionManager` and legacy `AuthSession`. Pick one source of truth.

**P1 (Fix before Google Play submission):**

3. **Remove or complete Roommate Finder** — Either remove from navigation or implement it. A screen with TODO comments and dead buttons WILL trigger rejection.
4. **Remove or complete Trip Planner** — Same as above.
5. **Fix notification bell** — Wire to `NotificationCenterScreen` or remove the icon.
6. **Add 401 auto-refresh to ApiClient** — Without this, every user will hit auth failures within a session. Add an OkHttp Authenticator.
7. **Fix EncryptedSharedPreferences fallback** — Prevent ghost logouts by extending splash screen while prefs initialize.

**P2 (Fix before public launch, not submission-blocking):**

8. **Add chat polling/real-time** — At minimum, poll every 10 seconds while thread is visible.
9. **Fix Google Pay environment detection** — Use BuildConfig instead of key prefix.
10. **Replace `android.util.Log` with `Timber`** — Remove production log leaks.
11. **Fix category filter** — Either make it server-side or validate subCategory consistency.
12. **Complete Edit Listing image management** — Allow reorder/delete with backend persistence.

---

### Positive Notes

The codebase has several well-implemented patterns:

- **Tolerant JSON parsing** throughout (handles camelCase/snake_case, nested vs flat responses)
- **Proper Stripe PaymentSheet integration** using Compose-aware `rememberPaymentSheet`
- **Good image upload handling** with progressive quality reduction and retry on 413
- **Solid notification routing** with comprehensive screen/type mapping
- **Pull-to-refresh and skeleton loading** states on major screens
- **Offline-resilient favorites** with optimistic updates and pending toggle queue
- **Calendar/availability management** is well-implemented with backend as source of truth

---

*End of audit report.*
