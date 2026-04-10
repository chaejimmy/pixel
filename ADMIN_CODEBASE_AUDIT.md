# PaceDream Admin Codebase Audit Report

**Date:** 2026-04-10
**Repository:** `chaejimmy/pixel` (branch: `claude/audit-admin-codebase-sj4rx`)
**Scope:** Admin codebase audit — production readiness for operating a live marketplace

---

## A. Executive Summary: Top 10 Admin Blockers

**CRITICAL FINDING: There is no admin codebase in this repository.**

The `pixel` repository contains **exclusively** the PaceDream Android mobile application — a Kotlin/Jetpack Compose marketplace app for guests and hosts. After exhaustive search across every file, directory, module, and commit:

| # | Blocker | Severity |
|---|---------|----------|
| 1 | **No admin panel exists in this repo** — zero screens, zero routes, zero modules | CRITICAL |
| 2 | **No admin authentication system** — no admin login, no admin sessions | CRITICAL |
| 3 | **No role-based access control** — no admin roles, no permission gates | CRITICAL |
| 4 | **No admin dashboard** — no operator metrics, no platform health view | CRITICAL |
| 5 | **No notification bell for operators** — no alerts for new users, listings, reports, KYC | CRITICAL |
| 6 | **No listing moderation tools** — no approve/reject UI for operator review | CRITICAL |
| 7 | **No user management tools** — no ban, suspend, or user detail views for operators | CRITICAL |
| 8 | **No report review workflow** — no way for operators to triage user reports | CRITICAL |
| 9 | **No verification/KYC review tools** — no way for operators to review ID submissions | CRITICAL |
| 10 | **No audit log** — no record of operator actions, no accountability trail | CRITICAL |

**Bottom line: PaceDream cannot be operated as a live marketplace with this repository alone. A separate admin panel must exist elsewhere or must be built from scratch.**

---

## B. Detailed Findings

### Finding 1: No Admin Panel Exists in This Repository

- **Severity:** CRITICAL — absolute launch blocker
- **Screen/Feature:** All admin functionality
- **Expected behavior:** A dedicated admin web panel (or in-app admin module) with login, dashboard, moderation queue, user management, report review, verification review, and audit logs.
- **Actual behavior from code:**
  - `settings.gradle.kts` lists 30+ modules — none are admin-related. Modules: `app`, `core:*`, `feature:signin`, `feature:booking`, `feature:payment`, `feature:guest`, `feature:notification`, `feature:create-account`, `feature:host`, `feature:home`, `feature:search`, `feature:auth`, `feature:chat`, `feature:wishlist`, `feature:inbox`, `feature:webflow`
  - Zero `.tsx`, `.jsx`, `.ts`, `.js`, `.html`, `.vue`, `.svelte` files exist. No web framework is present.
  - `ApiEndPoints.kt` (227 lines) defines ~100 endpoints across auth, user, properties, bookings, chat, notifications, payments, reviews, host, verification, analytics, bidding, split-booking, destinations — **zero `/admin/*` endpoints**.
  - `grep -ri "isAdmin|AdminScreen|AdminPanel|admin_panel"` across all `.kt`, `.java`, `.gradle` files returns **zero matches**.
  - `grep -ri "admin"` across code files returns only: (a) code comments referencing a backend admin approval process, (b) JSON field names (`adminApproved`, `admin_status`) from API responses, (c) Google Maps `administrative_area_level_1`.
- **Root cause:** The admin panel was either never built, or it lives in a completely separate repository not included here.
- **Files involved:**
  - `settings.gradle.kts` — full module list, no admin module
  - `core/network/src/main/java/com/shourov/apps/pacedream/core/network/ApiEndPoints.kt` — full API surface, no admin endpoints
- **Fix:** Locate the admin panel repo (if it exists) and audit it separately, or build an admin panel from scratch.

---

### Finding 2: No Admin Authentication or Session Handling

- **Severity:** CRITICAL
- **Screen/Feature:** Admin login
- **Expected behavior:** Admin users authenticate via separate credentials or SSO with elevated privileges; sessions are scoped to admin role with timeout policies.
- **Actual behavior from code:** The auth system (`feature:auth`, `feature:signin`, `core:network/auth/AuthSession.kt`) handles consumer/host authentication only. Auth0 integration is consumer-facing. No admin credential flow, no admin token scope, no admin session management.
- **Files involved:**
  - `core/network/src/main/java/com/shourov/apps/pacedream/core/network/auth/AuthSession.kt`
  - `feature/signin/` — OTP and email login for consumers
  - `feature/auth/` — Auth0 callback for consumers
- **Fix:** N/A in this repo — requires admin panel implementation.

---

### Finding 3: No Role-Based Access Control

