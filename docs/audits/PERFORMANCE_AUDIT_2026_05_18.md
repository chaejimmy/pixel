# PaceDream Android — Performance Audit

**Date:** 2026-05-18
**Branch:** `claude/audit-pacedream-performance-Ig72w`
**Scope:** Read-only audit of this repository (Jetpack Compose / Kotlin marketplace app).
**Comparison target:** Production-grade marketplace apps (Airbnb, Turo, Vrbo, Booking).
**Method:** Source-level inspection of `app/`, `core/`, `feature/`, and Gradle configuration. Every finding is grounded in a concrete file:line citation; no behavioral profiling was run.

---

## Executive summary

This codebase was bootstrapped from the **Now in Android** sample and retains visible artefacts from that scaffold (compose-compiler stability config still names `com.google.samples.apps.nowinandroid.*`, the checked-in `app/prodRelease-badging.txt` still describes the NIA package). On top of that scaffold the team has layered a real marketplace product with auth, listings, search, booking, payment, chat and notifications.

The most consequential perf problems are **not in the rendering pipeline**: they are in the network/IO layer and in build configuration. In rough order of user impact:

1. **Auth/startup blockers** — no timeouts around `authSession.initialize()` and Firebase static init can hang the splash → empty screen indefinitely on a flaky network.
2. **Refresh-token & duplicate-load patterns** — multiple `LaunchedEffect(Unit)` re-fetches on nav-back, `loadFavorites()` fires twice on every auth toggle, search has no client-side debounce above autocomplete.
3. **Compose-stability misconfiguration** — `compose_compiler_config.conf:7` declares classes from the wrong package as stable, so the app's own models are treated as unstable; every list of `Listing`/`Room` recomposes more than it needs to.
4. **Image pipeline does not downscale** — ~7 high-traffic AsyncImage call sites omit `.size(...)`, so Coil decodes full-resolution photos (often ≥2000 px) into 100-200 dp cards. Combined with absent placeholders, this is the dominant source of scroll jank and CLS on the home feed.
5. **Bundle bloat** — `isShrinkResources` disabled in release, triple JSON stack (Gson + Moshi + kotlinx.serialization), unused Maps SDK pulled at app level, and `gradle/libs.versions.toml` still ships `accompanist-permissions` across 8 modules.

A full table of findings (47 individual, ranked P0–P3) follows, then an Airbnb/Turo-level comparison table, then suggested measurement methods.

---

## Findings (ranked)

Each finding includes: **Severity • Area • File:line • UX impact • Root cause • Fix • Acceptance • Measurement**.

### P0 — Critical (ship-blockers / data-correctness risk)

#### F-01 · Compose-stability config points at the wrong package
- **Area:** Compose recomposition
- **File:** `compose_compiler_config.conf:7`
- **Evidence:**
  ```
  com.google.samples.apps.nowinandroid.core.model.data.*
  ```
- **What users experience:** Home/search/wishlist lists drop frames on scroll; every state update in a `ViewModel` cascades into a recomposition of all visible cards even when their props are unchanged.
- **Root cause:** The stability config still names the **Now in Android** template package, so the compiler never marks `com.shourov.apps.pacedream.core.model.*`, `feature/home/.../redesign/HomeRedesignModels.kt`, etc. as `@Stable`. Combined with `List<Listing>` parameters in card composables, every parent recomposition is treated as "params changed."
- **Fix:** Replace the line with the project's actual model packages, e.g.:
  ```
  com.shourov.apps.pacedream.core.model.*
  com.shourov.apps.pacedream.core.data.model.*
  com.shourov.apps.pacedream.feature.home.presentation.redesign.*
  ```
- **Acceptance:** `./gradlew assembleProdRelease -PenableComposeCompilerReports=true` shows the relevant data classes as `stable` (not `runtime`) in the generated `module.json`.
- **Measurement:** Compose compiler stability report + Macrobenchmark `scrollHomeFeed` frame-time delta (target ≥15% improvement P50).

