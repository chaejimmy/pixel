# Design-system literal coverage by feature module

Counts of banned literals still present in each feature module's Kotlin
source. Sorted by total violations descending. Modules with zero
violations are listed last (alphabetised).

## Method

Every `*.kt` file under each module root was scanned for:

| Column | Regex | Replacement |
|---|---|---|
| `Color(0x…)` | `Color\(0x` | `PaceDreamColors.*` or `CategoryColors.*` |
| `Color.White/Black` | `\bColor\.White\b` + `\bColor\.Black\b` (summed) | `MaterialTheme.colorScheme.*`, `OnBrandSurface`, or `scrimOnImage(alpha)` |
| raw `.dp` paddings | `\.padding\(\s*\d+\.dp` **plus** `(horizontal|vertical|start|end|top|bottom)\s*=\s*\d+\.dp` | `PaceDreamSpacing.*` |
| `RoundedCornerShape(N.dp)` | `RoundedCornerShape\(\s*\d+\.dp` | `RoundedCornerShape(PaceDreamRadius.*)` |

Module roots covered:
- `feature/<name>/` (top-level multi-module dirs)
- `app/src/main/kotlin/com/pacedream/app/feature/<name>/` (app's new namespace, shown below as `app/pacedream.app/feature/<name>`)
- `app/src/main/kotlin/com/shourov/apps/pacedream/feature/<name>/` (app's legacy namespace, shown below as `app/shourov/feature/<name>`)

`RoundedCornerShape(PaceDreamRadius.LG)` is **not** counted — only the raw
`.dp` literal form. Module dirs that contain no Kotlin sources (empty
Gradle stubs `feature/auth`, `feature/guest`, `feature/host`,
`feature/payment`) are excluded from the table.

## Results

| Module | `Color(0x` | `Color.White`/`Black` | raw `.dp` padding | `RoundedCornerShape(N.dp)` | Total |
|---|---:|---:|---:|---:|---:|
| `feature/home/.../redesign` &nbsp;<sup>†</sup> | 30 | 38 | 41 | 0 | **109** |
| `app/pacedream.app/feature/listingdetail` | 30 | 13 | 0 | 0 | **43** |
| `feature/booking` | 17 | 0 | 18 | 0 | **35** |
| `app/pacedream.app/feature/inbox` | 0 | 6 | 20 | 0 | **26** |
| `app/shourov/feature/search` | 5 | 14 | 4 | 0 | **23** |
| `app/pacedream.app/feature/settings` | 2 | 2 | 19 | 0 | **23** |
| `app/shourov/feature/propertydetail` | 3 | 9 | 9 | 0 | **21** |
| `feature/wanted` | 0 | 0 | 16 | 4 | **20** |
| `app/pacedream.app/feature/profile` | 0 | 8 | 12 | 0 | **20** |
| `app/shourov/feature/notification` | 10 | 0 | 8 | 0 | **18** |
| `app/shourov/feature/booking` | 7 | 3 | 8 | 0 | **18** |
| `feature/inbox` | 5 | 9 | 3 | 0 | **17** |
| `feature/signin` | 0 | 0 | 16 | 0 | **16** |
| `app/shourov/feature/profile` | 0 | 5 | 9 | 0 | **14** |
| `app/pacedream.app/feature/search` | 4 | 10 | 0 | 0 | **14** |
| `app/pacedream.app/feature/bookings` | 10 | 4 | 0 | 0 | **14** |
| `feature/chat` | 0 | 6 | 3 | 4 | **13** |
| `app/shourov/feature/homefeed` | 0 | 8 | 5 | 0 | **13** |
| `app/pacedream.app/feature/checkout` | 0 | 0 | 13 | 0 | **13** |
| `app/pacedream.app/feature/collections` | 0 | 0 | 10 | 0 | **10** |
| `app/pacedream.app/feature/verification` | 0 | 0 | 7 | 0 | **7** |
| `app/pacedream.app/feature/splitbooking` | 2 | 2 | 3 | 0 | **7** |
| `feature/wishlist` | 0 | 4 | 0 | 2 | **6** |
| `app/pacedream.app/feature/bidding` | 3 | 1 | 2 | 0 | **6** |
| `app/pacedream.app/feature/reviews` | 0 | 5 | 0 | 0 | **5** |
| `app/pacedream.app/feature/home` | 0 | 0 | 5 | 0 | **5** |
| `app/pacedream.app/feature/destination` | 0 | 5 | 0 | 0 | **5** |
| `app/pacedream.app/feature/tripplanner` | 0 | 2 | 2 | 0 | **4** |
| `feature/webflow` | 3 | 0 | 0 | 0 | **3** |
| `app/pacedream.app/feature/blog` | 0 | 1 | 2 | 0 | **3** |
| `feature/create-account` | 0 | 0 | 2 | 0 | **2** |
| `feature/wifi` | 0 | 0 | 1 | 0 | **1** |
| `app/shourov/feature/help` | 1 | 0 | 0 | 0 | **1** |
| `app/pacedream.app/feature/wishlist` | 0 | 0 | 1 | 0 | **1** |
| `app/pacedream.app/feature/about` | 0 | 0 | 1 | 0 | **1** |
| `feature/search` | 0 | 0 | 0 | 0 | **0** |
| `feature/notifications` | 0 | 0 | 0 | 0 | **0** |
| `feature/notification` | 0 | 0 | 0 | 0 | **0** |
| `app/shourov/feature/payment` | 0 | 0 | 0 | 0 | **0** |
| `app/shourov/feature/destinations` | 0 | 0 | 0 | 0 | **0** |
| `app/shourov/feature/bookingdetail` | 0 | 0 | 0 | 0 | **0** |
| `app/pacedream.app/feature/listing` | 0 | 0 | 0 | 0 | **0** |
| `app/pacedream.app/feature/hostprofile` | 0 | 0 | 0 | 0 | **0** |
| `app/pacedream.app/feature/faq` | 0 | 0 | 0 | 0 | **0** |
| `feature/home` (excl. `redesign/`) &nbsp;<sup>†</sup> | 0 | 0 | 0 | 0 | **0** |
| `app/shourov/feature/host` &nbsp;<sup>§</sup> | 0 | 0 | 0 | 0 | **0** |

<sup>†</sup> `feature/home` was migrated in `refactor/ds-migration-home` — the 13
component files under `feature/home/.../presentation/components/` are now
token-driven and protected by `designSystemCheck` (the row was previously
**210**).  The `redesign/` subtree underneath was deliberately carved out
of the migration because `HomeRedesignTheme.kt` defines a self-contained
internal palette (Purple + Coral ramps + Paper/Ink tokens) that cannot
be expanded into design-system tokens under the "no new brand colors"
rule; the carve-out is enforced in `build.gradle.kts` via `scanExcludes`.
A follow-up PR will either map the redesign palette onto existing
PaceDream tokens or formally adopt it as its own scoped theme module.

<sup>§</sup> `app/shourov/feature/host` was migrated in
`refactor/ds-migration-host` (previously **167** hits).  Mapping
highlights: the four private `Slot{Green,Blue,Red,Amber}` iOS-system
hex constants in `ListingCalendarScreen.kt` now resolve through
`PaceDreamColors.{Success,Info,Error,Warning}`; the two
`setToolbarColor` / `setNavigationBarColor` Int callers in
`HostEarningsScreen.kt` use `PaceDreamColors.Primary.toArgb()`; the
seven `#FFA500` warning-orange hits map to `PaceDreamColors.Warning`.
No subtree carve-outs — host has no `redesign/`.

## Totals

| Column | Sum across all modules |
|---|---:|
| `Color(0x…)` literals | 132 |
| `Color.White` / `Color.Black` | 155 |
| raw `.dp` paddings | 240 |
| `RoundedCornerShape(N.dp)` | 10 &nbsp;<sup>‡</sup> |
| **Grand total** | **537** |

(Down from 805 at commit `8d93034` — `refactor/ds-migration-home`
cleared 101 hits in `feature/home` outside `redesign/`;
`refactor/ds-migration-host` cleared 167 hits in `app/shourov/feature/host`;
the deferred **109** hits inside `feature/home/.../redesign/` are still
counted in the table row for that subtree so the grand total reflects
everything currently in code.)

<sup>‡</sup> `feature/wanted`, `feature/chat`, and `feature/wishlist`
were migrated by `refactor/ds-radius-cleanup` (PR #494) and no longer
contain any `RoundedCornerShape(N.dp)` literals.  Their rows above
still show the pre-merge counts and will be reconciled in the next
catch-all doc refresh — the merged code is the ground truth, and CI
enforces zero via STRICT mode regardless.

## Observations

1. **Both giants are done.**  `feature/home` (210) and
   `app/shourov/feature/host` (167) — the two modules the original
   coverage report flagged as ~47 % of total debt — are now fully
   migrated outside their carve-outs.  The largest single remaining
   row is the deferred `feature/home/.../redesign/` subtree at 109
   hits, followed by `app/pacedream.app/feature/listingdetail` at 43.
2. **The rebrand is uniformly applied across the migrated surface.**
   Both `feature/home` routes (legacy + app/pacedream.app namespace),
   the host module, and the Chrome Custom Tabs chrome bar opened from
   host's earnings flow now all read through `PaceDreamColors.Primary`
   (the green `#336633` per the rebrand documented in `Color.kt:452`).
3. **`RoundedCornerShape(N.dp)` is fully eradicated repo-wide** (10 → 0)
   and locked in by STRICT mode in `designSystemCheck`.  Any new hit
   anywhere in `feature/**` fails CI.
4. **17 source roots are now in `scanRoots`** — original 15 zero-row
   modules + `feature/home` (with `redesign/` excluded) +
   `app/shourov/feature/host`.
5. **`.dp` padding still leads the remaining debt** at 240 of 537 hits
   (≈ 45 %).  The per-value histogram + once-per-cluster rounding
   decision used in `feature/home` and `host` is reusable for the next
   medium-debt modules.

## Suggested next migration order

Pure leverage / impact ratio (post-host):

1. **`app/pacedream.app/feature/listingdetail`** (43) — 30 hex literals,
   all on image overlays; mirror the Phase 3 / feature/home pattern
   (`scrimOnImage` + `OnBrandSurface`).
2. **`feature/booking`** (35) and **`app/pacedream.app/feature/inbox`**
   (26) — medium-debt modules with a clean mix of hex + paddings.
3. **`app/shourov/feature/search`** (23) and **`app/pacedream.app/feature/settings`** (23)
   — similar shape; can be batched together if convenient.
4. **`feature/home/.../redesign/`** (109, currently carved out) — needs
   the design call on whether the Purple/Coral palette gets adopted
   into `PaceDreamColors` or stays a scoped internal theme.

---

_Originally generated by scanning Kotlin sources on commit_ `8d93034`
_(then the HEAD of `claude/optimize-pacedream-ux-7rC8D`).  Updated for
the `feature/home` row by `refactor/ds-migration-home` and for the
`app/shourov/feature/host` row by `refactor/ds-migration-host`._

## Guard-rails (prevent the debt from regrowing)

`chore(ci): ban hex colors + inline .sp in feature code` flipped
`designSystemCheck` from a narrow allowlist (only migrated modules
scanned) to a **deny-by-default** policy across every
`feature/**` and `app/src/main/kotlin/.../feature/**` Kotlin file.
Both the `Color.White` / `Color.Black` rules and the four
`Color(0x…)` / `fontSize = N.sp` / `RoundedCornerShape(N.dp)` rules
now run repo-wide on the standard `check` lifecycle, so a future PR
that introduces any of them inside feature code fails CI.

To unblock the rollout without forcing a 537-hit migration in one PR,
the 45 currently-violating files have been opted out with a top-of-file
`// @DesignSystemEscape (reason="…")` marker.  The escape table:

| Escape reason                                                          | File count |
|---|---:|
| `legacy debt tracked in DESIGN_SYSTEM_COVERAGE.md — migrate per the suggested order in that file before removing this opt-out` | 40 |
| `HomeRedesign owns a self-contained Purple/Coral palette pending a design call — see DESIGN_SYSTEM_COVERAGE.md` | 5 |

Removing an opt-out is the migration unit going forward: delete the
`@DesignSystemEscape` marker once the file is clean, re-run
`./gradlew designSystemCheck`, and the gate locks the zero-violation
state in.

A dark-mode `@Preview` snapshot gate (`darkModeSnapshotCheck`,
backed by Roborazzi) is wired alongside `designSystemCheck`.  Each new
screen-level composable should ship with a paired
`*ScreenDarkModeSnapshotTest.kt` so dark-mode regressions surface on
the same `check` step (see `DESIGN_SYSTEM_README.md` for the test
template).
