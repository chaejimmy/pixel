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

## Progress log

| Date | Task | Status | Notes |
|---|---|---|---|
| 2026-04-19 | #15 — wire profile TODOs (logout, host edit, host thread click) | ✅ Verified already wired | Audit finding was stale: `DashboardNavigation.kt:544/1090`, `HostNavigationGraph.kt:158/211` are all connected. |
| 2026-04-19 | #7 — standardize corner radii | ✅ Done | `SearchScreen.kt` shimmer (×3), `BiddingScreen.kt:364` bottom-sheet shape, `IDVerificationScreen.kt:231/258` card shapes all swapped to `PaceDreamRadius.XS/LG/SM`. |
| 2026-04-19 | #6 — BookingTabScreen status colors | ✅ Done | Replaced hex tints with `PaceDreamColors.Yellow/Blue/Green/Red/Gray500`; left dark-foreground text hex untouched with a note about AA contrast intent. |
| 2026-04-19 | #3 — Interactive-only a11y sweep | ✅ Partial (targeted) | Fixed two real bugs: `HomeScreen.kt:949` favorite Surface had a null `contentDescription` (siblings at `:1289` / `:1491` were already correct — brought this one into line). `ReviewsScreen.kt:835` / `:902` star-rating `IconButton`s announced nothing — now announce "Rate N star(s)". All other audited `null` cases are decorative icons paired with readable text and were intentionally left unchanged. |

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

3. **Accessibility — interactive-only `contentDescription` audit**
   - ✅ Partial pass 2026-04-19: mapped all `IconButton`, `Surface(onClick=)`,
     `.clickable(...)` sites across `feature/` and `app/feature/`. Total raw
     `contentDescription = null` count is 310, but the overwhelming majority
     are decorative icons sitting next to readable `Text` in the same Row /
     Column — those are correctly `null` (labelling them would make TalkBack
     double-read "Star, 4.8" etc.).
   - Real interactive bugs found and fixed:
     - `HomeScreen.kt:949` — the Home hero-card favourite `Surface(onClick=)`
       had a null-labelled heart icon. Fixed to match sibling cards at
       `:1289` / `:1491` which already toggle between "Add to favorites"
       and "Remove from favorites".
     - `ReviewsScreen.kt:835` & `:902` — the five star-rating `IconButton`s
       in the write-review sheets shared the same unlabelled icon. Fixed to
       announce "Rate N star(s)".
   - Open follow-ups (still P1 but lower urgency than originally logged):
     - Add `Role.Button` via `Modifier.semantics` to `.clickable` containers
       on Home (hero search bar, notification pill) so TalkBack announces
       "double-tap to activate" — they currently get the default click role
       but not an explicit button role. Low risk, cosmetic for screen-reader
       users.
     - Consider a design-system `FavoriteIconButton` component to prevent
       this null-label class of regression (three near-identical heart
       buttons exist across Home alone).

### P1 — Core design-system hygiene

4. **Add `testTag` to key interactive surfaces** (Home, Search, Bookings,
   ListingDetail, BookingTab). Zero feature screens currently tag anything —
   UI automation has nothing to grab.

5. **Migrate `DestinationScreen` to design-system components**
   - `app/src/main/kotlin/com/pacedream/app/feature/destination/DestinationScreen.kt`
   - Raw `OutlinedTextField`, `Button`, `Card` + multiple `RoundedCornerShape(dp)`
     → `PaceDreamSearchBar`, `EnhancedCard`, `PaceDreamRadius.*`.

6. ~~**Fix `BookingTabScreen` status colors**~~ — ✅ done 2026-04-19. Follow-up:
   derive a proper dark-mode foreground palette so the `Color(0xFF8C6600)`
   contrast colors can also be tokenised.

7. ~~**Standardize corner radii**~~ — ✅ done 2026-04-19 for Search/Bidding/ID
   Verification. Still to do: `DestinationScreen:309` (use `CircleShape`
   instead of `RoundedCornerShape(28.dp)` on the 56 dp avatar) and a repo-wide
   sweep for any remaining `RoundedCornerShape(Xdp)` in the legacy `feature/`
   modules.

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