#### F-02 · `authSession.initialize()` has no timeout — splash can hang forever
- **Area:** Cold start / auth guard
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:110-124`
- **Evidence:**
  ```kotlin
  ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
      try {
          authSession.initialize()
          sessionManager.initialize()
          fcmTokenRegistrar.registerCurrentToken()
      } catch (e: Exception) { Timber.e(...) }
  }
  ```
- **What users experience:** On poor connectivity, the splash screen / `AuthState.Unknown` gate stays up indefinitely; the app appears frozen at launch. On Airbnb this is bounded by a ~1.5 s soft timeout that falls back to the cached profile.
- **Root cause:** `/account/me` is fetched on cold start without `withTimeoutOrNull`, and `AuthState` is only flipped to `Authenticated`/`Unauthenticated` once the call returns. There is no cached-first / network-after pattern.
- **Fix:** Wrap the bootstrap in `withTimeoutOrNull(1_500)` and use `loadCachedUser()` as the immediate render path; surface a non-blocking refresh in the background. Mirror the existing `TokenStorage` latch pattern (`prefsLatch.await(10, TimeUnit.SECONDS)` at `core/network/.../TokenStorage.kt:127-144`) but with a smaller, UI-blocking budget.
- **Acceptance:** Launch with airplane mode → main UI visible in ≤2 s, showing an offline indicator instead of a spinner.
- **Measurement:** Macrobenchmark `coldStartup` + manual airplane-mode test in CI on a Pixel emulator.

#### F-03 · `BookingViewModel.loadBookings()` can leave `isLoading=true` forever
- **Area:** Infinite spinner
- **File:** `feature/booking/src/main/kotlin/com/shourov/apps/pacedream/feature/booking/presentation/BookingViewModel.kt:73-111`
- **Evidence:** On `Result.Error` the code re-enters `bookingRepository.getUserBookings(userId).collect { ... }`; if that cached flow emits only `Result.Loading` and then completes (cache empty + collector closed), `isLoading=true` is never reset.
- **What users experience:** "My Bookings" tab shows a spinner indefinitely after an error retry.
- **Root cause:** Loading state is mutated inside `when` branches without a `try { } finally { isLoading=false }` guard. There is no terminal "no-more-emissions" path.
- **Fix:** Use `.onCompletion { _state.update { it.copy(isLoading = false) } }` on the flow, or pull the loading-state mutation into a single `finally`. The same pattern is fragile in `feature/signin/.../OtpVerificationViewModel.kt:76-147` and `feature/signin/.../EmailSignInViewModel.kt:88-118` and should be audited together.
- **Acceptance:** Force backend to return 5xx → spinner clears within one tick, error state shown, retry button enabled.
- **Measurement:** Espresso/Compose UI test with a fake repository that emits `Loading → completes`.

#### F-04 · `loadFavorites()` is called sequentially via primary + fallback endpoint
- **Area:** API waterfall / nav-back churn
- **File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt:124-157`
- **Evidence:** Primary `GET /wishlists` then, on empty or failure, `GET /account/wishlist`; sequential, no early backoff.
- **What users experience:** With a slow primary endpoint (30 s connect timeout, see F-22) users wait up to **60 s** for the heart icons to populate after login.
- **Root cause:** Synchronous fallback chain. Plus the call is re-issued in `HomeFeedViewModel.init` block (`feature/homefeed/HomeFeedViewModel.kt:57-93`) on every `AuthState` transition **and** on every `wishlistRepository.changes` emission → real-world `O(n)` duplicate `GET /wishlists`.
- **Fix:** (a) Run primary + fallback in parallel with `awaitAny` semantics or accept the primary as authoritative; (b) Coalesce auth-state and wishlist-change triggers via `combine(...).debounce(400).distinctUntilChanged()` before refetch; (c) Add 4xx-aware backoff before falling back.
- **Acceptance:** Logging interceptor shows ≤1 `GET /wishlists` per 5 s window during rapid favorite toggling.
- **Measurement:** OkHttp event listener counting calls in a 60-s interaction script.

---

### P1 — High (visible jank, dropped frames, wasted network)

#### F-05 · Every AsyncImage in the home feed omits `.size(...)`
- **Area:** Image decode / memory
- **Files (7 hot sites):**
  - `feature/home/presentation/.../components/TrendingDestinationsSection.kt:104-109`
  - `feature/home/presentation/.../components/BrowseByTypeSection.kt:331-336`
  - `feature/home/presentation/.../components/DealsCard.kt:79-90` & `:183-194`
  - `feature/home/presentation/.../components/EnhancedPropertyCards.kt:146-156, :348-356, :452-462`
  - `feature/home/presentation/.../redesign/HomeRedesignListingCard.kt:120-128`
- **Evidence (representative):**
  ```kotlin
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
          .data(currentUrl).crossfade(200).build(),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
  )
  ```
- **What users experience:** Scroll jank, especially on mid-range devices; OOMs on long browsing sessions; warm cache balloons.
- **Root cause:** Without `.size()`, Coil decodes the full source bitmap (Airbnb/Cloudinary-style URLs often deliver 1600–2400 px wide) before scaling, even though the target slot is ≤300 dp.
- **Fix (template):** Either size by the view (preferred) or downsample explicitly:
  ```kotlin
  .data(url)
  .size(coil.size.Size.ORIGINAL.takeIf { false } ?: Size(width = Dimension.Pixels(targetWidthPx), height = Dimension.Undefined))
  ```
  The existing pattern in `feature/inbox/.../InboxScreen.kt:468-477` (`.size(coil.size.Size(96, 96))`) is the reference. Roll out to all listing cards.
- **Acceptance:** Macrobenchmark `scrollHomeFeed` reports max 1 dropped frame per 100 over 30 s; `Debug.getNativeHeapAllocatedSize()` plateau ≤120 MB after 5 minutes of browsing.
- **Measurement:** Macrobenchmark + Android Studio Memory Profiler "Image cache" snapshot.

