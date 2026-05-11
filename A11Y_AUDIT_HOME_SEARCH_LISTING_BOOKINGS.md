# Accessibility audit — Home → Search → Listing Detail → Bookings

**Method.** Static walk of every interactive node (`IconButton`,
`Surface(onClick = …)`, `Modifier.clickable`, `Modifier.semantics`) and
its associated `contentDescription` / `Text` label in the four named
Compose screens. Per-finding sections list **what TalkBack will
announce today** (inferred from source) and **propose a label fix**
inline with a unified-diff style patch.

> This is a source review, not a runtime trace. Some announcements
> depend on TalkBack's "merge descendants" heuristics and on dynamic
> state at compose time, so a few of the inferences below would shift
> slightly on a real device. The accompanying
> `SemanticsTraceTest.kt` test (see commit message) renders each
> screen on an emulator and dumps the actual semantics tree —
> recommended verification before merging any of these fixes.

Findings are tagged:

- 🟥 **MISSING** — interactive element has no usable label; TalkBack
  reads nothing or just the element kind ("button", "image").
- 🟧 **AMBIGUOUS** — label exists but is too generic, double-reads with
  an adjacent label, or omits state.
- 🟨 **RAW_ID** — label leaks a developer string (route name, enum,
  type code) that the user can't act on.

---

## HomeScreen (`app/.../pacedream/app/feature/home/HomeScreen.kt`)

### H-1 🟧 AMBIGUOUS — Notification button merges into hero pill

- **File / line:** `HomeScreen.kt:347-364`
- **Current source:**

  ```kotlin
  Surface(
      modifier = Modifier
          .size(44.dp)
          .testTag(HomeTestTags.NotificationButton)
          .semantics { role = Role.Button }
          .clickable(onClick = onNotificationClick),
      …
  ) {
      Box(…) {
          Icon(
              imageVector = PaceDreamIcons.Notifications,
              contentDescription = "Notifications",
              …
          )
      }
  }
  ```

- **What TalkBack reads today:** `"Notifications, button"`.
- **Why it's flagged:** The label is a noun, not an action — TalkBack
  users hear "Notifications, button" and don't know what tapping does
  (does it filter notifications? mark them read? open the centre?).
  Action-style labels are recommended for buttons.
- **Proposed fix:**

  ```diff
  -            contentDescription = "Notifications",
  +            contentDescription = "Open notifications",
  ```

---

### H-2 🟧 AMBIGUOUS — Search-bar card double-reads "Search"

- **File / line:** `HomeScreen.kt:420-499`
- **Current source:**

  ```kotlin
  Surface(
      modifier = Modifier
          …
          .semantics { role = Role.Button }
          .clickable(onClick = onSearchClick),
      …
  ) {
      Row(…) {
          Box(…) {
              Icon(
                  imageVector = PaceDreamIcons.Search,
                  contentDescription = "Search",
                  …
              )
          }
          Spacer(…)
          Column(modifier = Modifier.weight(1f)) {
              Text(text = "Search anywhere", …)
              Spacer(…)
              Text(text = "Spaces · Items · Services", …)
          }
          Spacer(…)
          Surface(
              modifier = Modifier
                  …
                  .semantics { role = Role.Button }
                  .clickable(onClick = onFilterClick),
              …
          ) {
              Icon(
                  imageVector = PaceDreamIcons.Tune,
                  contentDescription = "Filters",
                  …
              )
          }
      }
  }
  ```

- **What TalkBack reads today:** Three nodes — `"Search, Search
  anywhere, Spaces · Items · Services, button"` (the entire card walks
  as one button with merged Text children, plus the search Icon
  contributes its own description).
- **Why it's flagged:**
  1. The leading `Icon(contentDescription = "Search")` is decorative
     here (the Text already says "Search anywhere") — it should be
     `null` so TalkBack doesn't read "Search" twice.
  2. The subcopy text `"Spaces · Items · Services"` is **stale** — the
     `SearchTab` labels were renamed to `Use / Borrow / Split` in
     `6d444c9` (Phase 5). Both the visible copy and the TalkBack
     announcement still say the old names.
