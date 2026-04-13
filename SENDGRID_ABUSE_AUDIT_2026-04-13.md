# PaceDream -- SendGrid / Email Abuse Risk Audit

**Date:** 2026-04-13
**Scope:** `iOS26` (iOS client) + `pixel` (Android client) -- all flows that trigger server-side email or SMS sends
**Backend reference:** `pacedream-platform` (Express.js + SendGrid) -- findings inferred from client code + E2E Business Flow Review (2026-04-12)
**Auditor:** Automated security audit

---

## Executive Summary

**Final Verdict: YES -- these repos can be abused as an email-bomb vector if the backend lacks per-email/per-IP rate limiting.**

Both the iOS and Android clients expose multiple unauthenticated endpoints that trigger server-side SendGrid emails (password reset, signup verification) with **no client-side cooldowns, no CAPTCHA, and no bot protection**. The backend rate limiter currently uses an **in-memory store** (resets on server restart, not shared across instances). An attacker bypassing the mobile clients and hitting the API directly faces only this ephemeral rate limiting.

The OTP phone login flow is the **one exception** -- it has a 60-second cooldown on iOS (properly enforced) and a partial UI-only cooldown on Android. All other email/SMS-triggering paths are unprotected.

---

## Architecture Overview

```
Mobile Clients (iOS/Android)          Backend (Express.js)           Email Provider
  |                                      |                              |
  |-- POST /auth/forgot-password ------->|-- SendGrid API ------------->| Password reset email
  |-- POST /auth/signup/initiate ------->|-- SendGrid API ------------->| Verification code email
  |-- POST /auth/send-email-code ------->|-- SendGrid API ------------->| Email verification code
  |-- POST /auth/otp/send ------------->|-- Twilio Verify SMS --------->| OTP SMS
  |-- POST /auth/signup/send-sms-otp -->|-- Twilio Verify SMS --------->| Signup OTP SMS
  |                                      |                              |
  | SendGrid is ONLY called server-side  |  Rate limiter: in-memory     |
  | No API keys in client code           |  (resets on restart)          |
```

**Positive:** SendGrid is only called server-side. No SendGrid API keys exist in either client repo. All email sending goes through the backend API.

---

## P0 -- Critical Findings (Must Fix Before Production)

### P0-1: Forgot Password -- No Cooldown, No CAPTCHA (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **Severity** | P0 -- can be weaponized as email bomb |
| **Endpoint** | `POST /v1/auth/forgot-password` (unauthenticated) |
| **iOS file** | `PaceDream/Services/AuthService.swift` -- `forgotPassword(email:)` |
| **iOS view** | `PaceDream/Views/Authentication/AuthFlowSheet.swift` -- `handleForgotPassword()` |
| **Android file** | `feature/signin/.../forgotPassword/ForgotPasswordViewModel.kt` -- `sendResetLink()` |
| **Android view** | `feature/signin/.../forgotPassword/ForgotPasswordScreen.kt` |
| **Problem** | Both platforms: button disabled only during in-flight request (`isLoading`). Immediately re-enabled after response. No cooldown timer, no attempt counter, no CAPTCHA. |
| **Attack** | Script sends `POST /auth/forgot-password {"email":"victim@example.com"}` in a loop. Each call triggers a SendGrid email. Attacker can target any email address without authentication. |

**Fix:**
- **Backend (critical):** Add per-email rate limit: max 3 requests per email per 15 minutes. Return `429` with `Retry-After` header. Always return `200 OK` regardless of whether email exists (prevents enumeration).
- **iOS:** Add 60-second cooldown timer matching the OTP pattern in `OTPPhoneLoginService`. Disable "Send Reset Link" button during cooldown. Show "Resend in Xs" text.
- **Android:** Add ViewModel-persisted cooldown (not just Composable state). Show countdown timer on button.

### P0-2: Email Signup Initiation -- No Cooldown, No CAPTCHA (Android)