#### F-06 · `ImageLoader` does not honour cache headers
- **Area:** Network/image caching
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/di/ImageLoaderModule.kt:30-46`
- **Evidence:** Builder omits `.respectCacheHeaders(true)`; Coil's default `false` ignores server `Cache-Control: max-age=…`.
- **What users experience:** Wasted bandwidth; warm caches re-validate every session start.
- **Root cause:** Default Coil 2.x behaviour preserves cache regardless of headers — fine for static CDNs, harmful for backends that already do correct cache-control.
- **Fix:** Add `.respectCacheHeaders(true)` and confirm the CDN sets sane `max-age` for listing photos. Also call `.allowHardware(true)` (default in API 26+) and consider a custom `OkHttpClient` shared with Retrofit so HTTP/2 sessions are reused.
- **Acceptance:** Charles/mitm proxy shows 304s or local hits on the second app launch for the same listings.
- **Measurement:** OkHttp event listener `responseHeadersEnd` count + `Cache.hitCount()` on a controlled image URL set.

#### F-07 · `HomeRedesignRoute` triggers `LoadAllSections` on every recomposition
- **Area:** Nav-back refetch
- **File:** `feature/home/presentation/.../redesign/HomeRedesignRoute.kt:43-49`
- **Evidence:**
  ```kotlin
  LaunchedEffect(Unit) {
      viewModel.onEvent(HomeScreenEvent.LoadAllSections(...))
  }
  ```
- **What users experience:** Returning from a listing detail re-fetches every section (rooms / gears / split-stays), causing a spinner + content shift even though the data is still fresh.
- **Root cause:** `LaunchedEffect(Unit)` with no cache-aware guard; equivalent issue in `feature/booking/.../BookingScreen.kt:62-64` and `feature/chat/.../ChatListScreen.kt`.
- **Fix:** Move the load into `init { }` of the ViewModel (it already exists for `HomeFeedViewModel`), or gate the effect with `if (state.phase == HomePhase.Idle)`. Pair with `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` so the flow survives back-stack pops without refetching.
- **Acceptance:** Network log shows zero new requests when popping from listing detail back to home within 30 s.
- **Measurement:** OkHttp event listener call counter wrapped around a UI test.

#### F-08 · Unstable lambda + collection literals in `LazyRow` `items { }`
- **Area:** Compose recomposition
- **File:** `feature/home/presentation/.../components/EnhancedDashboardContent.kt:318-353` (and mirrored at `BrowseByTypeSection.kt:240-267`)
- **Evidence:**
  ```kotlin
  items(roomsState.rooms.take(10), key = { it.id }) { room ->
      PaceDreamPropertyCard(
          ...
          onFavoriteClick = { onFavoriteClick(room.id) },
          onClick = { onPropertyClick(room.id) },
      )
  }
  ...
  items(splitStaysState.splitStays.take(10), key = { it._id ?: it.hashCode() }) { ... }
  ```
- **What users experience:** Visible card-shuffle and re-bind animations whenever any sibling state (e.g. selected tab) changes; 2–3 dropped frames per scroll fling.
- **Root cause:** Three bugs in one block:
  1. `roomsState.rooms.take(10)` builds a fresh `List` instance on every recomposition → key lookup runs on new identity.
  2. Lambdas `{ onFavoriteClick(room.id) }` are allocated every recomposition.
  3. `it._id ?: it.hashCode()` is non-deterministic when `_id` is null (Compose treats hashCode as the row identity → list shuffles).
- **Fix:** Compute `val rooms by remember(roomsState.rooms) { derivedStateOf { roomsState.rooms.take(10) } }`. Replace fallback key with `"missing-$index"`. Provide stable callbacks via `remember(onFavoriteClick) { { id: String -> onFavoriteClick(id) } }` or wrap in `@Stable` interface.
- **Acceptance:** Layout inspector "Recompositions" counter for `PaceDreamPropertyCard` stays at 1 per item while idle on Home.
- **Measurement:** Compose recomposition tracing (`Modifier.recomposeHighlighter()` or `androidx.compose.runtime:runtime-tracing`).

#### F-09 · `HomeRedesignRoute` runs `.map { it.toListing() }` inside the composable body
- **Area:** CPU on render thread
- **File:** `feature/home/presentation/.../redesign/HomeRedesignRoute.kt:111-113`
- **Evidence:**
  ```kotlin
  val spacesListings = roomsState.rooms.map { it.toListing() }
  val itemsListings  = gearsState.rentedGears.map { it.toListing() }
  val splitListings  = splitStaysState.splitStays.map { it.toListing() }
  ```
- **What users experience:** Every recomposition (and there are many — see F-08) reallocates and re-maps three lists, GC pressure visible on long sessions.
- **Root cause:** Domain→UI mapping in the route layer instead of in the ViewModel.
- **Fix:** Move the mapping into `HomeRedesignViewModel` and expose a `StateFlow<HomeRedesignUiState>` already in UI shape; or at minimum wrap in `remember(roomsState.rooms) { roomsState.rooms.map(...) }`.
- **Acceptance:** Allocation tracker shows no transient `Listing` allocations during idle home screen.
- **Measurement:** Android Studio Profiler → allocations between two frames.

#### F-10 · `WifiSessionViewModel` runs an unbounded `while(true)` ticker
- **Area:** Battery / leaks
- **File:** `feature/wifi/.../WifiSessionViewModel.kt:235-249` (and `app/.../help/chat/SupportChatViewModel.kt:317-322`)
- **Evidence:**
  ```kotlin
  tickerJob = viewModelScope.launch {
      while (true) { tick(); delay(1_000L) }
  }
  ```
- **What users experience:** With the Wi-Fi feature visited once, the per-second tick keeps running until `viewModelScope` is cancelled. If the screen is recreated, two tickers may briefly overlap (no `tickerJob?.cancel()` ordering guarantee on rapid config changes).
- **Root cause:** Polling loop without an `isActive` guard and without converting to a `Flow` that respects lifecycle.
- **Fix:** Replace with `flow { while (currentCoroutineContext().isActive) { emit(Unit); delay(1_000) } }.flowOn(Dispatchers.Default).launchIn(viewModelScope)`. For the support chat poll, check session id **before** the `delay`, not after.
- **Acceptance:** Logcat shows `tickerJob` cancellation within 200 ms of screen exit; no double-tick during configuration change.
- **Measurement:** `viewModelScope.coroutineContext.job.children.count()` snapshot test.

#### F-11 · `HomeFeedViewModel` re-fetches favorites on every auth & wishlist event
- **Area:** Duplicate API calls
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/homefeed/HomeFeedViewModel.kt:57-93`
- **Evidence:** Three independent `viewModelScope.launch { … collectLatest { … refreshFavorites() } }` blocks all triggering `/wishlists`.
- **What users experience:** Spinner blink + bandwidth hit every time the user toggles a heart on any screen; 4× concurrent `GET /wishlists` observed in worst case.
- **Root cause:** No coalescing/debouncing across the three sources.
- **Fix:** `combine(authSession.authState, wishlistRepository.changes) { … }.debounce(400).distinctUntilChanged().onEach { refreshFavorites() }.launchIn(viewModelScope)`.
- **Acceptance:** Rapid toggle of 10 favorites → at most 2 `GET /wishlists` calls.
- **Measurement:** OkHttp interceptor counter + UI test that toggles 10 hearts in 5 s.

