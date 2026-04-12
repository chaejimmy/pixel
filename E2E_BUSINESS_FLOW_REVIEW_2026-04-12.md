# PaceDream — End-to-End Business Flow Review

**Date:** 2026-04-12
**Scope:** All four repos — `pacedream-platform` (web + API), `iOS26` (iOS), `pixel` (Android), `fulladmin` (admin)
**Baseline:** Delta from the 2026-03-18 Launch Readiness Report
**Question:** Can the business be operated end-to-end today?

---

## 1. TL;DR

| Surface | Can operate E2E today? | Blocker count | Effort to green |
|---|---|---|---|
| Web (guest + host) | **YES** | 0 code / 1 runtime flag | Set `NEXT_PUBLIC_ENABLE_WEB_MESSAGING=true` |
| Backend API | **YES** | 0 | — |
| Admin dashboard (ops) | **YES** | 0 | — |
| iOS app | **CONDITIONAL YES** | 1 P0 config | <1 hour (Auth0 prod tenant + Stripe live keys + Apple Pay domain) |
| Android app | **NO** | 4 P0s | 1–2 days code work |

**Overall verdict: You can launch and operate the business end-to-end today on web + iOS (after a <1h iOS config pass). Android is still behind and cannot complete a customer booking E2E — treat it as beta and keep it out of the public rollout until the four P0s are fixed.**

All four clients correctly point at the shared production backend `https://pacedream-backend.onrender.com/v1/`, and the admin panel shares the same MongoDB as the platform backend, so listing approval, refunds, and payouts flow cleanly across surfaces.

---

## 2. Verification of 2026-03-18 audit items

### `pacedream-platform` (web + backend)

| # | Issue | Status | Evidence |
|---|---|---|---|
| 1 | `mockMessageService` imports in `ChatComponent.tsx` / `messages/page.tsx` | **FIXED** | Both files now import real `messageService` from `@/services/messageService`; backend `/v1/chat/inbox` wired at `backend/src/routes/chat_route.js:52` |
| 2 | Competing `next.config.js` backend URLs | **FIXED** | Root `next.config.js:56` uses single `pacedream-backend.onrender.com/v1/` |
| 3 | `dangerouslyAllowSVG` + `ignoreBuildErrors` | **PARTIALLY FIXED** | Root config now `ignoreBuildErrors: false`, SVG removed with "SECURITY FIX" comment at `next.config.js:62-64`. **Still open:** `frontend/next.config.js:78` has conditional `ignoreBuildErrors: process.env.ENFORCE_STRICT_TYPES !== '1'` |
| 4 | Stale Firebase redirect URI | **FIXED** | `backend/src/config/config.json:5` now points to `https://www.pacedream.com/api/auth/callback/apple` |
| 5 | Committed API keys (Google Maps, SendGrid, Algolia) | **FIXED** | Templates in `.env.example`, no keys found in committed source |

### `iOS26`

| # | Issue | Status | Evidence |
|---|---|---|---|
| 1 | **Auth0 dev tenant in Release config** | **STILL OPEN — P0** | `PaceDream/Configs/Release.xcconfig:27` still hardcodes `AUTH0_DOMAIN = dev-ygmeh25wmmszid8u.us.auth0.com` — ships dev tenant in production builds |
| 2 | Stale `config.json:5` Firebase URI | **FIXED** (platform side) | Now `https://www.pacedream.com/api/auth/callback/apple` |
| 3 | Dead notification bell `Button(action: {})` in `HomeView.swift:3059` | **FIXED** | No empty-action button found in `HomeView.swift` |
| 4 | Facebook credentials in `Info.plist` | **FIXED** | None found in `.plist`/`.xcconfig` |
| 5 | "Totel" branding remnants (`SettingView.swift:276`, `PaymentView.swift:95`, `TotelApp.swift`) | **FIXED** | Only cosmetic file-header comments remain; no user-visible strings |
| 6 | Apple Pay domain verification | **PARTIAL** | Code complete (`ApplePayManager.swift`); Stripe Dashboard registration + `.well-known/apple-developer-merchantid-domain-association` still a runtime step |
| 7 | 332 TODO/FIXME | **FIXED** | Down to **13 TODOs**, all feature-related, none critical |

### `fulladmin`

