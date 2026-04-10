# Mobile Parity Fixes — Implementation Summary

**Date:** 2026-04-10
**Branch:** `claude/audit-mobile-parity-jwO7z`
**Based on:** `MOBILE_PARITY_AUDIT.md`

---

## Fixed Items

### 1. iOS: 3-Step OTP Signup Flow (P0)
**Files changed:**
- `iOS26/PaceDream/Models/Networking/Endpoints/AuthEndpoint.swift` — Added 6 new endpoints (`signupInitiate`, `signupSendSmsOtp`, `signupVerifyEmailOtp`, `signupVerifySmsOtp`, `signupComplete`, `signupCompleteMobile`)
- `iOS26/PaceDream/Services/AuthService.swift` — Added 6 new methods matching the Android/web 3-step flow: `initiateEmailSignup`, `initiateMobileSignup`, `verifyEmailSignupOtp`, `verifySmsSignupOtp`, `completeEmailSignup`, `completeMobileSignup`

**What it does:** iOS can now perform the same verified OTP signup flow as Android and web. The API endpoints and request/response shapes match exactly.

**QA steps:**
- [ ] Call `initiateEmailSignup(email:)` — verify backend sends a verification code
- [ ] Call `verifyEmailSignupOtp(email:code:)` with the received code — verify success
- [ ] Call `completeEmailSignup(email:password:firstName:lastName:)` — verify JWT returned and session established
- [ ] Repeat for mobile variants (SMS OTP)
- [ ] Verify existing single-step signup still works (backward compatibility)

---

### 2. Android: Push Device Deregistration on Logout (P0)
**Files changed:**
- `pixel/app/.../notification/FcmTokenRegistrar.kt` — Added `deregister()` method that calls `DELETE /v1/push-devices`
- `pixel/app/.../auth/AuthSession.kt` (SessionManager) — `signOut()` now accepts an optional `fcmTokenRegistrar` parameter and calls `deregister()` before clearing tokens

**What it does:** On logout, the app now tells the backend to stop sending push notifications to this device. Matches iOS `PushDeviceRegistrar.deregister(authToken:)`.

**QA steps:**
- [ ] Sign in on Android and verify push notifications arrive
- [ ] Sign out — verify `DELETE /push-devices` is called (check Logcat for `[FcmTokenRegistrar] Push devices deregistered`)
- [ ] Verify no push notifications arrive after logout
- [ ] Sign in again — verify push registration re-occurs

---

### 3. Android: Realtime Chat Message Polling (P1)
**Files changed:**
- `pixel/feature/inbox/.../presentation/ThreadViewModel.kt` — Added 5-second polling loop (`startMessagePolling`, `silentRefreshMessages`) that runs while a thread is open. Merges only new messages without disrupting scroll position.

**What it does:** Bridges the Ably realtime gap. While iOS uses Ably WebSocket subscriptions for live message delivery, Android now polls every 5 seconds. New messages appear automatically without manual refresh.

**QA steps:**
- [ ] Open a chat thread on Android
- [ ] Send a message from another device/web — verify it appears within ~5 seconds on Android
- [ ] Verify polling stops when leaving the thread (check Logcat)
- [ ] Verify polling does not interfere with sending messages (no duplicates)
- [ ] Verify no excessive network usage in background

---

### 4. iOS: Deep Link Navigation (P1)
**Files changed:**
- `iOS26/PaceDream/Utilities/DeepLinkManager.swift` — Replaced all 10 stub handlers with real navigation dispatchers using `NotificationCenter.default.post(name: .pdDeepLinkNavigation, ...)`. Added `NSNotification.Name.pdDeepLinkNavigation`.

**What it does:** Deep link handlers now post structured notifications that can be observed by `ContentView` / `AppRouter` to navigate to the correct screen. Message deep links use the existing `NotificationRouter.openThread(threadId:)` mechanism.

**QA steps:**
- [ ] Open `pacedream://listing/<id>` — verify `pdDeepLinkNavigation` notification posted with type "listing"
- [ ] Open `pacedream://message/<threadId>` — verify thread opens via `NotificationRouter`
- [ ] Open `pacedream://profile/<userId>` — verify `PD_OpenPublicProfile` notification posted
- [ ] Verify Stripe Connect return deep link still works
- [ ] Wire a `NotificationCenter` observer in `ContentView` to complete end-to-end navigation

---

### 5. Image Upload Size Alignment (P1)
**Files changed:**
- `iOS26/PaceDream/Services/CloudinaryUploader.swift` — Changed `maxDimension` from 2048 to 1280, `maxBytes` from 4MB to 460KB, primary JPEG quality from 0.82 to 0.75, fallback quality from 0.6 to 0.5