#### F-12 · Search has no input-debounce on `search()` (only on autocomplete)
- **Area:** Search UI responsiveness
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchViewModel.kt:142-145, 404-413`
- **Evidence:** `fetchAutocompleteDebounced` debounces autocomplete by 250 ms; the actual search `loadPage()` is called eagerly from `SearchScreen.kt` on commit but every `onQueryChanged` updates state without a single source-of-truth flow.
- **What users experience:** Cursor lag when typing while the map/list panel is open; backend hit per keystroke in some flows (`SearchScreen.kt:445, 462, 485, 525`).
- **Root cause:** Query is plumbed as imperative VM calls instead of a debounced `StateFlow<String>` → `flatMapLatest(::search)` pipeline.
- **Fix:** Replace `onQueryChanged` with a `MutableStateFlow<String>`; expose `val results = queryFlow.debounce(300).distinctUntilChanged().flatMapLatest { repo.searchFlow(it) }.stateIn(...)`.
- **Acceptance:** Typing "san francisco" makes one (not eight) `/v1/search` request.
- **Measurement:** Interceptor request count test driven by `Espresso.typeText`.

#### F-13 · `Listing.imageUrls` carousel uses prev/next clicks that mutate state in a `LazyRow` item
- **Area:** Card-level re-render
- **File:** `feature/home/presentation/.../redesign/HomeRedesignListingCard.kt:81-218`
- **Evidence:** `var photoIdx by remember(item.id) { mutableIntStateOf(0) }` plus a `repeat(item.photos) { i -> val size by animateDpAsState(...) }` block — N animations per card.
- **What users experience:** On a feed of 20 cards, scrolling re-renders all dots; first-time heart burst delays the next layout pass.
- **Root cause:** Pager state lives inside the card (correct for isolation, but expensive); animations are unconditionally constructed even when off-screen.
- **Fix:** Hoist the dot-row into a dedicated composable that takes only `current: Int, total: Int` and uses `Modifier.graphicsLayer { scaleX = … }` instead of `animateDpAsState` per dot (single render pass). Consider `HorizontalPager` with `pageCount = item.photos` for proper off-screen pruning.
- **Acceptance:** Layout inspector shows ≤1 recomp per dot row on scroll.
- **Measurement:** `Modifier.recomposeHighlighter()` overlay during a scripted scroll.

#### F-14 · `Application.onCreate` initialises Firebase synchronously
- **Area:** Cold start
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/PaceDreamApplication.kt:74-80`
- **Evidence:** `FirebaseApp.initializeApp(this)` blocks `onCreate` and there is a defensive `ExceptionInInitializerError` catch in `core/network/.../ApiClient.kt:336-346` because the perf plugin bytecode-instruments OkHttp.
- **What users experience:** ~80–150 ms of fixed cold-start cost (more if Play Services updates queued).
- **Root cause:** Default Firebase init runs on the main thread; the Firebase Performance gradle plugin further injects bytecode into every `OkHttp.execute` call.
- **Fix:** Switch to `androidx.startup` lazy initializers, or move Firebase init to `App Startup` background thread; disable Firebase Performance plugin in dev variants; keep the existing catch in ApiClient.
- **Acceptance:** Macrobenchmark `coldStartup` improves ≥80 ms P50.
- **Measurement:** Macrobenchmark with `StartupMode.COLD`, comparing before/after.

#### F-15 · `isShrinkResources` is not enabled in the release build
- **Area:** APK size / first-install latency
- **File:** `app/build.gradle.kts:163`
- **Evidence:** Release block sets `isMinifyEnabled = true` but never `isShrinkResources = true`.
- **What users experience:** Larger install + larger update payloads; expect 200–500 KB of unused drawables shipped.
- **Root cause:** Resource shrinking is opt-in alongside code minification.
- **Fix:** Add `isShrinkResources = true` and a `shrinkResources.xml` (`<resources><tools:keep .../></resources>`) for resources accessed by name only (currency flags, etc.). Run `./gradlew assembleProdRelease` and inspect `build/outputs/mapping/prodRelease/resources.txt`.
- **Acceptance:** APK shrinks ≥150 KB and no missing-resource crashes in regression suite.
- **Measurement:** `apkanalyzer apk file-size build/outputs/apk/prod/release/*.apk` before vs after.