| # | Issue | Status | Evidence |
|---|---|---|---|
| 1 | RBAC only ~50% route coverage | **FIXED (~95%)** | 267/282 admin routes use `requirePermission()`; remaining are public (auth login, public reviews) |
| 2 | No Joi/Yup input validation schemas | **STILL OPEN** | Ad-hoc manual checks only (e.g. `authController.js:109-114`, `adminRefundController.js:152-162`) |
| 3 | 171 `console.log` in production code | **PARTIAL (178 now)** | Slight regression — still elevated in `index.js`, `createAdmin.js`, some controllers |
| 4 | No refresh token rotation / blacklist | **STILL OPEN** | `authController.js:933-978` reuses same refresh token on refresh; logout just clears cookies |
| 5 | Redux DevTools in production | **FIXED** | `store.js:11-12` conditionally disabled when `NODE_ENV === 'production'` |
| 6 | ~20% test coverage | **STILL OPEN** | 9 test files, no E2E flow coverage for listingManagement / dispute / refund / payout controllers |

---

## 3. End-to-end flow coverage per surface

### 3a. Web (`pacedream-platform`) — **READY**

| Flow | Status | Notes |
|---|---|---|
| Guest: register (Auth0 + phone OTP via Twilio Verify) → search (Algolia) → listing detail → booking hold → Stripe Checkout → webhook confirms → "my bookings" → message host → leave review | **WORKS** | Messaging gated by `NEXT_PUBLIC_ENABLE_WEB_MESSAGING` — must enable in prod env |
| Host: register → create listing via wizard (`src/app/(listing-detail)/create-listing/`) → admin approval → publish (`listings_routes.js:15 PATCH /:id/publish`) → booking notifications → Stripe Connect payout (`Host/host_payouts_route.js`) | **WORKS** | — |
| Refund: guest cancels → 24h full-refund policy or prorated (`refunds_route.js:14`) → `stripe.refunds.create()` → admin visibility → `booking.refundId` idempotency | **WORKS** | Minor: orphaned-webhook reconciliation documented in `RUNBOOK_PAYMENT_RECONCILIATION.md` |

### 3b. Backend API — **READY**

All critical routes exist, authenticated, and audit-logged. Webhook path (`backend/src/routes/stripeWebhook.js`) correctly handles `payment_intent.succeeded` → booking creation.

### 3c. iOS (`iOS26`) — **CONDITIONAL READY**

| Flow | Status |
|---|---|
| Auth (Auth0 + phone OTP + Google/Apple/Facebook) | **WORKS** (`AuthService.swift:35`, `OTPPhoneLoginService.swift:65`) |
| Search / browse | **WORKS** — `SearchService.swift:34` hits real API; `HomeView.loadMockData()` (line 873–878) is intentionally empty (no silent fallback) |
| Booking + Stripe PaymentSheet | **WORKS** — `BookingService.swift:25`, `NativePaymentService` → `/v1/native-payment/quote` server-side |
| Payment Intent | **WORKS** — `PaymentService.swift:25`, keys injected via `Secrets.xcconfig` |
| Messaging (Ably) | **WORKS** — `MessagingService.swift:70+`, real backend |
| Host mode + listing creation | **WORKS** — `HostListingsService.swift:49` |
| Reviews | **WORKS** — real API |

**Blockers before production submit:**
1. **P0:** Update `Release.xcconfig:27` `AUTH0_DOMAIN` from dev tenant to production tenant / `auth.pacedream.com`
2. Inject Stripe live keys via `Secrets.xcconfig`
3. Register Apple Pay merchant domain in Stripe Dashboard + deploy `.well-known/apple-developer-merchantid-domain-association`

Total effort: **<1 hour of configuration work**, zero code changes.

### 3d. Android (`pixel`) — **NOT READY**

Core architecture is at ~75% iOS parity; user-facing completeness ~60%. The repo was **not** in the 2026-03-18 audit, so this is a fresh finding.

