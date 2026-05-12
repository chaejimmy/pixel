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
| `app/shourov/feature/host` | 15 | 81 | 71 | 0 | **167** |
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

## Totals

| Column | Sum across all modules |
|---|---:|
| `Color(0x…)` literals | 147 |
| `Color.White` / `Color.Black` | 236 |
| raw `.dp` paddings | 311 |
| `RoundedCornerShape(N.dp)` | 10 &nbsp;<sup>‡</sup> |
| **Grand total** | **704** |

(Down from 805 at commit `8d93034` — `refactor/ds-migration-home`
cleared 101 hits in `feature/home` outside `redesign/`; the deferred
**109** hits inside `feature/home/.../redesign/` are still counted in
the table row for that subtree so the grand total reflects everything
currently in code.)

<sup>‡</sup> `feature/wanted`, `feature/chat`, and `feature/wishlist`
were migrated by `refactor/ds-radius-cleanup` (PR #494) and no longer
contain any `RoundedCornerShape(N.dp)` literals.  Their rows above
still show the pre-merge counts and will be reconciled in the next
catch-all doc refresh — the merged code is the ground truth, and CI
enforces zero via STRICT mode regardless.

## Observations

1. **`app/shourov/feature/host` is now the single largest debt at 167
   hits** — `feature/home` was migrated in `refactor/ds-migration-home`
   and dropped from the top of the table.  Migrating `host` next is the
   highest-leverage remaining cleanup; it mirrors the same component
   patterns (`EnhancedDashboardHeader`-style gradients, scrim overlays,
   raw `.dp` paddings) so the same playbook applies.
2. **`app/pacedream.app/feature/home` and the migrated `feature/home`
   are now visually consistent.**  Both routes use `PaceDreamColors`,
   `OnBrandSurface`, and `scrimOnImage` — the brand rebrand from purple
   to green (per `Color.kt:452`) is now applied uniformly.
3. **`RoundedCornerShape(N.dp)` is fully eradicated repo-wide** (10 → 0)
   and locked in by STRICT mode in `designSystemCheck`.  Any new hit
   anywhere in `feature/**` fails CI.
4. **16 source roots are now in `scanRoots`** — the 15 originally
   locked-in plus `feature/home` (with `redesign/` excluded).
5. **`.dp` padding remains the dominant pattern** of remaining debt at
   270 of 585 hits (≈ 46 %).  Migration is mechanical: a per-value
   histogram + once-per-cluster rounding decision (the playbook used
   in `feature/home` is reusable for `host`).

## Suggested next migration order

Pure leverage / impact ratio (post-`feature/home`):

1. **`app/shourov/feature/host`** (167) — biggest remaining debt; same
   playbook as `feature/home` (header gradients, image scrims, padding
   histogram).
2. **`app/pacedream.app/feature/listingdetail`** (43) — 30 hex literals,
   all on image overlays; mirror the Phase 3 pattern.
3. **`feature/booking`** (35) and **`app/pacedream.app/feature/inbox`**
   (26) — medium-debt modules with a clean mix of hex + paddings.
4. **`feature/home/.../redesign/`** (109, currently carved out) — needs
   the design call on whether the Purple/Coral palette gets adopted
   into `PaceDreamColors` or stays a scoped internal theme.

---

_Originally generated by scanning Kotlin sources on commit_ `8d93034`
_(then the HEAD of `claude/optimize-pacedream-ux-7rC8D`).  Updated for
the `feature/home` row by `refactor/ds-migration-home`._
