# Performance fixes — implementation notes

Companion document to `PERFORMANCE_AUDIT_2026_05_18.md`. Records the
subset of audit findings that landed in this branch, what was deliberately
deferred, and how to validate each change.

## Files changed

| File | Audit finding(s) | Change |
|---|---|---|
| `compose_compiler_config.conf` | F-01, F-47 | Replace `com.google.samples.apps.nowinandroid.core.model.data.*` with the actual PaceDream model + presentation packages so the Compose compiler can mark them stable. |
| `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt` | F-02 | Wrap `authSession.initialize()` + `sessionManager.initialize()` in `withTimeoutOrNull(8 s)` so a stalled `/account/me` can't pin `AuthState.Unknown` forever. |
| `feature/booking/.../BookingViewModel.kt` | F-03 | Add `try/finally` around `loadBookings()` and replace the cache-fallback `.collect { }` with `.firstOrNull { it !is Result.Loading }` so the spinner always clears. |
| `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt` | F-04 | Bound each favorites endpoint with `withTimeoutOrNull(5 s)`; previously the primary→fallback chain could block for ≈60 s. |
| `app/src/main/kotlin/com/shourov/apps/pacedream/di/ImageLoaderModule.kt` | F-06 | Add `.respectCacheHeaders(true)` so Coil honours the CDN's `Cache-Control`. |
| `feature/home/.../components/TrendingDestinationsSection.kt` | F-19 | Add `.background(PaceDreamGray100)` to the image Box so cards never flash transparent before the photo decodes. |
| `feature/home/.../components/DealsCard.kt` (2 sites) | F-19 | Wrap each `AsyncImage` with a sized `Box.background(Gray100)` placeholder. |
| `feature/home/.../components/EnhancedPropertyCards.kt` (3 sites) | F-19 | Same placeholder-background pattern for the main card, compact card, and image carousel. |
| `feature/home/.../components/EnhancedDashboardContent.kt` | F-08 | `remember`-cache `roomsState.rooms.take(10)` / `gearsState.rentedGears.take(10)` / `splitStaysState.splitStays.take(10)`; switch services to `itemsIndexed(..., key = { idx, stay -> stay._id ?: "split-idx-$idx" })`. |
| `feature/home/.../components/BrowseByTypeSection.kt` | F-08 | Same `remember`-caching + stable `itemsIndexed` key for the browse-by-type rail. |
| `feature/home/.../redesign/HomeRedesignRoute.kt` | F-07, F-09 | Gate `LaunchedEffect(Unit)` so `LoadAllSections` only fires when the VM has no data and no error; `remember`-cache the `.map { it.toListing() }` mapping. |
| `app/build.gradle.kts` + `app/src/main/res/raw/keep.xml` | F-15 | Enable `isShrinkResources = true` on the release variant; add a `<resources tools:keep="@string/google_maps_key" />` declaration since the key is looked up via `Resources.getIdentifier`. |

## Deliberately deferred

The following audit recommendations were not implemented in this PR
because they require either invasive refactoring or build-time validation
that is not available in this environment.

- **F-05 explicit `ImageRequest.size(...)`** — Coil 2.x's
  `ConstraintsSizeResolver` already downsamples to the view's pixel size
  when the modifier carries an explicit width/height (which is the case
  at every site reviewed). Passing raw pixels via
  `ImageRequest.Builder.size()` risks regressions on tablets and density
  buckets, so we keep the default resolver and rely on the
  placeholder-background change to remove the visible jank.
- **F-16 triple JSON stack (Gson + Moshi + kotlinx.serialization)** —
  removing any of the three requires touching ~15 modules, regenerating
  Moshi adapters, and a real release build to confirm nothing depended
  on Gson's lenient parsing. Tracked separately.
- **F-17 unused Maps SDK** — re-verified during implementation; the SDK
  *is* used in `app/.../feature/listingdetail/ListingDetailScreen.kt`
  and `feature/search/SearchScreen.kt`. The original audit was wrong on
  this point.
- **F-33 OkHttp `Authenticator`** — the hand-rolled refresh in
  `ApiClient.kt:397-420` already serialises concurrent refreshes via a
  mutex and disables recursive refresh, so the bug class is contained.
  Switching to an `Authenticator` is a follow-up.
- **BookingViewModel unit test** — `BookingRepository` is a final class
  with private constructor dependencies (`PaceDreamApiService`,
  `ApiClient`, `AppConfig`, `Json`, `BookingDao`). Writing a focused
  test of the stuck-loading path requires either extracting an
  interface or pulling in a mocking library, neither of which fits the
  "no business-logic change" scope. The fix itself is small and
  obviously correct: `firstOrNull { it !is Result.Loading }` cannot
  block on a `Result.Loading`-only cache flow, and the `finally` block
  guarantees `isLoading = false` even on cancellation.

## Before / after notes

Because there is no Android SDK in the audit environment, the deltas
below are static-analysis predictions, not measured numbers. Each
prediction names the script the team can run to confirm it.

### Compose stability / recomposition

- **Before:** the compose-compiler stability configuration named the
  Now-in-Android template package, so `RoomModel`, `RentedGearModel`,
  `SplitStayModel`, and the redesign `Listing` were all treated as
  *runtime-unknown*. Any state update in the host screen rebuilt every
  card in the visible LazyRow.
- **After:** the relevant packages are listed in
  `compose_compiler_config.conf`. Compose-compiler stability reports
  should now mark these data classes as `stable`. Combined with the
  `remember`-cached `.take(n)` slices and the stable
  `itemsIndexed` keys, the per-frame recomposition count of
  `PaceDreamPropertyCard` / `UnifiedListingCard` should drop to 1× per
  visible item on scroll fling instead of N× per frame.
