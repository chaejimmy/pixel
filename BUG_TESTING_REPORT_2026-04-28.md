# PaceDream Android — Bug Testing Report

**Date:** 2026-04-28
**Branch:** `claude/bug-testing-report-FkqDh` (HEAD `f4d55e7`, 1 ahead of `main`)
**versionCode / versionName:** 14 / 0.1.4

---

## 1. Test execution status

The Gradle test suite could **not** be executed in this run because the
build environment cannot reach Google Maven:

```
$ curl -I https://dl.google.com/dl/android/maven2/.../com.android.application-9.1.0.pom
HTTP/2 403
x-deny-reason: host_not_allowed
```

`com.android.application` 9.1.0 is only published on Google Maven, so
plugin resolution fails immediately and no Gradle task — including the
pure-JVM `testDebugUnitTest` and `lint` — gets a chance to run:

```
* What went wrong:
Plugin [id: 'com.android.application', version: '9.1.0', apply: false]
was not found in any of the following sources:
  - Gradle Core Plugins
  - Included Builds
  - Plugin Repositories (could not resolve plugin artifact
    'com.android.application:com.android.application.gradle.plugin:9.1.0')
```

**Action required:** rerun the suite on a host with Google Maven
reachable (any developer machine or CI runner). The commands are:

```
./gradlew testDebugUnitTest --continue
./gradlew lintProdDebug
./gradlew :app:assembleProdDebug
```

The repository’s own `build_output.txt` is a stale artifact from a
different machine (`/Users/sean/pixel/...`) showing
`Unresolved reference 'activeListingsCount'` in `HostAnalyticsScreen.kt`,
`HostDashboardScreen.kt`, and `HostPostScreen.kt`. Those references no
longer exist on this branch — `activeListingsCount` only appears in
`HostProfileScreen.kt`/`HostProfileViewModel.kt`, where the field is
defined — so that compile error is **not present** on `f4d55e7` and the
file should be regenerated or removed on the next clean build.

## 2. Static review of code on `f4d55e7`

A static pass over `app/`, `feature/*/`, `core/*/` surfaced the items
below. None are confirmed crashes against live data — they are
flagged for verification once Gradle can run.

### 2.1 Duplicate imports (lint debt, not a compile error)

Kotlin compiles duplicate imports with a warning. Three files have one
each:

| File | Line | Duplicated symbol |
|---|---|---|
| `app/src/main/kotlin/com/pacedream/app/feature/home/HomeScreen.kt` | 17–18 | `androidx.compose.foundation.interaction.collectIsPressedAsState` |
| `app/src/main/kotlin/com/shourov/apps/pacedream/feature/propertydetail/PropertyDetailScreen.kt` | — | `androidx.compose.ui.platform.LocalContext` |
| `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/EditListingScreen.kt` | — | `androidx.compose.foundation.rememberScrollState` |

These will trip ktlint / Android Lint warnings but will not fail
`testDebugUnitTest`. Recommend deleting the second occurrence in each.

### 2.2 Fragile default argument in `ListingFieldSchema`

`app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/ListingFieldSchema.kt:101`

```kotlin
val defaultPricingUnit: PricingUnit = allowedPricingUnits.first(),
```

If a future caller constructs `ListingSubcategorySchema` with an empty
`allowedPricingUnits` list, the constructor throws
`NoSuchElementException` at instance creation time — the kind of crash
that won’t surface until a host opens that subcategory in the create-
listing wizard. All current 13 call sites pass non-empty lists, so this
is **not a live bug** today, but the default arg should fall back to
e.g. `allowedPricingUnits.firstOrNull() ?: PricingUnit.DAY` to harden
against regressions.

### 2.3 Pagination ranking ordering (subtle)

`app/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchViewModel.kt:285-295`

`rankByRelevance` is applied per-page. On a paginated `loadPage(reset =
false)` call, the new page is re-sorted internally and **appended** to
the existing list. A high-relevance match landing on page 2 therefore
appears below low-relevance items from page 1. If product expects
strictly relevance-ordered results, the ranking pass needs to apply
across the merged list, not per page. Behavior is acceptable if the
backend already returns globally-ranked pages; flag for product
confirmation rather than as a fix.

### 2.4 Swallowed exceptions

13 `catch (_: Exception) {}` / `catch (e: Exception) { }` sites across
production code. Most are deliberate (closing input streams, fire-and-
forget read receipts, opening external URIs from `AboutUsScreen`). Two
worth a second look once the suite can run:

* `app/src/main/kotlin/com/pacedream/app/feature/bookings/BookingsViewModel.kt:591`
* `app/src/main/kotlin/com/shourov/apps/pacedream/feature/booking/presentation/BookingTabViewModel.kt:301`

Both are in booking flows where a swallowed network/IO failure can
leave the UI silent. Reviewer should confirm the surrounding code
already surfaces failure through another path.

### 2.5 Test suite shape (no defects spotted by inspection)

* 501 `@Test` annotations across `app/src/test`,
  `feature/*/src/test`, `core/*/src/test`.
* 914 assertion calls (`assertEquals`, `assertTrue`, `assertFalse`,
  `assertNull`, `assertNotNull`).
* **0** `@Ignore` annotations, **0** `fail(...)` placeholders, **0**
  `TODO()` / `NotImplementedError` in test bodies.

Notable focused suites that should be exercised first when the
environment can run them: `ListingAddressRuleTest` (locks in PR #460’s
online-listing address rule), `RefreshWithFallbackTest` (token refresh
retry semantics), `ApiErrorTest` / `ApiResultOperationsTest`,
`BookingRepositoryParsingTest`, `HomeFeedRepositoryParsingTest`,
`SearchRepositoryParsingTest`, `WishlistRepositoryParsingTest`,
`InboxRepositoryParsingTest`.

## 3. Recommended next run

On a host with Google Maven access, run the commands from §1 plus the
P0 manual cases from `TEST_PLAN_ANDROID.md` (AUTH-01..09, HOME-01,
SEARCH-01/02, DETAIL-01/04/05, BOOK-01..08, PUSH-01..03/06/07,
LINK-01/03, STAB-01/03/05/06). Append actual results below this line
when available.
