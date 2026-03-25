# Android vs iOS Full Parity Audit Report

**Date:** 2025-03-25
**Auditor:** Claude Code (Automated Deep Audit)
**iOS Repo:** chaejimmy/ios26 (source of truth)
**Android Repo:** chaejimmy/pixel (audit target)

---

## 1. OVERALL VERDICT

### Parity Rating: **MEDIUM-HIGH** (approximately 75-80%)

Android has made significant progress toward iOS parity. The product structure, navigation architecture, screen hierarchy, host/guest mode separation, auth flow, and design system are all substantially aligned. However, there are several **functional gaps, incomplete wiring, and placeholder implementations** that prevent Android from being production-ready.

**Summary:**
- Tab structure and navigation architecture match iOS closely
- Auth0 + email/password auth flow is properly implemented
- Host mode switching mirrors iOS AppModeStore behavior
- Design system (PaceDreamColors, PaceDreamTypography, PaceDreamSpacing) is well-aligned
- Several critical action handlers are no-ops or TODOs
- Host inbox messages don't properly navigate to thread detail
- Guest profile logout is not wired
- Host profile edit actions are unimplemented
- Dual package architecture (`com.shourov.apps.pacedream` + `com.pacedream.app`) creates maintenance risk

---

## 2. WHAT MATCHES WELL

### Guest Mode Tab Structure
| iOS Tab | Android Tab | Match |
|---------|-------------|-------|
| Home | Home (HomeFeedScreen) | YES |
| Favorites (heart) | Favorites (WishlistScreen) | YES |
| Bookings (calendar) | Bookings (BookingTabScreen) | YES |
| Messages (message) | Messages (InboxScreen) | YES |
| Profile (person) | Profile (ProfileTabScreen) | YES |

### Host Mode Tab Structure
| iOS Tab | Android Tab | Match |
|---------|-------------|-------|
| Dashboard (chart.bar) | Dashboard (HostDashboardScreen) | YES |
| Earnings (dollarsign) | Earnings (HostEarningsScreen) | YES |
| Post (plus.circle.fill) | Post (HostPostScreen) | YES |
| Inbox (tray.full) | Inbox (HostInboxScreen) | YES |
| Profile (person) | Profile (HostProfileScreen) | YES |

### Auth & Session Management
- **Auth0 Universal Login**: Both platforms use Auth0 with PKCE flow
- **Email/password fallback**: Both support direct email login/signup
- **Token refresh**: Android's SessionManager implements the same bootstrap sequence as iOS (restore tokens -> validate via /account/me -> refresh if 401 -> fallback gracefully)
- **Session persistence**: Android uses EncryptedSharedPreferences (TokenStorage), iOS uses Keychain
- **Non-401 errors preserve session**: Both platforms avoid login loops by keeping tokens on non-auth failures

### Host Mode Management
- Android's `HostModeManager` closely mirrors iOS's `AppModeStore`
- SharedPreferences persistence matches iOS UserDefaults
- `PendingHostRoute` enum matches iOS (LISTINGS, POST_CREATE_LISTING)
- `syncWithBackendHostStatus()` handles returning-host recognition
- Sign-out from host mode without requiring guest switch first

### Navigation Patterns
- Listing detail as modal bottom sheet (matches iOS `.sheet(item:)`)
- Search as full-screen dialog (matches iOS `.fullScreenCover`)
- Tab switching via `TabRouter` (matches iOS notification-based routing)
- Auth gating via `AuthFlowSheet` bottom sheet (matches iOS auth sheet pattern)