- **Severity:** CRITICAL
- **Screen/Feature:** Permission gating
- **Expected behavior:** Admin endpoints and UI gated by role checks (e.g., `ADMIN`, `MODERATOR`, `SUPPORT`). Route guards prevent unauthorized access.
- **Actual behavior from code:** No `isAdmin`, `role`, `permission`, `guard`, `protect` references exist in any routing or screen code. The only role distinction is guest vs. host, determined by UI navigation (bottom tabs), not by server-enforced roles.
- **Files involved:**
  - `app/src/main/kotlin/com/shourov/apps/pacedream/navigation/DashboardNavigation.kt` — destinations: HOME, SEARCH, FAVORITES, BOOKINGS, INBOX, PROFILE only
- **Fix:** N/A in this repo.

---

### Finding 4: No Admin Dashboard

- **Severity:** CRITICAL
- **Screen/Feature:** Operator dashboard
- **Expected behavior:** Dashboard showing: total users, active listings, pending moderation items, open reports, KYC queue depth, revenue metrics, platform health.
- **Actual behavior from code:** The only dashboards are:
  - `DashboardScreen.kt` / `HomeScreen.kt` — consumer home feed showing listings
  - `HostDashboardScreen.kt` — host's own listings, bookings, earnings
  - Neither provides platform-wide operator metrics.
- **Files involved:**
  - `feature/home/presentation/src/main/java/.../DashboardScreen.kt`
  - `app/src/main/kotlin/.../host/presentation/HostDashboardScreen.kt`
- **Fix:** N/A in this repo.

---

### Finding 5: No Notification Bell for Operators

- **Severity:** CRITICAL
- **Screen/Feature:** Admin alerts for new registrations, new listings, reports, KYC, payment issues
- **Expected behavior:** Real-time notification bell surfacing: new user signups, new listing submissions needing review, user reports filed, verification documents submitted, payout failures.
- **Actual behavior from code:** The notification system (`feature:notification`, `NotificationCenterScreen.kt`, `PaceDreamFirebaseMessagingService.kt`, `OneSignalService.kt`) delivers consumer/host notifications only — booking confirmations, messages, review prompts. No operator-targeted notification channel exists.
- **Files involved:**
  - `feature/notification/` — consumer notification module
  - `app/src/main/kotlin/.../notification/` — FCM/OneSignal for consumer push
- **Fix:** N/A in this repo.

---

### Finding 6: No Listing Moderation Tools

- **Severity:** CRITICAL
- **Screen/Feature:** Approve/reject listings
- **Expected behavior:** Moderation queue where operators review submitted listings, approve or reject with reason, and the listing status updates accordingly.
- **Actual behavior from code:** The host-side code *references* admin approval — comments in `HostRepository.kt` (lines 900-960) say "pending admin approval" and check `adminApproved`/`admin_status` fields from API responses. This proves the **backend has a moderation concept**, but there is **no UI in this repo for an operator to perform that moderation**. The Android app only reads the approval status to display it to the host.
- **Files involved:**
  - `app/src/main/kotlin/.../host/data/HostRepository.kt:900-960` — reads `adminApproved`, `admin_status` from API
  - `app/src/main/kotlin/.../host/presentation/HostListingsViewModel.kt` — displays status to host
- **Fix:** N/A in this repo. An admin panel must provide the moderation queue.

---

### Finding 7: No User Management Tools

- **Severity:** CRITICAL
- **Screen/Feature:** Ban, suspend, view user details
- **Expected behavior:** Operator can search users, view profiles, see activity history, ban or suspend accounts, add notes.
- **Actual behavior from code:** No user management screens exist. No ban/suspend API endpoints. The `REPORT_CONTENT = "users/report"` endpoint exists for *users to report other users*, but there is no corresponding admin endpoint to *act on* those reports.
- **Files involved:**
  - `core/network/src/main/java/.../ApiEndPoints.kt:220` — `REPORT_CONTENT` is user-facing only
- **Fix:** N/A in this repo.

---

### Finding 8: No Report Review Workflow

- **Severity:** CRITICAL
- **Screen/Feature:** Report triage
- **Expected behavior:** Operator queue showing user-submitted reports with context, ability to take action (warn, ban, dismiss).
- **Actual behavior from code:** `ReportBlockSheet.kt` lets users submit reports via `REPORT_CONTENT` endpoint. Reports go to the backend and are never surfaced to any operator UI in this repo.
- **Files involved:**
  - `app/src/main/kotlin/com/pacedream/app/feature/inbox/ReportBlockSheet.kt` — user-facing report submission
- **Fix:** N/A in this repo.

---

### Finding 9: No Verification/KYC Review Tools

- **Severity:** CRITICAL
- **Screen/Feature:** ID verification review
- **Expected behavior:** Operator reviews submitted ID documents, approves or rejects verification, communicates status to user.
- **Actual behavior from code:** The app has a full user-facing verification flow (`IDVerificationScreen.kt`, `IdentityVerificationScreen.kt`, `PhoneVerificationScreen.kt`) that submits documents via `VERIFICATION_SUBMIT` and checks status via `VERIFICATION_STATUS`. But there is **no admin-side UI to review these submissions**. The backend presumably has an internal review process, but this repo provides no operator tools for it.
- **Files involved:**
  - `app/src/main/kotlin/com/pacedream/app/feature/verification/` — user-facing submission
  - `core/network/src/main/java/.../services/VerificationApi.kt` — API calls