| Attribute | Detail |
|-----------|--------|
| **Severity** | P0 -- can flood arbitrary email addresses with verification codes |
| **Endpoint** | `POST /v1/auth/signup/initiate` (unauthenticated) |
| **Android file** | `core/network/.../auth/AuthSession.kt` -- `initiateEmailSignup(email)` |
| **Problem** | Simple fire-and-forget POST with no throttle. The create-account wizard's OTP verification step (`OTPVerification.kt`) has `// todo` stubs for resend callbacks. |
| **Attack** | Loop `POST /auth/signup/initiate {"email":"victim@example.com"}` -- each call sends a verification email via SendGrid. |

**Fix:**
- **Backend:** Per-email rate limit on `/auth/signup/initiate`: max 3 per email per 15 minutes.
- **Android:** Wire up the OTP resend callback in `OTPVerification.kt` with a 60-second cooldown. Add cooldown to `initiateEmailSignup()` in ViewModel layer.

### P0-3: Backend Rate Limiter Uses In-Memory Store

| Attribute | Detail |
|-----------|--------|
| **Severity** | P0 -- all server-side rate limits are ephemeral |
| **Evidence** | `LAUNCH_READINESS_CHECK.md` P0 #9: "Rate limits use in-memory store -- reset on restart, not shared across instances. Redis is already connected" |
| **Problem** | The backend's rate limiting middleware (Express `express-rate-limit`) uses the default `MemoryStore`. On server restart (Render deployments, crashes), all rate limit counters reset to zero. Multi-instance deployments don't share state. |
| **Impact** | Even if the backend has rate limits configured for email-triggering endpoints, they can be circumvented by simply waiting for a deploy or targeting different instances. |

**Fix:**
- Switch `express-rate-limit` to use `rate-limit-redis` with the existing Redis connection. This was already identified in the March 2026 launch readiness check but remains unfixed as of the April 2026 E2E review.

---

## P1 -- High Findings (Fix Before Public Launch)

### P1-1: Legacy Auth Views With Weaker Protections (iOS)

| Attribute | Detail |
|-----------|--------|
| **iOS files** | `PaceDream/PaceDream/Views/Authentication/AuthenticationView.swift` (legacy) |
| **Contains** | `ForgotPasswordView` -- no cooldown on "Send Reset Email" button |
| **Contains** | `PhoneVerificationView` -- "Resend Code" button with NO cooldown at all |
| **Problem** | This legacy view exists alongside the modern `AuthFlowSheet.swift` and `OTPVerificationView.swift`. If reachable via any navigation path, it provides a weaker-protected route to the same endpoints. |

**Fix:** Remove or gate the legacy `AuthenticationView.swift` behind `#if DEBUG`. Audit navigation to ensure only the modern auth flow is reachable in production.

### P1-2: Android OTP Resend Cooldown Is UI-Only (Bypassable)

| Attribute | Detail |
|-----------|--------|
| **Android file** | `feature/signin/.../otp/OtpVerificationScreen.kt` -- `resendCountdown` |
| **Android file** | `feature/signin/.../otp/OtpVerificationViewModel.kt` -- `isResendCooldown` never set |
| **Problem** | The 60-second countdown lives in `remember { mutableStateOf(60) }` in the Composable. It resets on recomposition (screen rotation, navigation). The ViewModel's `isResendCooldown` flag is declared but never programmatically toggled. |
| **Impact** | Cooldown can be bypassed by rotating the device or navigating away and back. A modified client can call the OTP endpoint directly. |

**Fix:** Move cooldown state to the ViewModel with `System.currentTimeMillis()` persistence. Set `isResendCooldown = true` when send succeeds and start a coroutine-based countdown.

