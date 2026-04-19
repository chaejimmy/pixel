# Android Design QA Report

_Branch: `claude/android-design-qa-Wj4Ue` · Generated: 2026-04-19_

Audit of the Android Compose codebase against `DESIGN_SYSTEM_README.md`
(PaceDream tokens: `PaceDreamColors`, `PaceDreamSpacing`, `PaceDreamTypography`,
`PaceDreamRadius`) and against the iOS parity notes in
`UI_UX_COMPARISON.md` / `ANDROID_IOS_PARITY_AUDIT_2025_03_25.md`.

## Executive summary

| Category | Finding |
|---|---|
| Hardcoded `Color(0x…)` literals | 93 occurrences |
| `Color.White` / `Color.Black` in feature code | 100+ occurrences |
| `contentDescription = null` on `Icon` / `Image` | 115+ occurrences |
| Inline `fontSize = … .sp` instead of typography tokens | 12+ occurrences |
| Raw `.dp` padding (off-scale) | 97+ in `HomeScreen.kt` alone |
| `RoundedCornerShape(Xdp)` instead of `PaceDreamRadius.*` | 15+ |
| Screens using `isSystemInDarkTheme()` | 0 |
| Screens with `testTag` coverage | 0 feature screens |

Approximately **25–30% of feature code** deviates from the design system.

## Remaining tasks

### P0 — Release blockers

1. **Dark-theme support in `HomeScreen`**
   - `app/src/main/kotlin/com/pacedream/app/feature/home/HomeScreen.kt` (~line 86, 247–365)
   - Background hardcoded `Color(0xFFF8F8FA)`, 50+ `Color.White` overlays, shadows
     fixed to `Color.Black.copy(alpha = …)` — nothing adapts when the system is dark.
   - Fix: switch to `MaterialTheme.colorScheme.background / surface`, gate overlays
     and shadows on `isSystemInDarkTheme()`.

2. **Consolidate hardcoded colors in Home**
   - Same file, lines 638–645 (`CategoryCardData`), 406, 429, 512–524, 623, 682,
     712, 966, 984, 1016, 1025, 1608–1610.
   - Fix: route category/gradient colors through `PaceDreamColors.*` (or a
     `CategoryColors` object in the design-system module).

3. **Accessibility — `contentDescription` audit**
   - 115+ `contentDescription = null` sites. Hot spots:
     - `HomeScreen.kt` (19), `BookingsScreen.kt` (6), `ReviewsScreen.kt` (8),
       `ListingDetailScreen.kt` (5).
   - Fix: replace with semantic strings (or `stringResource(...)`) for every
     interactive icon; keep `null` only for purely decorative glyphs wrapped in
     a labeled parent.

### P1 — Core design-system hygiene

4. **Add `testTag` to key interactive surfaces** (Home, Search, Bookings,
   ListingDetail, BookingTab). Zero feature screens currently tag anything —
   UI automation has nothing to grab.

5. **Migrate `DestinationScreen` to design-system components**
   - `app/src/main/kotlin/com/pacedream/app/feature/destination/DestinationScreen.kt`
   - Raw `OutlinedTextField`, `Button`, `Card` + multiple `RoundedCornerShape(dp)`
     → `PaceDreamSearchBar`, `EnhancedCard`, `PaceDreamRadius.*`.

6. **Fix `BookingTabScreen` status colors**
   - `app/src/main/kotlin/com/shourov/apps/pacedream/feature/booking/presentation/BookingTabScreen.kt:527, 575–583`
   - Raw status hex (`0xFF59339A`, badge hex + alpha) → `PaceDreamColors.Warning/Info/Success`
     with dark-mode variants.

7. **Standardize corner radii** across `BiddingScreen`, `IDVerificationScreen`,
   `SearchScreen` (lines 610/622/630), `DestinationScreen`, `BlogScreen`.
   Replace `RoundedCornerShape(Xdp)` with `PaceDreamRadius.XS/SM/MD/LG/XL`.

8. **Dark-mode-aware card shadows**
   - Affects `HomeScreen.kt:364–365, 865–866, 1204–1205, 1406–1407` and every
     card across the app.
   - Introduce `Modifier.adaptiveShadow()` in the design-system module.

### P2 — Polish

9. **Migrate `TripPlannerScreen`** — 3× raw `OutlinedTextField`, inline
   `fontWeight = FontWeight.SemiBold`, custom `Card` colors → design-system
   equivalents + `PaceDreamTypography` named styles.

10. **Implement FAQ screen** — data models exist, UI does not. Gap from the
    website / iOS parity doc.

11. **Semantic labels on clickable containers** — wrap category pills, property
    cards, filter chips with `Modifier.semantics { role = Role.Button }`.

12. **Touch-target audit** — verify 48 dp minimum for Home category icons,
    icon buttons, chips.

13. **Consolidate `EnhancedSearchBar`**
    - `app/src/main/kotlin/com/pacedream/app/feature/search/EnhancedSearchBar.kt`
      (~220 lines of custom styling) → promote into `common/designsystem` as
      a `PaceDreamSearchBar` variant.

14. **Radius cleanup in `BlogScreen` / `SearchScreen`** — 8+ `RoundedCornerShape(4.dp)`
    → `PaceDreamRadius.XS`.

15. **iOS-parity: wire profile TODOs**
    - `DashboardNavigation.kt` (guest `onLogoutClick` is a no-op) →
      `SessionManager.signOut()`.
    - `HostNavigationGraph.kt:144–150` (`onEditProfileClick` TODO) → open edit
      sheet.
    - `HostInboxScreen.kt` `onThreadClick` is an empty lambda → wire to thread
      detail.

## Violations by feature module

| Module | Hardcoded colors | `Color.White/Black` | `testTag` | `contentDescription = null` | Dark-theme risk |
|---|---|---|---|---|---|
| home | 35+ | 50+ | none | 19 | critical |
| booking | 8 | 15+ | none | 6 | high |
| search | 12+ | 20+ | none | 8 | high |
| destination | 10+ | 12+ | none | 8 | high |
| listingdetail | 8 | 15+ | none | 5 | high |
| reviews | 5 | 10+ | none | 8 | medium |
| tripplanner | 4 | 8+ | none | 2 | medium |
| blog | 6 | 8+ | none | 4 | medium |
| bidding | 4 | 6+ | none | 2 | medium |

## Recommended sequencing

1. **Week 1 — P0:** dark-theme in `HomeScreen`, `contentDescription` sweep,
   `testTag` scaffolding on the 5 highest-traffic screens.
2. **Weeks 2–3 — P1:** `DestinationScreen` migration, `BookingTabScreen`
   status colors, corner-radius standardization, adaptive shadow modifier.
3. **Week 4+ — P2:** FAQ screen, semantic labels, touch-target audit,
   `EnhancedSearchBar` consolidation, profile-action TODOs.

Rough estimate: **160–200 engineer-hours** for the full 15-item list.

## Suggested guard-rails

- Add a Detekt / lint rule banning `Color(0x` and `fontSize = *.sp` inside
  `feature/**` (allow only in `core:designsystem`).
- Add a Compose preview of every screen in dark mode to the preview harness.
- Require `testTag` on top-level screen roots in PR review.