- **Fix:** N/A in this repo.

---

### Finding 10: No Audit Log

- **Severity:** CRITICAL
- **Screen/Feature:** Audit trail
- **Expected behavior:** Every operator action (approve listing, ban user, dismiss report) logged with timestamp, actor, action, target.
- **Actual behavior from code:** The only logging is `ProfileVerifierLogger.kt` (client-side debug logging for profile verification) and `FirebaseAnalyticsHelper.kt` (consumer analytics events). No audit log infrastructure exists.
- **Files involved:**
  - `app/src/main/kotlin/.../util/ProfileVerifierLogger.kt` — debug logger, not audit
  - `core/analytics/` — consumer analytics, not admin audit
- **Fix:** N/A in this repo.

---

## C. Classification

### Must Fix Before Launch

| # | Finding | Nature |
|---|---------|--------|
| 1 | No admin panel in repo | Entire admin system missing |
| 2 | No admin authentication | No operator login |
| 3 | No RBAC | No permission enforcement |
| 4 | No admin dashboard | No platform visibility |
| 5 | No notification bell for ops | Blind to platform events |
| 6 | No listing moderation UI | Cannot approve/reject listings |
| 7 | No user management | Cannot ban/suspend users |
| 8 | No report review | User reports go into a black hole |
| 9 | No KYC review tools | Cannot verify user identities |
| 10 | No audit log | No accountability |

### Fix Soon After Launch

N/A — there is nothing to defer because the entire admin surface is absent.

### Can Defer

N/A.

---

## D. Final Section

### Missing Operator Visibility

**Everything.** An operator running PaceDream with only this repo would have:
- No view of total users or growth
- No view of listing submissions or their moderation status
- No view of user reports
- No view of verification/KYC queue
- No view of payment/payout issues
- No view of platform revenue
- No view of booking disputes
- No real-time alerts for any platform event

The backend API shows evidence of admin-side concepts (listing approval status fields like `adminApproved`, `moderationStatus`, `admin_status`), which means the backend *expects* an admin operator to exist. But this repo provides no tooling for that operator.

### Security / Permission Risks

1. **No admin endpoint protection visible** — Since no admin endpoints exist in `ApiEndPoints.kt`, either (a) admin operations happen through a separate system not in this repo, or (b) admin operations don't exist yet. If the backend exposes admin endpoints without proper auth, that's a backend issue outside this repo's scope.

2. **Client-side moderation status interpretation** — `HostRepository.kt:940-960` performs client-side logic to override listing status based on `adminApproved`/`admin_status` fields. This means a modified client could display listings as approved even when they're not. This is cosmetic-only (the backend should enforce visibility), but it signals that the client is doing work the backend should handle.

3. **No content moderation warnings on Android** — Per `ANDROID_IOS_PARITY_AUDIT.md:283`, iOS has `ContentModerationService` that blocks/warns/allows message content, but Android doesn't show moderation warnings in chat (`ContentModerationCheck.kt` exists in the inbox feature but the audit doc flags it as a gap).

### Broken Moderation Workflows

There are **no moderation workflows** to break — they simply don't exist in this repo. The closest things are:

1. **Host listing status display** — The host can see their listing marked as "pending_review" when `adminApproved` is false. This is a *read-only status indicator*, not a moderation workflow.

2. **User report submission** — Users can report content via `ReportBlockSheet.kt` -> `REPORT_CONTENT` endpoint. The report is submitted to the backend but there is no admin-side tooling to review it.

3. **Content moderation check** — `ContentModerationCheck.kt` in the inbox feature performs client-side content checks on chat messages. This is a client-side filter, not an operator-driven moderation flow.

---

## Conclusion

**This repository is not the admin codebase.** It is the PaceDream Android consumer/host mobile application. It contains zero admin screens, zero admin API endpoints, zero admin authentication, zero moderation tools, and zero operator dashboards.

To audit the PaceDream admin system, you need to locate a separate repository — likely a web application (React/Next.js/Vue) — that provides the operator-facing admin panel. If no such repository exists, then PaceDream has no admin tooling and cannot be operated as a live marketplace.

### Evidence Summary

| What was searched | Result |
|---|---|
| `settings.gradle.kts` modules | 30+ modules, zero admin |
| `ApiEndPoints.kt` endpoints | ~100 endpoints, zero `/admin/*` |
| All `.kt`/`.java` files for `isAdmin`/`AdminScreen`/`AdminPanel` | Zero matches |
| All files for web frameworks (React/Next/Vue/Angular) | Zero web files |
| Navigation routes | HOME, SEARCH, FAVORITES, BOOKINGS, INBOX, PROFILE — no ADMIN |
| Git history for admin-related commits | None found |
| Markdown docs for admin panel references | Only `Firebase Admin` in runbook dependencies |