### P1-3: No CAPTCHA on Any Public Form (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **Affected endpoints** | `/auth/forgot-password`, `/auth/signup/initiate`, `/auth/signup/email`, `/auth/otp/send`, `/auth/send-email-code` |
| **Problem** | Zero bot protection across all unauthenticated flows. No reCAPTCHA, hCaptcha, or Cloudflare Turnstile. |
| **Impact** | Automated scripts can hit email/SMS-triggering endpoints without any human verification challenge. |

**Fix:**
- Add invisible reCAPTCHA v3 or Cloudflare Turnstile to the backend. Mobile clients pass a challenge token with each auth request. Backend validates token before processing.
- Minimum viable: Add reCAPTCHA to forgot-password and signup flows first.

### P1-4: Account Enumeration on Forgot Password (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **iOS file** | `PaceDream/Views/Authentication/AuthFlowSheet.swift` -- `handleForgotPassword()` |
| **Android file** | `feature/signin/.../forgotPassword/ForgotPasswordViewModel.kt` |
| **Problem** | The forgot-password flow returns different responses for existing vs. non-existing emails. iOS shows a green success banner on success, error on failure. Android sets `successMessage` vs `errorMessage`. |
| **Impact** | Attacker can enumerate valid email addresses by observing response differences. |

**Fix:** Backend should always return HTTP 200 with the same message regardless of whether the email exists: "If an account with this email exists, you'll receive a reset link." Both clients should display this uniform message.

### P1-5: No Abuse Logging or Anomaly Detection (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **Problem** | Neither client logs failed auth attempts, rapid resend patterns, or email-triggering frequency. Backend audit logging exists for admin actions but no specific email-abuse anomaly detection was documented. |
| **Impact** | Abuse patterns (e.g., 100 forgot-password requests for different emails from the same IP in 5 minutes) go undetected until SendGrid quota is exhausted or reputation is damaged. |

**Fix:**
- **Backend:** Log all email-sending events with IP, user-agent, email target, and timestamp. Set up alerts for: >10 forgot-password requests from one IP/hour, >5 requests for the same email/hour.
- **SendGrid:** Configure event webhooks for bounces, spam reports, and blocks. Feed into monitoring.

---

## P2 -- Medium Findings (Fix Post-Launch)

### P2-1: Legacy OTP Endpoints Still Defined (iOS)

| Attribute | Detail |
|-----------|--------|
| **iOS file** | `PaceDream/Models/Networking/Endpoints/AuthEndpoint.swift` |
| **Evidence** | `case sendOtp = "auth/send-otp"` and `case verifyOtp = "auth/verify-otp"` -- marked "Legacy OTP endpoints (kept for backward compatibility)" |
| **Problem** | If the backend still accepts these legacy endpoints, they provide a parallel path to trigger SMS sends without the cooldown protections of the new `auth/otp/send` flow. |

**Fix:** Deprecate and remove the legacy `auth/send-otp` and `auth/verify-otp` endpoints from the backend. Remove the legacy endpoint definitions from the iOS client.

### P2-2: Phone Verification Code Send -- No Cooldown (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **iOS file** | `PaceDream/Services/VerificationService.swift` -- `sendPhoneVerificationCode()` |
| **Android file** | `core/network/.../repository/VerificationRepository.kt` -- `sendPhoneVerificationCode()` |
| **Endpoint** | `POST /v1/users/verification/phone/send-code` (authenticated) |
| **Problem** | No cooldown or throttle. Can repeatedly trigger SMS sends. Requires authentication so lower risk than unauthenticated endpoints. |

**Fix:** Add 60-second cooldown matching the OTP pattern. Backend: per-user rate limit of 3 per hour.

### P2-3: Bounce / Suppression / Unsubscribe Handling Not Visible

| Attribute | Detail |
|-----------|--------|
| **Problem** | No evidence in either client repo of bounce handling, suppression list checking, or unsubscribe management. This is a backend/SendGrid concern but affects abuse posture. |
| **Impact** | Sending to bounced addresses wastes SendGrid quota and damages sender reputation. No suppression list means blocked addresses can still be targeted for email bombs. |

