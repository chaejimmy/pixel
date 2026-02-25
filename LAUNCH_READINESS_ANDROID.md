# PaceDream Android — Launch Readiness Report

**Date:** 2026-02-25
**Version:** 0.1.2 (versionCode 8)
**Package:** `com.shourov.apps.pacedream`

---

## Architecture Summary

| Aspect | Detail |
|--------|--------|
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM with StateFlow |
| DI | Hilt (SingletonComponent) |
| Networking | OkHttp 4.12.0 + custom ApiClient (iOS-parity) |
| Serialization | kotlinx.serialization (primary), Moshi (legacy models) |
| Auth | Auth0 SDK 2.10.2 + EncryptedSharedPreferences |
| Image Loading | Coil 2.6.0 |
| Database | Room 2.6.1 |
| Push | Firebase Cloud Messaging (BOM 32.4.0) |
| Payments | Stripe Android 22.5.0 |
| Maps | Google Maps Compose 4.4.1 |
| Min SDK | 24 / Target SDK 34 |
| Build Variants | debug (.debug suffix) / release (R8 enabled) |
| Flavors | prod (single) |

---

## P0 Issues (MUST fix before release)

### P0-1 — FCM token never registered with backend ✅ FIXED
- **File:** `app/.../notification/PaceDreamFirebaseMessagingService.kt`
- **Problem:** `sendTokenToServer()` was a TODO stub. After an FCM token refresh, the server never received the new token, so push notifications would silently stop reaching devices.
- **Fix:** Implemented actual POST to `/v1/notifications/register-device` with the token and platform payload. Only sends when user is authenticated.

### P0-2 — Missing POST_NOTIFICATIONS runtime permission guard ✅ FIXED
- **File:** `app/.../notification/PaceDreamNotificationService.kt`
- **Problem:** On Android 13+ (API 33), `NotificationManagerCompat.notify()` throws `SecurityException` if `POST_NOTIFICATIONS` permission has not been granted. The service was calling `notify()` without checking.
- **Fix:** Added `hasNotificationPermission()` check before every `notify()` call. Logs a warning and returns gracefully when permission is absent.

### P0-3 — ProGuard rules missing for kotlinx.serialization ✅ FIXED
- **File:** `app/proguard-rules.pro`
- **Problem:** R8 in full mode strips `@Serializable` metadata, causing `SerializationException` crashes on all JSON parsing in release builds. Also missing rules for Auth0 and Stripe SDKs.
- **Fix:** Added comprehensive keep rules for kotlinx.serialization companions and serializers, Moshi JsonAdapter, Auth0 SDK, and Stripe SDK.

### P0-4 — Hardcoded Auth0 client ID placeholder ✅ FIXED
- **Files:** `app/build.gradle.kts`, `app/.../config/AppConfig.kt`
- **Problem:** `DEFAULT_AUTH0_CLIENT_ID = "YOUR_AUTH0_CLIENT_ID"` would cause Auth0 login to fail silently in production.
- **Fix:** Added `AUTH0_CLIENT_ID` BuildConfig field sourced from `local.properties` / CI. AppConfig now reads BuildConfig first. Logs an explicit error if the value is blank.

### P0-5 — Dead TokenExpiredInterceptor (no-op on 401) ✅ FIXED
- **File:** `core/network/.../interceptor/TokenExpiredInterceptor.kt`
- **Problem:** All 401/403 handling logic was commented out with empty synchronized block, making Retrofit-based code paths silently ignore expired tokens.
- **Fix:** Replaced with Timber logging so 401 events are visible in Crashlytics/logs. The primary ApiClient already handles 401→refresh→retry properly.

---

## P1 Issues (Should fix before public launch)

### P1-1 — Release signing uses debug keystore
- **File:** `app/build.gradle.kts:48`
- **Impact:** Play Store uploads will fail with debug keystore. Must configure a dedicated release keystore.
- **Recommendation:** Create `keystore.properties` and load signing config from it.

### P1-2 — No certificate pinning
- **File:** `app/src/main/res/xml/network_security_config.xml`
- **Impact:** MITM attacks possible on rooted/compromised devices.
- **Recommendation:** Add SHA-256 pin-set for `pacedream-backend.onrender.com` and `pacedream.com`.