| Flow | Status | Evidence |
|---|---|---|
| Auth (Auth0 + email) | **WORKS** | `AuthSession.kt:46-49` → `/v1/auth/auth0/callback`, `/login/email`, `/signup/email` |
| Search / browse | **WORKS** | `SearchScreen.kt` — live API with filters, 14 category tabs |
| Listing detail | **PARTIAL** | Missing interactive map, "show all photos", house rules, cancellation policy |
| Booking flow | **WORKS** (up to payment) | `CheckoutScreen.kt` + `CheckoutViewModel.kt` |
| Stripe payment | **WORKS** | Real `PaymentSheet`, Stripe SDK v22.5.0 |
| **Post-booking confirmation** | **STUB — P0** | `ConfirmationScreen.kt` exists but is not wired after successful payment. No success screen / booking ID display. |
| **Guest logout** | **BROKEN — P0** | `DashboardNavigation.kt` — `onLogoutClick = { }` is a no-op; `SessionManager.signOut()` never called |
| **Reviews UI** | **STUB — P0** | Listing detail shows "Reviews preview coming soon" placeholder |
| **Guest/logged-out browsing** | **MISSING — P0** | Forces onboarding before any home content (iOS allows 2-tab unauth mode) |
| Messaging/chat (Ably) | **WORKS** — but host inbox `onThreadClick` is no-op in `HostNavigationGraph.kt` | — |
| Host mode | **PARTIAL** | Dashboard, earnings, create-listing (8–9 steps) all present; but **edit profile + profile photo are TODO** in `HostNavigationGraph.kt` |
| Notifications (FCM) | **WORKS** | `PaceDreamFirebaseMessagingService.kt` — FCM token registered to `/v1/notifications/register-device`, POST_NOTIFICATIONS permission guarded for Android 13+. Actually **more polished than iOS** (5-tab filter vs date-grouped) |
| Wishlist | **WORKS** | — |