**Fix:**
- Enable SendGrid's Event Webhook. Process `bounce`, `dropped`, `spam_report` events. Maintain a suppression list in MongoDB. Check before sending.
- Enable SendGrid's Suppression Groups and add an unsubscribe header to transactional emails.

### P2-4: Email Login Brute Force (Both Platforms)

| Attribute | Detail |
|-----------|--------|
| **iOS file** | `PaceDream/Services/AuthService.swift` -- `login(email:password:)` |
| **Android file** | `feature/signin/.../EmailSignInViewModel.kt` -- `loginWithEmail()` |
| **Problem** | No client-side lockout, no exponential backoff, no CAPTCHA after failed attempts. While this doesn't directly trigger emails, it enables credential stuffing. |

**Fix:** Backend: Implement progressive lockout (lock account after 5 failures for 30 minutes). Return 429 after threshold. Consider CAPTCHA after 3 failed attempts.

---

## Comprehensive Endpoint Inventory

### All Routes That Trigger Email/SMS Sends

| # | Endpoint | Method | Auth Required | Triggers | Client Cooldown (iOS) | Client Cooldown (Android) | Risk |
|---|----------|--------|---------------|----------|-----------------------|---------------------------|------|
| 1 | `/auth/forgot-password` | POST | No | SendGrid email | NONE | NONE | **P0** |
| 2 | `/auth/signup/initiate` | POST | No | SendGrid email | N/A (not in iOS) | NONE | **P0** |
| 3 | `/auth/send-email-code` | POST | No | SendGrid email | NONE | NONE | **P1** |
| 4 | `/auth/otp/send` | POST | No | Twilio SMS | 60s (proper) | 60s (UI-only) | Low/P1 |
| 5 | `/auth/send-otp` | POST | No | Twilio SMS (legacy) | 60s (via VM timer) | N/A | **P2** |
| 6 | `/auth/signup/email` | POST | No | Welcome email | `isLoading` only | `isLoading` only | P1 |
| 7 | `/auth/signup/send-sms-otp` | POST | No | Twilio SMS | N/A | NONE | P1 |
| 8 | `/users/verification/phone/send-code` | POST | Yes | Twilio SMS | NONE | NONE | P2 |

### Routes That Do NOT Trigger Emails (Confirmed Safe)

- `/auth/login/email` -- authentication only, no email sent
- `/auth/verify-email-code` -- verification only, no new email sent
- `/auth/otp/check` -- verification only, no new SMS sent
- `/auth/otp/login` -- login after OTP, no new SMS sent
- `/auth/auth0/callback` -- OAuth exchange, no email sent
- `/auth/apple` -- Apple Sign-In, no email sent
- All search, booking, payment, messaging, review, and listing endpoints

### Flows NOT Found (Confirmed Absent)

- No contact/support forms that trigger emails
- No invite/referral/share flows that trigger emails
- No in-app email composer or mailto: links that could be abused
- No booking confirmation emails triggered directly by client code
- No newsletter or marketing email subscription flows

---

## Credential Leakage Assessment

| Check | Result |
|-------|--------|
| SendGrid API key in iOS source | **CLEAN** -- not found |
| SendGrid API key in Android source | **CLEAN** -- not found |
| Any API key in iOS committed source | **CLEAN** -- keys loaded from `Secrets.xcconfig` (gitignored) |
| Any API key in Android committed source | **CLEAN** -- keys loaded from `secrets.properties` (gitignored) |
| `secrets.defaults.properties` (Android) | Contains placeholder/dev values only (Auth0 dev domain, empty Stripe key) |
| Git history contamination | **PREVIOUSLY FOUND** -- `cookies.txt` and `cookies_signup.txt` with JWT tokens were in `pacedream-platform` git history (noted in March 2026 audit as needing BFG purge) |
| Auth0 dev tenant in production build | **STILL OPEN** -- iOS `Release.xcconfig:27` hardcodes dev Auth0 domain |