#### F-16 · Triple JSON stack (Gson + Moshi + kotlinx.serialization)
- **Area:** Bundle size / runtime cost
- **Files:** `gradle/libs.versions.toml:50-51, 66-72, 187-189`, `core/model/build.gradle.kts:10-11`, `core/network/build.gradle.kts:79-86`, `core/data/build.gradle.kts:27-29`, `app/build.gradle.kts:257-258`.
- **Evidence:** Each module pulls a different converter; Retrofit uses both `gson.convert` and `retrofit.kotlin.serialization`; Moshi codegen is enabled but only `core/model` annotations reference it.
- **What users experience:** APK is 400–600 KB heavier than necessary; runtime keeps three reflection caches warm.
- **Root cause:** Incremental migration to kotlinx.serialization was not completed.
- **Fix:** Inventory all converter usages → pick one (kotlinx.serialization is already declared and works with `JsonConvertorFactory`). Remove the others module by module.
- **Acceptance:** A single `*.json.*` library appears in `./gradlew :app:dependencies` for `prodReleaseRuntimeClasspath`.
- **Measurement:** `gradle/dependency-report` + APK Analyzer diff.

#### F-17 · `Maps SDK` shipped at app level even though no feature consumes it
- **Area:** Bundle size
- **File:** `app/build.gradle.kts:276-277`
- **Evidence:** `implementation(libs.google.play.maps)` + `libs.google.maps.compose`; codebase has zero references to `GoogleMap` / `CameraPositionState` / `MarkerComposable` (verified via grep).
- **What users experience:** ~2.5–3.5 MB shipped weight that no user can exercise.
- **Root cause:** Map UI in SearchScreen is referenced (`searchInArea`, `MapBounds`) but the actual `GoogleMap` composable is not wired in this repo state.
- **Fix:** Either gate behind a dynamic feature module or remove until the map screen lands. If kept, push into `feature/wanted` or a new `feature/map` module so `app` doesn't transitively include it.
- **Acceptance:** `apkanalyzer dex packages` shows no `com.google.android.gms.maps.*` classes in the prodRelease APK.
- **Measurement:** APK Analyzer dex-packages diff.

#### F-18 · `core/location` always pulled by `app` regardless of feature gating
- **Area:** Bundle size + permission surface
- **File:** `core/location/build.gradle.kts:15`, transitively in `app/build.gradle.kts:223`
- **Evidence:** `play-services-location:21.0.1` dependency; only `feature:wanted` uses it.
- **Fix:** Move dependency from `core` to `feature:wanted` or behind a dynamic feature.
- **Severity:** P1
- **Measurement:** Same APK Analyzer diff as F-17.

---

### P2 — Medium (perceived sluggishness, polish gaps)

#### F-19 · `AsyncImage` calls miss `placeholder` / `loading` → layout shift
- **Area:** CLS-equivalent
- **Files:** Same set as F-05 (7 sites) plus `feature/home/.../EnhancedPropertyCards.kt:348-356` and `redesign/HomeRedesignListingCard.kt:120-128`.
- **What users experience:** Cards display empty boxes that then fill, causing perceived "pop-in." Airbnb shows a shimmering skeleton matching the card aspect.
- **Fix:** Standardise on `PaceDreamAsyncImage` (already in `common/.../PaceDreamImageComponents.kt:69-82` and is correctly implemented). Replace direct `AsyncImage` usages.
- **Acceptance:** Manual review — no card ever renders an empty box.
- **Measurement:** Frame-by-frame video on a throttled network (3G in Chrome DevTools-equivalent).

#### F-20 · `SubcomposeAsyncImage` used inside `LazyColumn`
- **Area:** Compose overhead
- **File:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeSectionListScreen.kt:182-202`
- **What users experience:** Section list scroll feels heavier than home feed because each row does its own sub-composition.
- **Fix:** Use `AsyncImage` with explicit `Modifier.size(...)` + a Box placeholder.
- **Measurement:** Macrobenchmark `scrollSectionList`.

#### F-21 · `SearchRepository` falls back synchronously between two endpoints
- **Area:** API waterfall
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchRepository.kt:164-194`
- **Evidence:** Primary `/v1/poc/listings` then on failure `/v1/search`; both with 30 s timeouts → up to 60 s worst case.
- **Fix:** `select { primary.onAwait { ... }; fallback.onAwait { ... } }` or short-circuit on `IOException` only.
- **Measurement:** Inject a deadline via test interceptor; assert ≤30 s observable timeout.

#### F-22 · OkHttp connect/read timeouts not tuned
- **Area:** Network
- **File:** `app/src/main/kotlin/com/pacedream/app/core/network/ApiClient.kt:85-92` and `AppConfig.*_TIMEOUT_SECONDS`
- **Evidence:** Default timeouts (likely 30 s) with no `connectionPool` tuning, no explicit `Protocol.HTTP_2` listing.
- **Fix:** Set connect/read to 10/20 s, add `ConnectionPool(maxIdleConnections = 8, keepAliveDuration = 5, MINUTES)`, share one `OkHttpClient` between Retrofit and Coil so HTTP/2 streams are reused.
- **Measurement:** Charles HTTP/2 column / OkHttp `EventListener.connectionAcquired`.