**P0 blockers (real customers can't complete an E2E booking):**
1. Guest logout is a no-op — users can never sign out
2. No post-booking confirmation screen after Stripe success
3. Reviews are a placeholder — guests cannot evaluate listings before booking
4. Forced login before any browsing (no guest mode)

**Build status:** Clean compile. The three committed JVM crash logs (`hs_err_pid*.log`) are local-machine `-Xmx6g` OOM events, not production runtime issues — but they should be moved to `.gitignore`.

**Other hygiene:**
- Release signing still uses debug keystore (P1)
- No certificate pinning (P1)
- Single `prod` flavor — no staging (P1)
- `secrets.properties.template` correctly points at `https://pacedream-backend.onrender.com/v1/`

### 3e. Admin (`fulladmin`) — **READY**

All 12 operational flows are wired end-to-end (frontend → route → controller → model → audit log):

| # | Flow | Status |
|---|---|---|
| 1 | Admin login (5-failure lockout, 15m access / 7d refresh) | **WORKS** — `authController.js:81-99`, `adminLoginLimiter` at `authRoute.js:22` |
| 2 | User mgmt (list, view, suspend/ban, delete) | **WORKS** — all routes use `requirePermission('users.*')` |
| 3 | **Listing approval queue** | **WORKS** — `listingManagementController.js:298-440` `getReviewQueue()` + `reviewListing()` at line 1575 updates status, audit-logs, and calls `notifyHostReviewResult()`. **Critical path for host operations — confirmed working.** |
| 4 | Booking mgmt (list, filter, status history) | **WORKS** |
| 5 | Dispute mgmt (open, investigate, resolve, notify) | **WORKS** — `disputeController.js:193-250` |
| 6 | Refund processing (Stripe refund → ledger → audit → notify host) | **WORKS** — `adminRefundController.js:139-250` calls `stripe.refunds.create()`, writes `PaymentHistory REFUND` record |
| 7 | Payout mgmt (hold, release, retry, mark-for-review) | **WORKS** — `adminPayoutRoutes.js:11-22` |
| 8 | Content moderation (reports, actions) | **WORKS** |
| 9 | Image moderation (manual + batch + webhook) | **WORKS** — `imageModerationRoutes.js:13-25` |
| 10 | Analytics dashboard (real MongoDB aggregations over User, Property, PaymentHistory, Booking) | **WORKS** — `analyticsController.js:16-83`, no stubs |
| 11 | System settings (feature flags, fee schedule) | **WORKS** — changes persist and take effect on platform backend via shared DB |
| 12 | Audit log browser (search / stats / event types) | **WORKS** — `auditLogRoutes.js:10-12`, 61+ event types |

**Database coupling:** **SHARED.** `apps/backend/config/db.js:9` reads `PACEDREAM_MONGODB_URL` first, same URL as platform backend (documented in `.env.example:18-19`). This is what makes listing approval, refunds, and payouts flow cleanly across surfaces — if they were separate DBs, the admin panel could not approve listings created on the platform.

**No stub routes found.** All `res.json({success: true})` sites carry real `data:` payloads.

---

## 4. Cross-repo consistency checks

| Check | Result |
|---|---|
| All four clients point at `https://pacedream-backend.onrender.com/v1/` | **YES** (verified in platform `next.config.js:56`, iOS `Release.xcconfig:37-38`, Android `secrets.properties.template:24-25`, admin shared DB) |
| All four clients use consistent Auth0 config | **NO** — iOS still on dev tenant in Release (see P0) |
| Platform + admin share MongoDB | **YES** — same `PACEDREAM_MONGODB_URL` |
| Stripe webhook endpoint configured | Code ready; runtime config step |
| Firebase / Apple redirect URI consistent | **YES** — `config.json:5` now `https://www.pacedream.com/api/auth/callback/apple` |

---

## 5. Blocker summary (what to actually fix)

### P0 — must fix before production

| # | Repo | File:Line | Issue | Effort |
|---|---|---|---|---|
| 1 | iOS26 | `PaceDream/Configs/Release.xcconfig:27` | Auth0 dev tenant in Release build | 5 min |
| 2 | iOS26 | Stripe Dashboard + `public/.well-known/apple-developer-merchantid-domain-association` | Apple Pay domain unregistered | 30 min |
| 3 | iOS26 | `Secrets.xcconfig` | Stripe live keys not injected | 15 min |
| 4 | pixel | `app/.../DashboardNavigation.kt` `onLogoutClick = { }` | Guest logout is a no-op | 15 min |
| 5 | pixel | Checkout flow → `ConfirmationScreen.kt` wiring | No post-booking confirmation screen | 2 hours |
| 6 | pixel | Listing detail / ReviewsScreen integration | Reviews are a placeholder | 4–6 hours |
| 7 | pixel | Home / onboarding gating | Force-login blocks guest browsing (iOS allows it) | 2–4 hours |
| 8 | platform (runtime) | Vercel env | Set `NEXT_PUBLIC_ENABLE_WEB_MESSAGING=true` to un-gate messaging | 1 min |

**P0 subtotal: <1 hour on iOS + runtime config, ~1–2 days on Android.**

### P1 — before full public launch

- `pacedream-platform/frontend/next.config.js:78` — align `ignoreBuildErrors` to match root
- `fulladmin` — add Joi/Yup schema validation on listing review and dispute resolution payloads
- `fulladmin` — implement refresh token rotation + blacklist on logout
- `fulladmin` — clean up 178 `console.log` statements (replace with structured logger)
- `iOS26` — certificate pinning, Apple Pay end-to-end device test
- `pixel` — release signing with real keystore, certificate pinning, fix host profile edit/photo, implement host inbox thread navigation, add staging flavor
- `pixel` — add `hs_err_pid*.log` to `.gitignore`

---

## 6. Can you operate the business end-to-end today?

**Yes — on Web + iOS + Admin**, after the <1 hour iOS config pass. All critical flows (onboarding → discovery → booking → payment → host payout → admin approval → refund → audit) are wired to real services with shared database, server-side Stripe verification, and a working admin operational console.

**No — on Android** yet. The Kotlin/Compose app has the plumbing but is missing 4 user-visible pieces that would block any real customer from completing a booking and coming back for a second one. Keep Android out of the first public rollout; plan 1–2 days to close the gap, then bring it to parity.

**Recommended launch shape:**
1. Day 0 (today): Set `NEXT_PUBLIC_ENABLE_WEB_MESSAGING=true`; fix iOS Auth0 tenant + Stripe live keys + Apple Pay domain.
2. Day 1: Web public pilot + iOS TestFlight.
3. Day 2–3: Android P0 sprint (logout, confirmation screen, reviews UI, guest browsing).
4. Day 4: Android internal dogfood build.
5. Day 5+: Android public rollout after smoke test.

Admin ops team is ready to approve listings, process refunds, manage payouts, and respond to disputes on Day 0. Shared-DB coupling means any action taken in admin is immediately visible to the platform, iOS, and Android clients.

---

*Report compiled from live code inspection across all four repos on 2026-04-12. File:line references are authoritative; verdict is a delta from the 2026-03-18 launch readiness baseline.*