### P1-3 — Single prod flavor only (no dev/staging)
- **File:** `build-logic/.../PaceDreamFlavor.kt`
- **Impact:** No way to test against staging backend without code changes.
- **Recommendation:** Re-enable `dev` flavor with staging API URL.

### P1-4 — Crashlytics mapping upload disabled
- **File:** `build-logic/.../AndroidApplicationFirebaseConventionPlugin.kt`
- **Impact:** Stack traces from production crashes will be obfuscated.
- **Recommendation:** Enable `mappingFileUploadEnabled = true` and configure Firebase project.

### P1-5 — No Coil disk cache configuration
- **Impact:** All images re-downloaded on cold start; increased data usage and slower loads.
- **Recommendation:** Configure `ImageLoader` with `diskCache` in Application class.

### P1-6 — Database version 1 with destructive migration
- **File:** `core/database/.../PaceDreamDatabase.kt`
- **Impact:** Any schema change will destroy all local data.
- **Recommendation:** Add proper Room migration strategy before adding any new entities.

### P1-7 — No offline/connectivity handling in UI
- **Impact:** Network errors show cryptic messages; no offline banners.
- **Recommendation:** Add ConnectivityManager observer and show offline banner in Scaffold.

### P1-8 — Deep link autoVerify requires /.well-known/assetlinks.json
- **File:** `AndroidManifest.xml` (intent-filters with `autoVerify="true"`)
- **Impact:** App Links won't work unless `pacedream.com/.well-known/assetlinks.json` is published.
- **Recommendation:** Deploy Digital Asset Links file to production domain.

---

## Launch Gates

| Gate | Status | Blocker? |
|------|--------|----------|
| All P0 fixes verified in release build | ✅ Code complete | Yes |
| Release signing configured | ❌ Not done | Yes |
| Auth0 client ID set for prod | ❌ Needs CI config | Yes |
| Stripe publishable key set for prod | ❌ Needs CI config | Yes |
| Play Store listing drafted | ❓ Unknown | Yes |
| Data Safety form completed | ❌ Not done | Yes |
| Crashlytics mapping upload | ❌ Disabled | No (P1) |
| Digital Asset Links deployed | ❓ Unknown | No (P1) |
| Certificate pinning | ❌ Not done | No (P1) |
| Internal test track QA pass | ❌ Not started | Yes |

---

## Permissions Justification (Play Store)

| Permission | Justification |
|------------|---------------|
| `INTERNET` | Core app functionality – all content is fetched from backend API. |
| `POST_NOTIFICATIONS` | Push notifications for messages, booking updates, and promotions. Runtime permission requested on Android 13+. |
| `VIBRATE` | Haptic feedback for message and booking notifications. |
| `WAKE_LOCK` | Required by Firebase Messaging to process push notifications when screen is off. |
| `ACCESS_FINE_LOCATION` | Show nearby properties and pre-fill search location. User can deny; app falls back to manual entry. |
| `ACCESS_COARSE_LOCATION` | Fallback for location-based search when fine location is denied. |
| `AD_ID` | **Removed** via `tools:node="remove"`. Not used. |

---

## Data Safety Section Checklist

| Category | Collected? | Shared? | Notes |
|----------|------------|---------|-------|
| Name | Yes | No | First/last name for profile |
| Email | Yes | No | Auth and profile |
| Phone | Optional | No | Verification only |
| Location (precise) | Optional | No | Search nearby; not stored server-side |
| Payment info | Yes | Yes (Stripe) | Processed via Stripe; PCI compliant |
| Device identifiers | Yes | No | FCM token for push notifications |
| Crash logs | Yes | No | Firebase Crashlytics (when enabled) |
| App interactions | Optional | No | Firebase Analytics (disabled by default) |
| Photos | Optional | No | Profile avatar upload |

---

## Build Verification

```
Build variants:     prodDebug, prodRelease
Minification:       R8 (release only)
ProGuard:           proguard-android-optimize.txt + app/proguard-rules.pro
Baseline Profile:   Auto-generated for release builds
Dependency Guard:   prodReleaseRuntimeClasspath
```