### Design System Alignment
- PaceDreamColors, PaceDreamTypography, PaceDreamSpacing tokens defined
- HostAccent color (#10B981 green) for host-specific actions
- Shared component library (HostGroupedCard, HostProfileRow, HostSectionHeader, etc.)

### Host Earnings
- Uses single `/host/earnings/dashboard` endpoint (matches iOS PayoutsService.fetchDashboard)
- Stripe Connect onboarding flow implemented
- Payout request functionality wired
- User-friendly error messages for auth/network failures (iOS parity)

### Host Dashboard
- Loads bookings, listings, payouts concurrently with partial-success handling
- Inline error banners (iOS parity)
- Pull-to-refresh support
- Payout setup prompt when eligible

### Host Profile (Dedicated)
- Separate `HostProfileScreen` (no longer reuses guest profile)
- Correct sections: Identity card, Stats, Host tools, Settings, Switch to guest, Sign out
- Host accent color applied to actions

---

## 3. REMAINING VISUAL / UX MISMATCHES

### 3.1 Guest Profile Screen
- **iOS**: Has tabbed sections (Reviews, Drafts, Partners, Wishlist) within the profile
- **Android**: `ProfileTabScreen` structure needs verification -- the Profile tab serves both modes conditionally via `isHostMode` flag but the iOS guest profile has richer sub-navigation (Reviews, Drafts, Partners tabs within the profile view)
- **Impact**: Medium -- affects profile screen information architecture

### 3.2 Favorites/Wishlist Tab Naming
- **iOS**: Tab is labeled "Favorites" with heart icon in tab bar, but the view title may say "Wishlist" or "Favorites" depending on the variant used (SimpleWishListView vs WishListView)
- **Android**: Uses "Favorites" tab label, "Wishlist" as screen title internally
- **Impact**: Low -- naming is close but should be verified for exact copy parity

### 3.3 Home/Discover Screen Sections
- **iOS HomeView**: Features categories, trending destinations, featured listings with specific section ordering
- **Android HomeFeedScreen**: Has similar content organized via `HomeSectionKey` enum (SPACES, etc.) with "See all" navigation
- **Impact**: Medium -- section ordering and exact content may differ; needs visual verification

### 3.4 Host Dashboard Layout
- **iOS HostDashboardView**: Specific section ordering with stat cards, CTA rows, quick actions
- **Android HostDashboardScreen**: Has similar structure but the exact card layout, stat card arrangement, and CTA positioning may differ
- **Impact**: Medium -- visual differences in card layout and stat presentation

### 3.5 Host Earnings Screen Layout
- **iOS HostEarningsView**: Uses HostDS design tokens, specific section structure (balance, payouts list, transaction history, "How payouts work" section)
- **Android HostEarningsScreen**: 45KB file with comprehensive implementation; visual alignment needs verification
- **Impact**: Medium -- screen exists and is functional, but visual parity needs QA

### 3.6 Bottom Navigation Icon Styling
- **iOS**: Uses SF Symbols (system icons) with specific styling
- **Android**: Uses custom drawable icons (`ic_home`, `ic_favorite`, `ic_booking`, `ic_notifications`, `ic_profile`)
- **Impact**: Low -- functional parity exists; icon style differences are expected cross-platform

---

## 4. BROKEN OR INCOMPLETE ANDROID FEATURES

### 4.1 [P0 - RELEASE BLOCKING] Guest Profile Logout Not Wired

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/navigation/DashboardNavigation.kt`
**Line:** `onLogoutClick = { /* Handle logout */ }`

**Expected (iOS):** Tapping "Log Out" in guest profile calls `AuthService.logout()`, clears tokens, resets to unauthenticated state
**Actual (Android):** The `onLogoutClick` callback is a no-op comment. Users cannot sign out from the guest profile.

**Root cause:** The callback was never connected to `SessionManager.signOut()`.

### 4.2 [P0 - RELEASE BLOCKING] Host Profile Edit Actions are TODOs

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/navigation/HostNavigationGraph.kt`

```kotlin
onEditProfileClick = { /* TODO: open edit profile sheet */ },
onEditPhotoClick = { /* TODO: open photo picker */ },
```

**Expected (iOS):** Edit profile and edit photo actions open their respective sheets/pickers
**Actual (Android):** Both callbacks are no-ops with TODO comments. Host cannot edit their profile or photo.

### 4.3 [P1 - FUNCTIONAL] Host Inbox Thread Navigation Not Wired

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/navigation/HostNavigationGraph.kt`

```kotlin
composable(HostScreen.Inbox.route) {
    HostInboxScreen(
        onThreadClick = { threadId -> },  // NO-OP
        messagesContent = {
            InboxScreen(
                onThreadClick = { threadId -> }  // NO-OP
            )
        }
    )
}
```

**Expected (iOS):** Tapping a message thread in host inbox navigates to the chat/thread detail screen
**Actual (Android):** Both `onThreadClick` handlers are empty lambdas. Host cannot open any message thread from the inbox.

**Root cause:** Navigation to thread screen was implemented for guest mode (DashboardNavigation uses `ThreadScreen`) but was never wired in the host navigation graph.

### 4.4 [P1 - FUNCTIONAL] Host Inbox Notifications Tab Shows Static Placeholder

**File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/HostInboxScreen.kt`

The Notifications tab in host inbox always shows a static "No notifications yet" placeholder. There's no ViewModel or API call to fetch actual notifications.

**Expected (iOS):** Host inbox notifications tab shows real notification data
**Actual (Android):** Static placeholder only

### 4.5 [P1 - FUNCTIONAL] Feature Module Source Code Split

The `feature/host` gradle module only contains `build.gradle.kts` -- no source files. All host feature code lives in `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/`. Similarly:

- `feature/home` -- no `src` directory found (404 on GitHub)
- `feature/host` -- only build.gradle.kts
- `feature/guest` -- only AndroidManifest.xml

Meanwhile `feature/booking`, `feature/wishlist`, `feature/inbox`, `feature/chat`, `feature/search` have actual source code in their `src/main/kotlin` or `src/main/java` directories, but these are **separate from** the app module's corresponding packages (`com.pacedream.app.feature.bookings`, `com.shourov.apps.pacedream.feature.booking`).

**Impact:** Potential duplicate or conflicting implementations. Need to verify which module's code is actually used at runtime.

### 4.6 [P2 - POLISH] Split Booking Feature Parity

- **iOS**: Has comprehensive `SplitInviteView` with participant gathering, payment tracking, progress visualization, join experience
- **Android**: Has `SplitBookingListScreen` and `SplitBookingScreen` in `com.pacedream.app.feature.splitbooking` -- feature exists but depth of implementation needs verification against iOS's richer split booking flow

### 4.7 [P2 - POLISH] Blog, Trip Planner, Bidding Features

Android has screens for these at `com.pacedream.app.feature.{blog, tripplanner, bidding}`. These appear to be Android-only additions or web parity features. Need to verify:
- Whether iOS has equivalent screens
- Whether Android implementations are complete or placeholder

---

## 5. BACKEND / DATA / AUTH ISSUES

### 5.1 Auth Token Exchange

**Android AuthSession.kt** implements:
- `POST /v1/auth/auth0/callback` -- Auth0 token exchange
- `POST /v1/auth/login/email` -- Email login (body: `{ method, email, password }`)
- `POST /v1/auth/signup/email` -- Email signup (body: `{ email, firstName, lastName, password, dob, gender }`)
- `GET /v1/account/me` -- Profile fetch with fallbacks
- `POST /v1/auth/refresh-token` -- Token refresh with proxy fallback

**iOS AuthService.swift** implements:
- Auth0 Universal Login (ASWebAuthenticationSession, Authorization Code + PKCE)
- `POST /auth/login-email` -- Email login
- `POST /auth/signup-email` -- Email signup
- `GET /account/me` -- Profile fetch with fallbacks (`/users/get/profile`, `/user/get/profile`)

**Potential Issue:** Android uses `/v1/` prefix on API paths while iOS appears to use paths without the prefix. This needs verification -- if the backend expects different paths, Android may be hitting 404s on some endpoints.

### 5.2 Host API Endpoints

**Android HostApiService.kt** (19KB) and **HostRepository.kt** (18KB) appear to have comprehensive host API coverage. The single `/host/earnings/dashboard` endpoint usage matches iOS's PayoutsService pattern.

**Status:** Appears aligned but field mapping should be verified against actual API responses.

### 5.3 User Profile Parsing Tolerance

Both platforms implement tolerant JSON parsing:
- Multiple key formats (`_id`/`id`, `firstName`/`first_name`, `profileImage`/`profile_image`/`avatar`)
- Multiple nesting levels (`data.token`, `data.data.token`, `token`)

**Status:** Good parity -- Android's parsing is defensive and handles backend inconsistencies.

### 5.4 Token Shape Validation

Android validates JWT shape (3 dot-separated parts) before accepting tokens. iOS uses Auth0's CredentialsManager for token management.

**Status:** Android's approach is appropriate for its architecture.

### 5.5 Cached User Fallback

Both platforms preserve cached user data when non-auth errors occur, preventing login loops. Android explicitly calls `loadCachedUser()` on parse failures.

**Status:** Good parity.

---

## 6. RELEASE-BLOCKING ISSUES

| # | Issue | Severity | Screen |
|---|-------|----------|--------|
| 1 | Guest profile logout not wired (`onLogoutClick = { }`) | **P0 - BLOCKER** | Guest Profile |
| 2 | Host profile edit/photo actions are TODOs | **P0 - BLOCKER** | Host Profile |
| 3 | Host inbox thread navigation is no-op | **P1 - CRITICAL** | Host Inbox |
| 4 | Host inbox notifications tab is static placeholder | **P1 - CRITICAL** | Host Inbox |
| 5 | Potential duplicate code across feature modules and app module | **P1 - RISK** | Architecture |
| 6 | API path prefix mismatch (`/v1/` vs non-prefixed) needs verification | **P1 - RISK** | Network Layer |

---

## 7. PRIORITIZED FIX LIST

### P0 -- Must Fix Now (Release Blockers)

1. **Wire guest profile logout**
   - In `DashboardNavigation.kt`, connect `onLogoutClick` to `sessionManager.signOut()` + navigate to auth screen
   - Reference: iOS `SettingsHomeView.swift` logout action

2. **Implement host profile edit actions**
   - In `HostNavigationGraph.kt`, wire `onEditProfileClick` to navigate to personal info/edit screen
   - Wire `onEditPhotoClick` to photo picker
   - Reference: iOS `HostHomeView.swift` edit actions

### P1 -- Should Fix Next (Critical Functional)

3. **Wire host inbox thread navigation**
   - In `HostNavigationGraph.kt`, connect `onThreadClick` to navigate to `ThreadScreen` with threadId
   - The `ThreadScreen` composable already exists in DashboardNavigation -- reuse it

4. **Implement host notifications tab**
   - Create NotificationsViewModel for host inbox notifications tab
   - Or reuse the existing `NotificationCenterScreen` from guest mode
   - Reference: iOS host inbox notifications segment

5. **Verify feature module vs app module code**
   - Audit which implementations are active: `feature/booking/src/...` vs `app/.../feature/booking/`
   - Remove or consolidate duplicate code
   - Ensure gradle dependencies reference correct modules

6. **Verify API path consistency**
   - Confirm whether backend expects `/v1/` prefix or not
   - Test all auth endpoints end-to-end
   - Test profile fetch fallback paths

### P2 -- Polish Later

7. **Guest profile sub-tabs parity**
   - iOS has Reviews, Drafts, Partners, Wishlist tabs within profile
   - Android's ProfileTabScreen may not have equivalent sub-navigation

8. **Split booking feature depth**
   - Verify Android split booking matches iOS's participant management, payment tracking, progress visualization

9. **Home feed section ordering**
   - Verify exact section ordering matches iOS HomeView
   - Verify category chips and browse-by-type parity

10. **Visual QA pass**
    - Compare spacing, card corner radii, shadow depths
    - Verify empty/loading/error states match iOS patterns
    - Host vs guest color system verification

11. **Remove stale files from repo root**
    - `hs_err_pid*.log` (JVM crash logs) committed to repo
    - `replay_pid*.log` files committed to repo
    - `local.properties` committed (contains local SDK paths)
    - `.DS_Store` committed

---

## 8. FILES / AREAS TO TOUCH

### Android Files Requiring Changes

| Priority | File | Issue |
|----------|------|-------|
| P0 | `app/.../navigation/DashboardNavigation.kt` | Wire logout handler |
| P0 | `app/.../feature/host/navigation/HostNavigationGraph.kt` | Wire edit profile, edit photo, thread navigation |
| P1 | `app/.../feature/host/presentation/HostInboxScreen.kt` | Notifications tab needs real data |
| P1 | `app/.../feature/host/navigation/HostNavigationGraph.kt` | Connect onThreadClick to ThreadScreen |
| P1 | `app/.../core/network/` | Verify API path prefixes |
| P2 | `app/.../feature/profile/` | Guest profile sub-tabs parity |
| P2 | Root directory | Remove committed crash logs and local.properties |

### iOS Reference Files

| Screen | iOS File |
|--------|----------|
| Root Navigation | `PaceDream/Views/Root/RootTabView.swift` |
| Home | `PaceDream/Views/Main/Home/HomeView.swift` |
| Wishlist | `PaceDream/Views/Main/Bookings/WishList/WishListView.swift` |
| Bookings | `PaceDream/Views/Main/Profile/ModernBookingsTabView.swift` |
| Messages | `PaceDream/Views/Main/Inbox/Messages/MessagesEntryView.swift` |
| Guest Profile | `PaceDream/Views/Main/Profile/` |
| Host Dashboard | `PaceDream/Views/HostMode/HostDashboardView.swift` |
| Host Earnings | `PaceDream/Views/HostMode/HostEarningsView.swift` |
| Host Profile | `PaceDream/Views/Main/Profile/Host/HostHomeView.swift` |
| Post/Create Listing | `PaceDream/Views/Main/Home/PostStartView.swift` |
| Settings | `PaceDream/Views/Main/Profile/Settings/SettingsHomeView.swift` |
| Mode Switching | `PaceDream/App/AppModeStore.swift` |
| Auth | `PaceDream/Services/AuthService.swift` |
| Content View | `PaceDream/Views/ContentView.swift` |

---

## 9. ARCHITECTURAL OBSERVATIONS

### Dual Package Structure
Android has two package roots in the app module:
- `com.shourov.apps.pacedream` -- Original developer's code (navigation, host features, search, booking, inbox, profile)
- `com.pacedream.app` -- Newer additions (auth, network, settings, blog, reviews, trip planner, split booking, checkout)

This split is not inherently broken but creates risk:
- Naming confusion
- Import complexity
- Potential for conflicting implementations
- Harder for new developers to navigate

**Recommendation:** Consider a phased migration to unify under `com.pacedream.app` namespace.

### Feature Module Architecture
The multi-module structure (`feature/booking`, `feature/host`, `feature/wishlist`, etc.) follows Now in Android patterns, but several modules appear to be empty shells or have code that duplicates app-module implementations. This should be cleaned up.

### Code Quality
- Extensive use of `// iOS parity:` comments throughout Android code -- helpful for tracking alignment
- Proper use of Hilt dependency injection
- Proper use of Jetpack Compose state management (StateFlow + collectAsStateWithLifecycle)
- Coil for image loading (standard Android approach)
- Timber for logging
- Pull-to-refresh support on key screens

---

## 10. ANSWER TO AUDIT QUESTIONS

### A. Does Android now feel like the same product as iOS?
**Mostly yes.** The navigation structure, screen hierarchy, host/guest separation, and core flows are aligned. A user switching between platforms would recognize the same product. However, several broken handlers (logout, edit profile, thread navigation) would immediately surface as issues.

### B. Which screens still visually differ in a meaningful way?
- Guest Profile (missing sub-tabs)
- Home Feed (section ordering needs verification)
- Host Dashboard (card layout differences)
- Host Earnings (visual layout needs QA)

### C. Which flows still behave differently?
- Guest logout (doesn't work on Android)
- Host profile editing (doesn't work on Android)
- Host inbox message threading (doesn't navigate on Android)
- Host notifications (placeholder on Android)

### D. Which Android features are broken, incomplete, stale, or not wired correctly?
- Guest logout: no-op handler
- Host edit profile: TODO
- Host edit photo: TODO
- Host inbox thread navigation: no-op
- Host notifications tab: static placeholder
- Feature module consolidation needed

### E. Which issues are cosmetic vs functional vs release-blocking?
- **Release-blocking (P0):** Guest logout, host edit profile/photo
- **Functional (P1):** Host inbox threading, notifications, API path verification
- **Cosmetic (P2):** Section ordering, sub-tab parity, visual polish, stale repo files
