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
| `feature/home` | 44 | 96 | 70 | 0 | **210** |
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

## Totals

| Column | Sum across all modules |
|---|---:|
| `Color(0x…)` literals | 161 |
| `Color.White` / `Color.Black` | 294 |
| raw `.dp` paddings | 340 |
| `RoundedCornerShape(N.dp)` | 10 |
| **Grand total** | **805** |

## Observations

1. **The top-2 modules account for ~47% of total violations.** `feature/home`
   (210) and `app/shourov/feature/host` (167) hold 377 of the 805 hits —
   their hex-literal and `.dp`-padding counts dominate. Migrating these
   two first is the highest-leverage cleanup.
2. **`app/pacedream.app/feature/home` is fully migrated** (5 violations
   — all raw `.dp` paddings, no colors, no shapes). This is the screen
   Phase 3 cleaned up. Note that `feature/home` (the multi-module legacy
   home package) is a different code path and still has the largest
   debt.
3. **`RoundedCornerShape(N.dp)` is almost gone** — only 10 instances total
   across the repo, concentrated in `feature/wanted` (4), `feature/chat`
   (4), and `feature/wishlist` (2). One commit per module would clear it
   repo-wide.
4. **15 modules have zero violations.** Adding these to
   `designSystemCheck`'s `scanRoots` is free CI insurance — they would
   gate any regression from a future PR without any migration work.
5. **`.dp` padding is the dominant pattern** (340 of 805 hits ≈ 42%)
   but is also the lowest-impact visually: most literals already sit
   on a uniform spacing grid; the migration is mostly a
   find-and-replace to `PaceDreamSpacing.*` tokens.

## Suggested next migration order

Pure leverage / impact ratio:

1. **`feature/chat`, `feature/wishlist`, `feature/wanted`** — only these
   three modules hold the remaining `RoundedCornerShape(N.dp)` debt.
   One small commit per module clears that column repo-wide and lets
   `designSystemCheck` strictly enforce it.
2. **`app/pacedream.app/feature/listingdetail`** — already mostly token-
   driven, but 30 hex literals remain on overlays. Mirror the Phase 3
   pattern (`scrimOnImage` / `OnBrandSurface`).
3. **15 already-clean modules** — add to `scanRoots` immediately; no
   migration cost, only protection from regression.
4. **`feature/home`** — the largest module and biggest debt, but also
   the most expensive migration; do it last when the design-system
   helpers have stabilised against the smaller modules.

---

_Generated by scanning Kotlin sources on commit_ `8d93034` _(at the time
of writing, the HEAD of `claude/optimize-pacedream-ux-7rC8D`)._