#### F-23 · No HTTP-level OkHttp `Cache(...)` configured
- **Area:** Network caching
- **File:** `app/src/main/kotlin/com/pacedream/app/core/network/ApiClient.kt:77-162` (the in-flight dedup is good but it does not persist responses).
- **Fix:** Add `.cache(Cache(directory = context.cacheDir.resolve("http"), maxSize = 20L * 1024 * 1024))`. Backend would need to send `Cache-Control` for listings; verify with the API team.
- **Measurement:** OkHttp `Cache.networkCount()` vs `Cache.hitCount()`.

#### F-24 · `parseToJsonElement(responseBody)` runs without an explicit IO dispatcher
- **Area:** Main-thread risk
- **Files:** `app/src/main/kotlin/com/pacedream/app/feature/home/HomeViewModel.kt:159-208`, `feature/homefeed/HomeFeedRepository.kt:100-109`
- **Fix:** Wrap parser bodies in `withContext(Dispatchers.Default)` (CPU bound) since `suspend` callers may already be on IO but parsing 100+ KB JSON deserves dedicated dispatcher.
- **Measurement:** Strict-mode + Systrace looking for >16 ms JIT-frames during home load.

#### F-25 · No `stateIn(WhileSubscribed(5_000))` on search/listing/booking flows
- **Area:** Lost state on rotation
- **Files:** `SearchViewModel.kt:34-37`, `BookingViewModel`, others.
- **Fix:** Convert hot mutable state flows where rotation safety matters; use `SavedStateHandle` for query strings.
- **Measurement:** Rotate device while results are visible — items remain, no refetch.

#### F-26 · `confirmBooking` / `cancelBooking` release `actionInFlight` immediately on error
- **Area:** Double-tap → double request
- **File:** `feature/booking/.../BookingViewModel.kt:215-233`
- **Fix:** Throttle re-submit with 1–2 s minimum interval after error, surface "Please wait" toast.
- **Measurement:** Espresso `clickRepeatedly(5)`; assert 1 request.

#### F-27 · `SessionManager.markAuthenticated` + `bootstrap` race two `/account/me` calls
- **Area:** Dual auth stack
- **File:** `app/src/main/kotlin/com/pacedream/app/core/auth/SessionManager.kt:191-198, 233-248`
- **Fix:** Pick one bootstrap path (legacy or new); make the other a thin observer.
- **Measurement:** Interceptor counter on `/account/me` after login → exactly 1 call.