---

## What the Clients Do Right

1. **SendGrid is server-side only** -- no direct email sending from either client
2. **No API keys committed** -- both use gitignored secret config files
3. **HTTP 429 handling** -- both clients parse and display rate-limit errors from the backend
4. **OTP phone login (iOS)** -- proper 60-second cooldown with in-flight guard and backend rate-limit passthrough
5. **Security error handling** -- both clients handle `ACCOUNT_BLOCKED`, `SPAM_DETECTED`, `FRAUD_DETECTED` error codes from the backend
6. **Account restriction flow** -- iOS shows `AccountRestrictionView` when backend signals abuse

---

## Recommended Fix Priority and Effort

| Priority | Fix | Effort | Owner |
|----------|-----|--------|-------|
| **P0** | Switch backend rate limiter to Redis store | 30 min | Backend |
| **P0** | Add per-email rate limit on `/auth/forgot-password` (3/15min) | 1 hour | Backend |
| **P0** | Add per-email rate limit on `/auth/signup/initiate` (3/15min) | 1 hour | Backend |
| **P0** | Add 60s cooldown to forgot-password on iOS | 30 min | iOS |
| **P0** | Add 60s cooldown to forgot-password on Android | 30 min | Android |
| **P1** | Uniform forgot-password response (prevent enumeration) | 30 min | Backend |
| **P1** | Add CAPTCHA (Turnstile/reCAPTCHA) to public auth endpoints | 4-6 hours | Backend + clients |
| **P1** | Fix Android OTP cooldown to persist in ViewModel | 1 hour | Android |
| **P1** | Remove/gate legacy iOS `AuthenticationView.swift` | 30 min | iOS |
| **P1** | Add email abuse monitoring + alerting | 2-4 hours | Backend/Ops |
| **P2** | Deprecate legacy `/auth/send-otp` and `/auth/verify-otp` | 1 hour | Backend + iOS |
| **P2** | Enable SendGrid event webhooks + suppression handling | 2-3 hours | Backend |
| **P2** | Add cooldown to phone verification code send | 30 min | Both |
| **P2** | Add login brute-force lockout | 2 hours | Backend |

**Total estimated effort: ~2-3 days of focused work.**

---

## Final Verdict

> **Can this system be abused as an email relay or email bomb vector?**
>
> **YES -- with current protections, moderate effort required.**
>
> An attacker who bypasses the mobile UI (trivial with cURL or a proxy tool) can hit the unauthenticated `/auth/forgot-password` and `/auth/signup/initiate` endpoints in a tight loop, causing unlimited SendGrid emails to be sent to any arbitrary email address. The backend's in-memory rate limiter is the only protection, and it resets on every deploy.
>
> **Risk magnitude:** An attacker could send thousands of emails per hour until SendGrid's own account-level limits kick in (default: 100 emails/day on free tier, higher on paid plans). This would exhaust SendGrid quota, damage sender reputation, and could get the SendGrid account suspended.
>
> **Mitigating factors:** SendGrid's own account-level rate limits provide a ceiling. The backend does have rate-limiting middleware (just uses wrong storage). Both clients properly handle 429 responses, so once backend limits are fixed to use Redis, the system will be materially safer.
>
> **Bottom line:** Fix the three P0s (Redis rate limiter + per-email limits on forgot-password and signup/initiate) and this risk drops from "exploitable" to "adequately protected." Add CAPTCHA for defense-in-depth.

---

*Report compiled from live code inspection of `chaejimmy/iOS26` and `chaejimmy/pixel` repositories on 2026-04-13. File references are authoritative. Backend findings are inferred from client code, API endpoint definitions, and prior audit documents (`LAUNCH_READINESS_CHECK.md`, `E2E_BUSINESS_FLOW_REVIEW_2026-04-12.md`).*