- **Proposed fix:**

  ```diff
  @@ Row leading icon
  -            Icon(
  -                imageVector = PaceDreamIcons.Search,
  -                contentDescription = "Search",
  -                tint = PaceDreamColors.Primary,
  -                modifier = Modifier.size(20.dp)
  -            )
  +            Icon(
  +                imageVector = PaceDreamIcons.Search,
  +                contentDescription = null, // decorative — Text below labels the card
  +                tint = PaceDreamColors.Primary,
  +                modifier = Modifier.size(20.dp)
  +            )
  @@ subcopy
  -                text = "Spaces · Items · Services",
  +                text = "Use · Borrow · Split",
  ```

---

### H-3 🟧 AMBIGUOUS — Category-pill icon double-reads with adjacent Text

- **File / line:** `HomeScreen.kt:571-602` (function `CategoryFilterChip`)
- **Current source:**

  ```kotlin
  Column(
      …
      modifier = Modifier
          .semantics { role = Role.Button }
          .clickable(…)
          …,
  ) {
      Icon(
          imageVector = icon,
          contentDescription = name,   // ← e.g. "Restroom"
          …
      )
      Spacer(…)
      Text(text = name, …)             // ← same "Restroom"
  }
  ```

- **What TalkBack reads today:** `"Restroom, Restroom, button"` —
  the icon and the text both announce the same word.
- **Why it's flagged:** Decorative icons paired with readable adjacent
  Text should have `contentDescription = null`. The bug pattern was
  called out repeatedly in the 2026-04-19 sweep
  (`DESIGN_QA_REPORT_ANDROID.md`); this site was missed.
- **Proposed fix:**

  ```diff
  @@ CategoryFilterChip
  -        Icon(
  -            imageVector = icon,
  -            contentDescription = name,
  +        Icon(
  +            imageVector = icon,
  +            contentDescription = null, // adjacent Text already labels the pill
              …
          )
  ```

---

### H-4 🟧 AMBIGUOUS — Category-card icon double-reads (same bug at second site)