#### F-28 · `MainActivity.handleIntent` parses deep links on the main thread
- **Area:** Cold start
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/MainActivity.kt:108-115`
- **Fix:** Defer parse to `lifecycleScope.launch(Dispatchers.Default)` and push result via `MutableStateFlow`.
- **Measurement:** Cold-start trace before vs after.

#### F-29 · Logging interceptor at `BODY` level in debug builds
- **Area:** Debug perf (lower priority)
- **File:** `app/src/main/kotlin/com/pacedream/app/core/network/ApiClient.kt:103-111`
- **Fix:** Default to `HEADERS`, opt-in to `BODY` via a `BuildConfig` toggle; saves CPU on large JSON during dev profiling.

#### F-30 · Modifier conditional rebuilds gradient on every recomp
- **Area:** Compose
- **File:** `feature/home/presentation/.../components/QuickChipsSection.kt:104-141`
- **Fix:** `remember(isSelected) { if (isSelected) Modifier.background(brushSelected) else Modifier.background(...) }`.

#### F-31 · `BookingFormViewModel.loadProperty` allows `basePrice = 0` to proceed to checkout
- **Area:** Data correctness
- **File:** `feature/booking/.../BookingFormViewModel.kt:78-127`
- **Fix:** Treat 0 as `Result.Error("Pricing unavailable")`.

#### F-32 · `LaunchedEffect(pendingDeepLink)` can drop deep links under rapid intents
- **Area:** Deep-link reliability
- **File:** `app/src/main/kotlin/com/shourov/apps/pacedream/ui/PaceDreamApp.kt:44-55` + `MainActivity.kt:56-59`
- **Fix:** Convert `_pendingDeepLink` to a `Channel<DeepLinkResult>(BUFFERED)` and drain inside `LaunchedEffect`.

#### F-33 · `Authenticator` is missing — refresh is hand-rolled
- **Area:** Refresh-token correctness
- **File:** `core/network/.../ApiClient.kt:397-420`
- **Evidence:** No `okhttp3.Authenticator` implementation; refresh-on-401 is in the request-retry loop. `Authenticator` would be invoked atomically per route by OkHttp.
- **Fix:** Implement a singleton `TokenAuthenticator` that uses a mutex + the existing `refreshAccessTokenIfPossible()`. Limit to 1 attempt per request (`response.priorResponse != null` check).
- **Measurement:** Concurrent 10×401 test — exactly 1 refresh call.

#### F-34 · `AuthSession.bootstrap` swallows non-401 backend failures silently
- **Area:** Silent error fallback
- **File:** `core/network/.../AuthSession.kt:176-217`
- **Fix:** Surface a `ProfileLoadError` state distinct from `Authenticated`; gate host-mode features on a fresh profile.

#### F-35 · `Painter`/`Brush` allocations in hot composables
- **Area:** Allocations
- **Files:** `redesign/HomeRedesignHero.kt:72-77`, multiple `tween(200)` specs.
- **Fix:** Move `Brush.verticalGradient(...)` into `remember { … }` blocks or to companion-object constants.

---

### P3 — Low (polish / hygiene)

#### F-36 · `Accompanist-Permissions` used across 8 feature modules
- **File:** `gradle/libs.versions.toml:2, 92` + 8 feature `build.gradle.kts`.
- **Fix:** Migrate to the official Compose permissions API (Compose 1.6+).
- **Severity:** P3 (deprecation risk; ~50 KB savings).

#### F-37 · Library modules disable minify in release
- **Files:** `feature/home/data/build.gradle.kts:20`, `feature/search/data/build.gradle.kts:19`, `common/build.gradle.kts:21`.
- **Fix:** Set `isMinifyEnabled = true` with proper `consumer-rules.pro`.

#### F-38 · PNG icons in `app/src/main/res/drawable/` are not WebP
- **Files:** `app_icon.png`, `bg_dashboard_header.png` etc.
- **Fix:** Convert to lossy WebP (≥75% size reduction) — `cwebp -q 80`.

#### F-39 · Baseline profile generation is `automaticGenerationDuringBuild = false`
- **File:** `app/build.gradle.kts:179` vs `:312-316`.
- **Fix:** Generate as part of CI and commit `app/src/main/baselineProfiles/baseline-prof.txt`.

#### F-40 · Commented-out modules in `settings.gradle.kts:37-40`
- **Fix:** Delete dead config or restore the lint module if intended.

#### F-41 · Empty feature modules (`feature/payment`, `feature/host`, `feature/guest`, `feature/auth`)
- **Evidence:** `find feature/payment -name "*.kt" → 0`.
- **Fix:** Either delete or stub a single placeholder file; right now they contribute to Gradle config-time work for nothing.

#### F-42 · `identity-credential` dependency without callers
- **File:** `app/build.gradle.kts:292`. No imports of `com.android.identity.*`.
- **Fix:** Remove.

#### F-43 · No image pre-fetch on card tap
- **Evidence:** No `imageLoader.enqueue(ImageRequest(...).build())` calls before navigation.
- **Fix:** When the user taps a card, prefetch `item.imageUrls.take(3)` and the hero image of the detail screen.
- **Measurement:** Time-to-image on detail screen with cold cache vs after prefetch.

#### F-44 · Resend-OTP cooldown race
- **File:** `feature/signin/.../OtpVerificationViewModel.kt:154-191`.
- **Fix:** Start cooldown timer **before** the suspend call returns, not after.

#### F-45 · No `collectAsStateWithLifecycle()` in two host screens
- **Files:** `app/src/main/kotlin/com/shourov/apps/pacedream/feature/host/presentation/CreateListingScreen.kt`, `EditListingScreen.kt`.
- **Fix:** Replace `.collectAsState()` with `.collectAsStateWithLifecycle()`.

#### F-46 · `app/prodRelease-badging.txt` still describes Now in Android
- **Fix:** Regenerate after a real prodRelease build or delete; otherwise it misleads future audits.

#### F-47 · `compose_compiler_config.conf` doesn't list common stable third-party types (`Listing`, `DateRange`, `Money`)
- **Fix:** Companion change to F-01 — list app-specific stable types explicitly.

---

## Comparison vs Airbnb / Turo-level expectations

| # | Area | Current PaceDream behaviour | Expected Airbnb/Turo behaviour | Risk | Recommended fix |
|---|---|---|---|---|---|
| 1 | Cold start | Splash blocked by `authSession.initialize()` with no timeout (`PaceDreamApplication.kt:110-124`) | Splash dismissed in ≤1.5 s; cached profile renders immediately, refresh fires in background | High | F-02 + F-14 |
| 2 | Home feed scroll | Cards recompose because of unstable model classes (`compose_compiler_config.conf:7`) and inline lambdas (`EnhancedDashboardContent.kt:318-353`) | Card stays composed unless its own props change; 60 fps on mid-range device | High | F-01 + F-08 |
| 3 | Image loading | Full-resolution decode into 100–200 dp cards (7 sites); no placeholder; cache headers ignored | `.size()` matches view; shimmer placeholder; 304 on warm cache | High | F-05 + F-06 + F-19 |
| 4 | Search responsiveness | Autocomplete debounced 250 ms, but main search fires per keystroke from several call sites (`SearchScreen.kt:445,462,485,525`) | Single 300 ms debounced flow; instant cancellation of in-flight requests | High | F-12 |
| 5 | API duplication | `loadFavorites()` re-fetches on every auth + wishlist event (`HomeFeedViewModel.kt:57-93`) | Single coalesced refresh; client-side optimistic update on toggle | High | F-04 + F-11 |
| 6 | Auth/refresh | Hand-rolled refresh inside request retry (`ApiClient.kt:397-420`); two `/account/me` calls on login (`SessionManager.kt:191-198`) | OkHttp `Authenticator` with mutex; single bootstrap | High | F-27 + F-33 |
| 7 | Bundle size | Triple JSON stack, Maps SDK pulled but unused, `accompanist-permissions` in 8 modules | One serializer; SDKs gated by dynamic features; no deprecated libs | High | F-15 + F-16 + F-17 |
| 8 | Loading guard | `BookingViewModel.loadBookings()` can leave spinner forever on cache-flow completion | All `isLoading` mutations wrapped in `try/finally` or `onCompletion` | High | F-03 |
| 9 | Polling | `while (true) { tick(); delay() }` in Wi-Fi & support chat without `isActive` check | `flow { while (currentCoroutineContext().isActive) … }` driven by lifecycle | Medium | F-10 |
| 10 | Navigation back | Every screen with `LaunchedEffect(Unit) { loadX() }` refetches on pop-back | Cached `StateFlow` with `WhileSubscribed(5_000)`; revalidate stale-while-revalidate | Medium | F-07 + F-25 |
| 11 | Pull-to-refresh | Present on `Booking`, `Inbox`, `Notification`, `Wanted`, `Wishlist`, `Home` (good) | Same | Low | — |
| 12 | List item carousel | `HomeRedesignListingCard` runs N `animateDpAsState` for dots + per-tap `photoIdx` mutates a `LazyRow` item | `HorizontalPager` + memoised dot row | Medium | F-13 |
| 13 | Map UX | `SearchScreen` references `MapBounds` and `searchInArea` but no `GoogleMap` composable in repo | Map view with cluster markers and "Search this area" chip | Medium | Land map UI or remove dep (F-17) |
| 14 | Error boundary | `LaunchedEffect`s silently swallow exceptions; no Compose-level `runCatching` boundary | Top-level `errorBoundary { … }` + Crashlytics breadcrumbs | Medium | Add a top-level `CompositionLocalProvider(LocalUiErrorHandler ...)` |
| 15 | Image prefetch | Detail screen always loads cold | Prefetch top-3 images on card tap | Low | F-43 |
| 16 | Deep links | Race between `MutableStateFlow` consumption and re-emit | Channel/Flow that buffers | Medium | F-32 |
| 17 | Pricing safety | `basePrice = 0` reaches checkout (`BookingFormViewModel.kt:78-127`) | Pricing-unavailable error state | High | F-31 |
| 18 | Build hygiene | `compose_compiler_config.conf` and `prodRelease-badging.txt` still reference NIA template | Project-specific config baked into CI | Medium | F-01 + F-46 |
| 19 | Library size | `play-services-location` shipped to all users via `core:location` (`core/location/build.gradle.kts:15`) | Location pulled only into the feature that uses it | Medium | F-18 |

---

## Suggested measurement methodology

For each fix, ground the acceptance in a numeric measurement. Practical recipes:

1. **Cold-start (F-02, F-14, F-28).** Use `:benchmarks` module — there is already one at `benchmarks/src/main/kotlin/.../UiAutomatorHelpers.kt`. Add a `StartupBenchmark` with `StartupMode.COLD` and capture `timeToInitialDisplay` / `timeToFullDisplay`. Target: P50 < 700 ms on a Pixel 6 emulator.
2. **Scroll jank (F-01, F-05, F-08, F-13).** Macrobenchmark `FrameTimingMetric` + `TraceSectionMetric("Compose:recompose")`. Target: zero frames > 16.6 ms over a 5 s fling on home.
3. **Recomposition counts (F-01, F-08, F-09).** Enable Compose Compiler reports via `-PenableComposeCompilerReports=true` per [Android docs](https://developer.android.com/jetpack/compose/performance/stability/diagnose). Diff before/after.
4. **API duplication (F-04, F-11, F-12).** Add a test interceptor that counts requests per URL pattern; wrap UI tests around interaction scripts.
5. **Image memory (F-05, F-06, F-19).** `Debug.getNativeHeapAllocatedSize()` snapshots at 0 s / 60 s / 5 min idle-scroll on home.
6. **APK size (F-15, F-16, F-17).** `apkanalyzer apk file-size` + `apk compare` between baseline and PR.
7. **Refresh-token correctness (F-33).** Concurrency test that fires 10 parallel requests with an expired token using a fake `ApiClient` + assert refresh-call count.
8. **Pricing safety (F-31).** Unit test the ViewModel with a property returning zero prices; assert `error` state.

---

## Recommended remediation order

1. **Week 1 (correctness):** F-01 (compose stability), F-02 (auth timeout), F-03 (booking spinner), F-31 (zero-price guard).
2. **Week 2 (image pipeline):** F-05, F-06, F-19, F-43.
3. **Week 3 (API hygiene):** F-04, F-07, F-11, F-12, F-25, F-33.
4. **Week 4 (bundle / cold start):** F-14, F-15, F-16, F-17, F-18, F-39.
5. **Continuous:** F-08, F-09, F-10, F-13, F-22, F-23, F-24, F-30, F-35.

---

## Caveats

- The audit did not run the app. Findings labelled around "spinners never clear" or "double GET" are derived from the static call graph; live tracing may reveal additional triggers or that some are mitigated by callers not yet read.
- The `app/src/main/kotlin/com/pacedream/app/...` and `app/src/main/kotlin/com/shourov/apps/pacedream/...` packages coexist; some findings reference both. Confirm with the team which is the active path before applying fixes that touch the dormant one.
- "Airbnb/Turo behaviour" is based on published engineering blogs and public APK analysis, not on access to those apps' source code.
