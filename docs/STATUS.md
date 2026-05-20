# PaceDream Android — Live Status

**Last reverified:** 2026-05-20 (branch `claude/audit-repo-analysis-8fDXG`)

This document supersedes the ~30 audit / report markdowns at the repo root for
day-to-day triage. Those originals are still valid as historical context but
many of their P0/P1 items have been quietly closed in the months since they
were written. Each row below was reverified against the code at HEAD on
2026-05-20.

When you add a new finding, add a row here first and link out to a dated
audit under `docs/audits/<YYYY-MM-DD>-<topic>.md` for the long-form analysis.

---

## Open

| ID  | Item                                                                 | Severity | Source                                  | Notes                                                                                                                                                          |
|-----|----------------------------------------------------------------------|----------|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| O-1 | No OkHttp certificate pinning for `api.pacedream.com`                | P1       | LAUNCH_READINESS_ANDROID.md P1-2        | `core/network` has no `CertificatePinner`. Needs SPKI hashes from the prod cert + a backup pin; load from `secrets.properties`, fail closed only in `prod`.    |
| O-2 | Backend rate limiter is in-memory (not Redis-backed)                 | P1       | SENDGRID_ABUSE_AUDIT_2026-04-13.md P0-3 | Out of this repo's scope; track in the backend repo. Android client cooldowns are already in place (EmailSignInViewModel:153, PhoneEntryViewModel:86).         |
| O-3 | Orphan redesign code paths still in tree                             | P2       | (new, this audit)                       | `com.pacedream.app.feature.home.HomeScreen`, `DealsCard.kt`, `EnhancedDashboardContent.kt` (with 20 hardcoded Unsplash URLs) are never called. Delete.         |
| O-4 | Nine empty / stub Gradle modules                                     | P2       | (this audit)                            | `feature/{auth,guest,host,payment}`, `core/{data-test,datastore,datastore-proto,datastore-test,domain}` have build files but no code. Delete or finish.        |
| O-5 | Test coverage is sparse                                              | P2       | (this audit)                            | Only 5/17 feature modules have any tests; ~25 test files total. Critical paths (booking, webflow, signin) need at least one ViewModel test each.               |
| O-6 | AGP 9.x deprecation warnings + Baseline Profile version mismatch     | P3       | (this audit)                            | `android.builtInKotlin`, `android.newDsl`, `excludeLibraryComponentsFromConstraints` flags are deprecated. Baseline Profile tested on AGP 8.3 vs running 9.1.  |
| O-7 | iOS parity is documented in five overlapping files                   | P3       | (this audit)                            | Pick one canonical `docs/PARITY.md` and archive the rest under `docs/audits/`.                                                                                 |

## Closed since the audits were written (reverified 2026-05-20)

| ID  | Item                                                                                                  | Original source                                | Where it was actually fixed                                                                                                  |
|-----|-------------------------------------------------------------------------------------------------------|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| C-1 | `compileProdReleaseKotlin` fails on unresolved `activeListingsCount` (host screens)                   | `build_output.txt` (now removed)               | `HostDashboardData` field renamed to `activeListings`; screens read it. Stale `build_output.txt` was from another machine.   |
| C-2 | Login success routes to `CREATE_ACCOUNT` instead of dashboard                                         | INTERACTION_AUDIT_REPORT.md C-01               | `SignInScreen.kt:162` calls `navController.navigate(route = DASHBOARD_ROUTE) { popUpTo(0) { inclusive = true } }`.            |
| C-3 | Phone OTP validation bypassed                                                                         | INTERACTION_AUDIT_REPORT.md C-03               | `PhoneEntryScreen.kt:115` calls `viewModel.sendOTP(...)`, button is gated on `uiState.isValidPhone && !isLoading && !isBlocked`. |
| C-4 | Back button is a hardcoded TODO in `BaseeScreen`                                                      | INTERACTION_AUDIT_REPORT.md C-04               | `BaseeScreen.kt:53-57` wires `onclick = onBackClick` on the navigation icon.                                                 |
| C-5 | `booking_details/{bookingId}` missing from `HostNavigationGraph`                                      | PRODUCTION_AUDIT.md P0-1                       | `HostNavigationGraph.kt:99` declares the route with `bookingId` nav arg and renders `HostBookingDetailScreen`.               |
| C-6 | `withdraw_earnings` route missing                                                                     | PRODUCTION_AUDIT.md P0-2                       | Consolidated into `HostScreen.Earnings` (`HostNavigationGraph.kt:124`). Host dashboard calls it via `onEarningsClick`.       |
| C-7 | "Rent Now" CTAs hardcoded to `onClick = {}` on DealsCard / LastMinuteDealCard / RentedGearDealsCard   | INTERACTION_AUDIT_REPORT.md C-05–C-07          | Those composables exist but are no longer called. The live home (`HomeFeedScreen`) wires its own CTAs in `DashboardNavigation.kt:300`. |
| C-8 | Notification bell on Home is an empty lambda                                                          | INTERACTION_AUDIT_REPORT.md C-08               | `DashboardNavigation.kt:324` passes `onNotificationClick = { navController.navigate("notifications") }`.                     |
| C-9 | Forgot-password endpoint has no client-side cooldown                                                  | SENDGRID_ABUSE_AUDIT_2026-04-13.md P0-1        | `EmailSignInViewModel.kt:153` enforces 60s post-call and 120s post-rate-limit cooldown.                                      |
| C-10| Coil missing disk cache                                                                               | LAUNCH_READINESS_ANDROID.md P1-5               | `di/ImageLoaderModule.kt` configures a 250 MB disk cache + 25 % memory cache + `respectCacheHeaders(true)`.                  |
| C-11| Room destructive migration                                                                            | LAUNCH_READINESS_ANDROID.md P1-6               | `core/database/migration/Migrations.kt` defines `MIGRATION_1_2` preserving cached booking rows.                              |
| C-12| Release signing falls back to debug keystore unconditionally                                          | LAUNCH_READINESS_ANDROID.md P1-1               | `app/build.gradle.kts:141-156` creates a real `release` signing config from `keystore.properties` / `secrets.properties` / `-P` Gradle properties, with the debug fallback only when no keystore is configured (so OSS builds still work). |
| C-13| Crashlytics mapping upload disabled                                                                   | LAUNCH_READINESS_ANDROID.md P1-4               | `AndroidApplicationFirebaseConventionPlugin.kt:49-57` gates upload behind `-PenableCrashlyticsMappingUpload=true` so CI opts in. |

## Notes on the audit docs

The root-level audit MDs were written at different points across Feb-May 2026
and contradict each other in places (e.g. Auth0 hardcoding "fixed" in
LAUNCH_READINESS but later "rotated" in SECURITY_REMEDIATION_REPORT). For new
work, write findings into this file and date-stamp the underlying analysis
under `docs/audits/`. Treat the original root MDs as archive — they are useful
history, not a to-do list.