**What it does:** Both platforms now produce identically-sized uploads (~1280px, ~450KB JPEG), ensuring consistent listing photo quality and upload speed.

**QA steps:**
- [ ] Upload a high-res photo (4000+ px) from iOS — verify it's downscaled to 1280px
- [ ] Compare upload time on cellular between iOS and Android — should be similar
- [ ] Verify listing photos display correctly after upload from both platforms

---

### 6. Android: Auth0 Cancel Not Treated as Error (P1)
**Files changed:**
- `pixel/core/network/.../auth/AuthSession.kt` (legacy) — Changed `onFailure` handler: when `error.isCanceled`, now returns `Result.success(Unit)` instead of `Result.failure(Exception("Login cancelled"))`.

**What it does:** Users who cancel Auth0 login no longer see error toasts/snackbars. Matches iOS silent dismissal behavior.

**QA steps:**
- [ ] Start Auth0 login on Android → tap back/cancel → verify no error shown
- [ ] Complete Auth0 login normally → verify success still works
- [ ] Verify the new `SessionManager` cancel handling (was already correct) still works

---

### 7. Android: `forgotPassword` in New SessionManager (P1)
**Files changed:**
- `pixel/app/.../auth/AuthRepository.kt` — Added `forgotPassword(email)` method + `ForgotPasswordRequest` model
- `pixel/app/.../auth/AuthSession.kt` (SessionManager) — Added `forgotPassword(email)` delegating to repository

**What it does:** Screens using the new auth system can now trigger password reset. Matches iOS `AuthService.forgotPassword(email:)`.

**QA steps:**
- [ ] Call `SessionManager.forgotPassword("test@example.com")` — verify `POST /auth/forgot-password` is sent
- [ ] Verify success message returned
- [ ] Verify error handling for invalid email

---

### 8. Android: Account Restriction UI (P1)
**Files changed:**
- `pixel/app/.../stability/AccountRestrictionScreen.kt` — New composable with Lock icon, restriction message, "Contact Support" button, and "Sign Out" button.

**What it does:** Provides a full-screen overlay for restricted accounts. Matches iOS `AccountRestrictionView.swift`.

**QA steps:**
- [ ] Render `AccountRestrictionScreen` with a test message — verify layout
- [ ] Wire into main navigation: observe `AuthState.Restricted` and show this screen
- [ ] Verify "Sign Out" calls `SessionManager.signOut()`
- [ ] Verify "Contact Support" opens email/web support link

---

## Intentionally Remaining Differences

| Item | Reason |
|------|--------|
| **Tab bar count** (Android: 6 guest tabs, iOS: 5) | Requires product decision on whether Search deserves a tab. Not a bug. |
| **OTP phone login UX** (Android: standalone screens, iOS: in-sheet) | Platform-native navigation pattern. Both reach the same API endpoints. |
| **Ably SDK on Android** | Full Ably integration requires SDK dependency, build system changes, and Ably auth token provisioning. Polling is the safe bridge. |
| **Sort options on Android search** | Requires UI work in FilterScreen. Not a functional blocker. |
| **iOS push registration retry** | iOS single-attempt is adequate when combined with re-registration on each app launch. |

---

## Risks

1. **Polling battery impact (Fix 3):** 5-second polling is conservative but could drain battery on long chat sessions. Monitor battery metrics; consider increasing to 10s or switching to server-sent events.
2. **Deep link observer not wired (Fix 4):** The `pdDeepLinkNavigation` notification is now posted, but `ContentView`/`AppRouter` must add an `onReceive` observer to complete end-to-end navigation. Without this, deep links are dispatched but not consumed.
3. **Push deregistration timing (Fix 2):** `deregister()` is fire-and-forget. If the network call fails, the backend may still send pushes to the logged-out device until the token expires.
4. **SessionManager.signOut signature change (Fix 2):** The optional `fcmTokenRegistrar` parameter maintains backward compatibility, but callers should pass the registrar for full push cleanup.

---

## QA Checklist (Cross-Platform)

- [ ] **Auth:** Sign up via email OTP on iOS → sign in on Android → verify same account
- [ ] **Auth:** Sign up via phone OTP on iOS → verify token returned
- [ ] **Logout:** Logout on Android → verify no push received → sign back in → pushes resume
- [ ] **Chat:** Send message from web → verify Android receives within 5s → verify iOS receives via Ably
- [ ] **Deep links:** Open `pacedream.com/listing/123` on both platforms → verify navigation
- [ ] **Image upload:** Upload same photo from both platforms → verify similar file size and quality
- [ ] **Auth0 cancel:** Cancel Google login on both platforms → verify no error shown
- [ ] **Forgot password:** Trigger reset from both platforms → verify email received
- [ ] **Account restriction:** Simulate 403 from backend → verify both platforms show restriction UI