- **File / line:** `HomeScreen.kt:687-703` (function `CategoryCardChip`)
- **Why it's flagged:** Same pattern as H-3 — `Icon(contentDescription
  = category.name)` paired with `Text(text = category.name)` inside a
  `role = Role.Button` Surface. Double-read.
- **Proposed fix:** Identical to H-3 — set the Icon
  `contentDescription = null`.

---

### H-5 🟧 AMBIGUOUS — Listing-card AsyncImage uses title as its description

- **File / line:** `HomeScreen.kt:968-976` (and identical pattern at
  `:1366`, `:1566`, `:1880-1900` for the Featured / Items / Services /
  Destinations sections)
- **Current source:**

  ```kotlin
  Surface(
      modifier = Modifier
          …
          .semantics { role = Role.Button }
          .clickable(onClick = onClick),
      …
  ) {
      Column {
          Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f) …) {
              AsyncImage(
                  …
                  contentDescription = item.title,
                  contentScale = ContentScale.Crop,
              )
              // type badge, favourite heart, etc.
          }
          // visible Text(text = item.title) appears below the image
      }
  }
  ```

- **What TalkBack reads today:** The full card walks as a button.
  Inside, the AsyncImage announces the title, then the visible Text
  below also says the title — and the user has to focus through both.
- **Why it's flagged:** Per Material a11y guidance, an image whose
  caption is provided by adjacent text should be `null`-described.
  The current pattern produces a double-read on every card.
- **Proposed fix:**

  ```diff
  -                contentDescription = item.title,
  +                contentDescription = null, // adjacent Text caption labels the card
  ```

  *Applies to all four listing-card variants on Home that use this
  pattern: lines 968, 1366, 1566, 1880-ish in `HomeScreen.kt`.*

---

### H-6 ✅ already correct — `FavoriteIconButton`

- **File / line:** `HomeScreen.kt:1019`, `:1444`, `:1644` (call sites)
- The component is in `core/designsystem/FavoriteIconButton.kt` and
  already announces `"Add to favorites"` / `"Remove from favorites"`
  based on the `isFavorite` flag. No change needed.

---

### H-7 🟨 RAW_ID — Type badge announces raw type code

- **File / line:** `HomeScreen.kt:994-1016` (and the analogous badges
  in other sections)
- **Current source:**

  ```kotlin
  Surface(…) {
      Text(
          text = when (item.type) {
              "time-based" -> "Space"
              "gear"       -> "Item"
              "split-stay" -> "Service"
              else         -> item.type.replaceFirstChar { it.uppercase() }
          },
          …
      )
  }
  ```

- **Why it's flagged:** Most rows hit the `when` cases and announce
  human strings (`"Space"`, `"Item"`, `"Service"`). The `else` branch
  raw-uppercases whatever the backend sent (`time_based` → `Time_based`,
  `hourly_rental` → `Hourly_rental`). If the backend introduces a new
  type, TalkBack will read the wire format.
- **Proposed fix:**

  ```diff
  -                else         -> item.type.replaceFirstChar { it.uppercase() }
  +                else         -> "Listing" // catch-all so TalkBack never leaks a wire type
  ```

  Optionally, log a Timber warning so we notice new backend types.

---

## SearchScreen (`app/.../pacedream/app/feature/search/SearchScreen.kt`)

### S-1 🟧 AMBIGUOUS — "Clear" without object

- **File / line:** `SearchScreen.kt:173-181`
- **Current source:**

  ```kotlin
  IconButton(onClick = { viewModel.onQueryChanged("") }) {
      Icon(
          PaceDreamIcons.Close,
          contentDescription = "Clear",
          …
      )
  }
  ```

- **What TalkBack reads today:** `"Clear, button"`. The user has no
  way to know what's being cleared if they navigated to the icon
  directly.
- **Proposed fix:**

  ```diff
  -            contentDescription = "Clear",
  +            contentDescription = "Clear search query",
  ```

---

### S-2 🟥 MISSING — Search tabs lack `role = Tab` and selection state

- **File / line:** `SearchScreen.kt:220-249`
- **Current source:**

  ```kotlin
  tabs.forEach { tab ->
      val isSelected = uiState.selectedTab == tab
      Surface(
          modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(PaceDreamRadius.SM))
              .clickable { viewModel.onTabChanged(tab) },
          color = if (isSelected) PaceDreamColors.Primary else Color.Transparent,
          …
      ) {
          Box(…) {
              Text(tab.label, …)
          }
      }
  }
  ```

- **What TalkBack reads today:** Just `"Use"` (or `"Borrow"`, etc.) —
  no role, no selection state. A user can't tell that the three
  surfaces form a related group, nor which one is active.
- **Proposed fix:**

  ```diff
  @@ tabs.forEach
       Surface(
           modifier = Modifier
               .weight(1f)
               .clip(RoundedCornerShape(PaceDreamRadius.SM))
  +            .semantics {
  +                role = Role.Tab
  +                selected = isSelected
  +                stateDescription = if (isSelected) "Selected" else "Not selected"
  +            }
               .clickable { viewModel.onTabChanged(tab) },
           …
       )
  ```

  Required imports:
  ```kotlin
  import androidx.compose.ui.semantics.Role
  import androidx.compose.ui.semantics.role
  import androidx.compose.ui.semantics.selected
  import androidx.compose.ui.semantics.semantics
  import androidx.compose.ui.semantics.stateDescription
  ```

---

### S-3 🟧 AMBIGUOUS — Search-result-row AsyncImage same as H-5

- **File / line:** `SearchScreen.kt:411-420`
- Same pattern as H-5 on Home: `AsyncImage(contentDescription =
  item.title)` paired with a visible title Text underneath.
- **Proposed fix:** Same as H-5 — set `contentDescription = null`.

---

## ListingDetailScreen (`app/.../pacedream/app/feature/listingdetail/ListingDetailScreen.kt`)

### L-1 🟧 AMBIGUOUS — Hero gallery AsyncImage uses page title

- **File / line:** `ListingDetailScreen.kt:1525-1534`
- **Current source:**

  ```kotlin
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
          .data(url)
          …
          .build(),
      contentDescription = title,   // ← listing title
      …
  )
  ```

- **What TalkBack reads today:** Every image in the carousel
  announces the same listing title. The user can't tell that they're
  on image 2 of 5; nor what image 3 depicts.
- **Proposed fix:**

  ```diff
  -                    contentDescription = title,
  +                    contentDescription = "Photo ${index + 1} of $totalImages",
  ```

  Pass `index` and `totalImages` from the pager state — the
  surrounding code already constructs `pagerState` and `imageUrls`.
  Falls back to `"Listing photo"` if `totalImages` is zero (defensive).

---

### L-2 ✅ already correct — Back / Share / Favorite

- `IconButton(onBackClick)` ⇒ `"Back"` ✓
- `IconButton(onShare)` ⇒ `"Share"` ✓
- `IconButton(onToggleFavorite)` ⇒ dynamic `"Add to favorites"` /
  `"Remove from favorites"` ✓ (fixed in `d2da4dd`, Phase 4).

---

### L-3 🟧 AMBIGUOUS — Superhost / Verified icons next to host avatar

- **File / line:** `ListingDetailScreen.kt:1880-1908`
- **Current source:**

  ```kotlin
  Icon(
      imageVector = PaceDreamIcons.Star,
      contentDescription = "Superhost",
      …
  )
  …
  Icon(
      imageVector = PaceDreamIcons.Verified,
      contentDescription = "Verified",
      …
  )
  ```

- **What TalkBack reads today:** `"Superhost"` and `"Verified"` —
  these are nouns without context. A user hearing "Verified" in the
  middle of a host card needs to scroll back to find which item is
  verified.
- **Proposed fix:**

  ```diff
  -            contentDescription = "Superhost",
  +            contentDescription = "Superhost badge",
  …
  -            contentDescription = "Verified",
  +            contentDescription = "Identity verified",
  ```

  Or — since these icons sit immediately next to the host Text — set
  both to `null` and append `" · Superhost"` / `" · Verified"` to
  the host-name Text. Cleaner TalkBack reading.

---

### L-4 ✅ already correct — Edit / Delete review

- `IconButton(contentDescription = "Edit review")` ✓
- `IconButton(contentDescription = "Delete review")` ✓

---

### L-5 ✅ already correct — Star-rating IconButtons

- **File / line:** `ListingDetailScreen.kt:2626`, `:2751`
- `contentDescription = "Rate $star star${if (star > 1) "s" else ""}"`
  — singular/plural handled correctly. Fixed in the 2026-04-19 sweep.

---

### L-6 🟨 RAW_ID — ReserveButton announces just `"Reserve"` regardless of state

- **File / line:** `ListingDetailScreen.kt:1471-1493`
- **Current source:**

  ```kotlin
  val ctaLabel = when {
      !canReserve     -> "Loading…"
      !isAvailable    -> "Unavailable"
      instantBook == false -> "Request to book"
      else            -> "Reserve"
  }
  …
  Button(
      onClick = onReserveClick,
      enabled = isAvailable && canReserve,
      modifier = Modifier.testTag(ListingDetailTestTags.ReserveButton),
      …
  ) {
      Text(ctaLabel, …)
  }
  ```

- **What TalkBack reads today:** Whichever label the `when` resolves
  to — `"Reserve, button"`, `"Unavailable, button"`, etc. Decent
  baseline. **But** the price the user is about to be charged is
  not announced: TalkBack just says "Reserve" without saying "at
  $X / night". For a primary commit-purchase CTA this is risky.
- **Proposed fix:** Add a `semantics { contentDescription = "$ctaLabel
  for $pricingLabel" }` to the Button when the label is "Reserve" /
  "Request to book":

  ```diff
   Button(
       onClick = onReserveClick,
       enabled = isAvailable && canReserve,
  -    modifier = Modifier.testTag(ListingDetailTestTags.ReserveButton),
  +    modifier = Modifier
  +        .testTag(ListingDetailTestTags.ReserveButton)
  +        .semantics {
  +            // Read both the action and the price so users can verify before
  +            // committing to an authorisation hold.
  +            if (pricingLabel != null && (ctaLabel == "Reserve" || ctaLabel == "Request to book")) {
  +                contentDescription = "$ctaLabel · $pricingLabel"
  +            }
  +        },
       …
   )
  ```

---

## BookingsScreen (`app/.../pacedream/app/feature/bookings/BookingsScreen.kt`)

### B-1 🟥 MISSING — Tab pills lack `role = Tab` and selection state

- **File / line:** `BookingsScreen.kt:295-308`
- Same pattern as S-2: clickable Row with `Text(tab.label)` inside,
  selection conveyed only by `containerColor` change.
- **Proposed fix:** Add the same `semantics { role = Role.Tab;
  selected = isSelected; stateDescription = … }` block as S-2.

---

### B-2 🟧 AMBIGUOUS — Booking-card AsyncImage uses title

- **File / line:** `BookingsScreen.kt:383-388`
- Same H-5 / S-3 double-read pattern.
- **Proposed fix:** Set `contentDescription = null` since the Text
  caption below the image already announces the title.

---

### B-3 🟧 AMBIGUOUS — Status badge announces just colour/text without context

- **File / line:** `BookingsScreen.kt:540-560` area (status pill on
  each card)
- **What TalkBack reads today:** Whatever the status string is —
  `"Confirmed"`, `"Cancelled"`, `"Pending"` — without explicitly
  saying it's a *booking status*.
- **Why it's flagged:** When focused in isolation (e.g. via "next
  control"), the word `"Pending"` is ambiguous: pending what?
- **Proposed fix:** Add `stateDescription = "Booking status"` to the
  status Surface, or include the prefix in the Text itself —
  `"Status: Pending"`.

---

### B-4 🟧 AMBIGUOUS — Empty-state icon has `contentDescription = null`

- **File / line:** `BookingsScreen.kt:399-401`
- **Current source:**

  ```kotlin
  Icon(
      imageVector = PaceDreamIcons.Image,
      contentDescription = null,
      …
  )
  ```

- This is fine *when the icon sits inside an empty-state composable
  with a visible Text* (which it does — line 396+ has a Text
  describing the empty state). No fix needed; flagged here so a
  future reviewer doesn't accidentally "fix" it.

---

## Summary table

| ID | Screen | Severity | One-line fix |
|---|---|---|---|
| H-1 | Home | 🟧 AMBIGUOUS | `"Notifications"` → `"Open notifications"` |
| H-2 | Home | 🟧 AMBIGUOUS | drop decorative Icon label + update stale subcopy `"Spaces · Items · Services"` → `"Use · Borrow · Split"` |
| H-3 | Home | 🟧 AMBIGUOUS | `Icon(contentDescription = name)` on category pill → `null` |
| H-4 | Home | 🟧 AMBIGUOUS | same as H-3 on category card |
| H-5 | Home | 🟧 AMBIGUOUS | `AsyncImage(contentDescription = item.title)` on listing card → `null` (×4 sites) |
| H-6 | Home | ✅ | already dynamic |
| H-7 | Home | 🟨 RAW_ID | type-badge `else` branch leaks wire-format string |
| S-1 | Search | 🟧 AMBIGUOUS | `"Clear"` → `"Clear search query"` |
| S-2 | Search | 🟥 MISSING | tab pills need `role = Tab` + selection state |
| S-3 | Search | 🟧 AMBIGUOUS | result-row image same as H-5 |
| L-1 | Listing | 🟧 AMBIGUOUS | hero gallery image announces page title for every photo |
| L-2 | Listing | ✅ | already labelled |
| L-3 | Listing | 🟧 AMBIGUOUS | host badges need context |
| L-4 | Listing | ✅ | already labelled |
| L-5 | Listing | ✅ | already pluralised |
| L-6 | Listing | 🟨 RAW_ID | Reserve button should include price in the announcement |
| B-1 | Bookings | 🟥 MISSING | tab pills need `role = Tab` + selection state |
| B-2 | Bookings | 🟧 AMBIGUOUS | card image same as H-5 |
| B-3 | Bookings | 🟧 AMBIGUOUS | status pill missing `"Booking status"` context |
| B-4 | Bookings | ✅ | correctly `null` (decorative beside Text) |

**Tally:** 14 fixes proposed (4 missing + 7 ambiguous + 3 raw-id /
state) across 18 findings.

---

## Verification

The accompanying `SemanticsTraceTest.kt` (under
`app/src/androidTest/`) renders each of the four screens on an
emulator, calls `onRoot().printToLog("SEMANTICS")`, and writes the
full semantics tree to logcat. Running the four tests on a real
Pixel 6 API 34 emulator and grepping the logcat output for each of
the flagged labels above is the recommended verification step
before merging any of the fixes — proves the fix actually shows up
in the announced semantics tree, not just in source.

To run:

```sh
./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=\
com.pacedream.app.a11y.SemanticsTraceTest
```

Then in another terminal:

```sh
adb logcat -s SEMANTICS:V
```

Each test prints `===== <screen name> =====` before dumping. Grep
that to isolate each screen's tree.