- **Validate:** `./gradlew :app:assembleProdRelease -PenableComposeCompilerReports=true`
  then diff `app/build/compose_compiler/...` reports between the
  baseline and this branch.

### Startup timeout

- **Before:** `authSession.initialize()` could block the auth-state
  stream indefinitely on a flaky `/account/me` (the only ceiling was
  OkHttp's read timeout).
- **After:** capped at 8 s. On expiry, the UI falls through to whatever
  `AuthSession` exposes (cached user or `Unauthenticated`) and a Timber
  warning is logged. `currentUser.collect { … }` on the next line still
  reconciles when the network recovers.
- **Validate:** boot the app on airplane mode → main UI must render in
  ≤9 s with the unauthenticated empty state. A Macrobenchmark
  `coldStartup` test would quantify the change.

### Image requests

- **Before:** `AsyncImage` calls in the home cards rendered against
  transparent backgrounds, so a slow network produced a visible
  empty-card flash and (depending on parent constraints) a layout
  shift. `ImageLoader` ignored `Cache-Control`.
- **After:**
  - Every home-feed image now sits inside a sized `Box.background(Gray100)` so
    the slot is never transparent.
  - `ImageLoader.respectCacheHeaders(true)` lets the CDN's existing
    `max-age` decide cache freshness. Repeat sessions should serve more
    304s.
  - Coil still drives bitmap dimensions from view constraints (no
    explicit `ImageRequest.size(...)`).
- **Validate:** Charles proxy in front of a release build → second
  launch shows 304s / disk hits on previously-seen listing photos.

### Stuck loading fix

- **Before:** `loadBookings()` on the cache-fallback path used
  `.collect { … }` and only set `isLoading = false` on `Result.Success`
  / `Result.Error`. If the cache flow emitted `Result.Loading` and then
  closed (cache empty + flow terminator), the spinner stayed up
  forever.
- **After:** `.firstOrNull { it !is Result.Loading }` returns either a
  terminal value or `null` (flow closed without one). All branches
  drive `isLoading = false`; a top-level `finally` adds belt-and-braces
  in case of cancellation.
- **Validate:** UI test driving a `FakeBookingRepository` that emits
  `Loading` and then completes → screen must show the API-error
  retry, not a spinner.

### APK size impact

- **Before:** release build runs R8/ProGuard (`isMinifyEnabled = true`)
  but leaves all resources packaged.
- **After:** `isShrinkResources = true` will trim unreferenced
  drawables and string resources from the APK. The audit estimated
  200–500 KB savings; the exact number depends on how many res entries
  R8 can prove unreachable. `app/src/main/res/raw/keep.xml` keeps
  `@string/google_maps_key` from being stripped because it's accessed
  via `Resources.getIdentifier` in `core/location`.
- **Validate:** `./gradlew :app:assembleProdRelease` before vs after,
  compare via `apkanalyzer apk file-size app/build/outputs/apk/prod/release/*.apk`
  and `apkanalyzer apk compare … …`.

## Remaining risks

1. **`compose_compiler_config.conf` correctness** — marking a class as
   stable is a *promise* to the compiler. If any of the listed data
   classes contains a field of a runtime-unstable type that we missed,
   recomposition can skip when it shouldn't, leading to UI not updating
   on state change. The packages chosen contain only `val` properties
   of stable types, but a future field added to one of them (e.g. a
   `MutableList`) would break the contract. Mitigation: when adding
   fields to model classes, prefer `kotlinx.collections.immutable` over
   `MutableList`.
2. **`isShrinkResources = true` plus `Resources.getIdentifier`** — the
   `keep.xml` only protects `google_maps_key`. If anyone in the future
   adds another resource lookup by name, they must add it to the keep
   list. Mitigation: grep for `getIdentifier` in code review.
3. **`needsInitialLoad` gate in HomeRedesignRoute** — the condition
   considers the home empty *and* not loading *and* error-free. A
   recently-failed load (with the user still on Home) will not be
   retried automatically; the user must pull to refresh. This matches
   the previous behaviour for retries, but the previous code at least
   re-issued the call on screen recreate. If the team wants a "stale
   data → background revalidate" pattern (Airbnb-style), it should be
   added at the ViewModel level.
4. **`BookingViewModel.loadBookings()` cache `null` path now surfaces
   the original API exception** — this changes a corner-case message
   from "loaded an empty cache without an error" to "show the API
   error." If product expects a silent empty state on this path, the
   `else` branch should fall back to `Result.Success(emptyList())`
   instead. The new behaviour is more conservative for the user (they
   see something is wrong) but worth confirming.
5. **`withTimeoutOrNull` on a `suspend` block that catches its own
   exceptions** — `authSession.initialize()` swallows exceptions
   internally, so the timeout is the only way for the bootstrap to be
   "interrupted." That's fine, but if `initialize()` ever changes to
   re-throw, the timeout will return `null` and the outer `try/catch`
   will log it as "failed."

## Validation commands

```bash
# Compose compiler stability reports (requires Android SDK).
./gradlew :app:assembleProdRelease -PenableComposeCompilerReports=true

# Run unit tests for the modules touched.
./gradlew :feature:booking:test :feature:home:presentation:test :app:testDebugUnitTest

# Build + APK diff.
./gradlew :app:assembleProdRelease
apkanalyzer apk file-size app/build/outputs/apk/prod/release/*.apk
```
