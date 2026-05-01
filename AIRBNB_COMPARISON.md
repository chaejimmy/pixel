# PaceDream vs Airbnb — Side‑by‑Side Comparison & Improvement Report

**Date:** 2026‑05‑01
**Scope:** PaceDream Android app (this repo) compared against Airbnb's mobile app, focused on user‑facing functionality. Architectural items (DI, networking layer, etc.) are out of scope here — those are tracked in `FEATURE_PARITY_REPORT.md`.

PaceDream is a broader platform than Airbnb — it covers hourly spaces, gear rental, and split stays in addition to short‑term lodging — but the booking/marketplace UX is the part most directly comparable to Airbnb. The gaps below are the ones that move the needle for a guest‑side booking experience.

---

## 1. Side‑by‑side feature matrix

Legend: ✅ implemented · 🟡 partial · ❌ missing

| Area | Airbnb | PaceDream Android | Status |
|---|---|---|---|
| **Search — map view** | Map with price pins, draggable bounds, "Search this area" | List/Map toggle, GoogleMap with markers, gesture-gated "Search this area" pill | ✅ |
| **Search — filter sheet** | Type, price slider w/ histogram, dates, guests, rooms/beds, amenities, host language, instant book, accessibility | `FilterScreen.kt`: type, price range, dates, guests (adults/children/infants/pets), bedrooms/beds/baths, instant-book toggle, amenities (criteria emitted via `FilterCriteria`; wiring into SearchViewModel still pending) | 🟡 |
| **Search — categories** | Iconic horizontal category rail (Beachfront, Treehouse, …) that re‑queries | Category chips (Restroom, Nap Pod, Meeting Room, Storage, Parking) — fewer than the website's 9 | 🟡 |
| **Search — autocomplete / "where to"** | Location search with recents, nearby, "I'm flexible" | Single search bar `Where to?`; no recents/nearby | 🟡 |
| **Search — "search this area" pan** | Yes | Pill surfaces after gesture-driven camera move when bounds meaningfully differ | ✅ |
| **Listing — photo gallery** | Pager + grid, share/save in bar | `HorizontalPager` gallery, share, save | ✅ |
| **Listing — amenities** | Categorised, "Show all 28 amenities" sheet | Amenities list | ✅ |
| **Listing — reviews** | Per‑category bars (cleanliness, accuracy, …), search & filter reviews, host responses | Categories present (cleanliness, accuracy, comm, location, check‑in, value, comfort, convenience), host responses | ✅ |
| **Listing — calendar/availability** | Inline 2‑month calendar; min/max stay rules | DatePicker for availability | 🟡 |
| **Listing — location** | Approximate map with circle until booked | Location map | ✅ |
| **Listing — similar listings** | "Other things to consider" carousel | Carousel infrastructure exists | 🟡 |
| **Listing — translation** | "Translated automatically — show original" | None | ❌ |
| **Booking — price breakdown** | Nightly × n, cleaning, service, taxes, total in one sheet | Price breakdown shown | ✅ |
| **Booking — instant book vs request** | Both, badged on card | Card `BookingModeBadge` (Instant Book / Request to Book / Unavailable); listing-detail CTA toggles between "Reserve" and "Request to book" with charge-timing copy | ✅ |
| **Booking — house rules / cancellation** | Clear surface before pay, must‑agree checkbox | Cancellation policy displayed | 🟡 |
| **Booking — split payments / pay‑less‑upfront** | Yes | No | ❌ |
| **Booking — guest selector w/ infants/pets** | Adults / Children / Infants / Pets, with rules | Guest selector | 🟡 |
| **Trips — upcoming/past** | Tabs, with reservation code, message host CTA | Upcoming/past, cancel | ✅ |
| **Trips — check‑in card** | Address unlock at check‑in, Wi‑Fi, directions | Check‑in dates tracked, no day‑of card | 🟡 |
| **Trips — receipts** | PDF / share | Receipt flow exists | ✅ |
| **Host — listing creation wizard** | Long step‑through (12 steps) | `CreateListingScreen.kt` | ✅ |
| **Host — calendar w/ smart pricing** | Yes | Calendar mgmt exists; smart pricing not seen | 🟡 |
| **Host — earnings / payouts** | Dashboard + statements | Earnings infra present | 🟡 |
| **Host — co‑host / team** | Yes | No | ❌ |
| **Messaging — threads** | Yes, with quick replies, photo, location | Threads + image attachments | ✅ |
| **Messaging — read receipts** | Yes | Read state in models | 🟡 |
| **Messaging — push** | Yes | Infra referenced, limited | 🟡 |
| **Wishlist — multiple lists** | Multiple named lists, share, collaborate | Single wishlist | 🟡 |
| **Profile — verifications** | Government ID + selfie, email/phone, social | `IDVerificationScreen.kt`, payment methods | ✅ |
| **Profile — reviews about user** | "Reviews from hosts/guests" tabs | Not surfaced on profile | ❌ |
| **Reviews — write review** | Multi‑step (rating + categories + private + public) | `WriteReviewScreen.kt` with 8 categories | ✅ |
| **Notifications — push** | Yes | Limited | 🟡 |
| **Notifications — in‑app center** | Yes | `NotificationScreen.kt`, grouped by date | ✅ |
| **i18n — language switcher** | Many languages, in‑app switch | Not visible | ❌ |
| **i18n — currency switcher** | In‑app, sticky | USD only, hardcoded | ❌ |
| **Accessibility — TalkBack labels, large‑text** | Comprehensive | Min 44dp tap targets only; no `contentDescription` audit | ❌ |
| **Dark mode** | Yes | Light only at app theme level (a few category tints react to system) | ❌ |
| **Offline / poor‑network** | Cached recents, optimistic favorite | Optimistic favorite toggle queue | 🟡 |
| **Deep links** | Universal links into listings/trips | `DeepLinkHandler.kt` for Stripe + listings | ✅ |
| **Referrals / gift cards** | Yes | Blocked in moderation; not built | ❌ |
| **Trust & safety — AirCover‑style guarantee** | AirCover banner on every listing & trip | Not present | ❌ |

---

## 2. Top‑10 prioritised improvements

Ranked by impact‑to‑effort for a guest who is comparing PaceDream to Airbnb.

### P0 — book/discover gaps that hurt conversion

1. **Map view in search results.** Add a map/list toggle on `SearchScreen` with price pins and "Search this area" on pan. This is the single biggest UX gap vs Airbnb. Use Maps Compose; reuse the LatLng already in listing models.
2. **Full filter sheet.** Extend `FilterScreen.kt` with: dates, guests (adults/children/infants/pets), bedrooms/beds/baths, instant‑book toggle, host language, and an amenities multiselect grouped by category. Persist last‑used filters per tab (Use/Borrow/Split).
3. **Multi‑field search header (WHAT / WHERE / DATES / GUESTS).** The website already has it; the app collapses to "Where to?". Mirror the website tab + field layout (already flagged in `UI_UX_COMPARISON.md` §2, but unblocked once filters land).
4. **Instant‑book vs request‑to‑book.** Surface as a badge on cards and as a CTA distinction on listing detail; today they look identical.

### P1 — trust, retention, and parity polish

5. **Day‑of trip card.** On `BookingsScreen`, when check‑in is today, swap the row for a card with full address, Wi‑Fi password (already have a `wifi` feature module), directions deeplink, and "Message host". Pure UI change over existing data.
6. **Multiple wishlists.** Replace single wishlist with named lists (`Weekend in Tahoe`, `Meeting rooms`). The toggle endpoint is already optimistic; extending to lists is mostly model + a "save to…" sheet.
7. **Profile: reviews about the user.** Add a "Reviews" tab on `ProfileScreen`. Backs onto the same review pipeline that already powers `ReviewsScreen.kt`.
8. **Dark mode.** `core/designsystem` already has a `darkColorScheme` per‑tint pattern in `CategoryColors.kt`; promote that to the full theme so the app respects system dark. Low‑risk, high‑perceived‑quality.

### P2 — internationalisation and accessibility

9. **i18n + currency switcher.** Today USD is hard‑coded. Two changes: (a) `Locale`‑driven number/date formatting in price/date components, (b) currency dropdown stored in DataStore that re‑formats prices client‑side from a base currency on the API. Add a language list (ship with EN + 1–2 others to validate the layer).
10. **Accessibility audit.** Add `contentDescription` to all images/icon buttons, `Modifier.semantics { role = … }` on tappable cards, ensure Dynamic Type / `fontScale` doesn't clip listing‑card titles, and verify TalkBack focus order on `ListingDetailScreen`. None of this needs new features — it's a sweep.

---

## 3. Quick wins (< 1 day each)

- Add the missing category chips already on the website: **Study Room, Short Stay, Apartment, Luxury Room** (`UI_UX_COMPARISON.md` §3 — still open).
- Add a **share sheet** on listing detail using `ACTION_SEND` with a `pacedream.com` deep link — Airbnb users expect this.
- Add a **"Report listing"** entry in the listing overflow menu — required for app‑store trust & safety review anyway.
- Show **host response rate / response time** on the listing's host card (data exists from host profile MVP merged in #473).
- Surface **cancellation‑policy summary** as a chip on search cards ("Free cancellation"), not only inside the booking sheet.

---

## 4. Things PaceDream does that Airbnb doesn't

Worth keeping front and centre — these are differentiators that should *not* regress as we close Airbnb gaps:

- **Hourly bookings** (`/properties/bookings/timebased`) — Airbnb has no hour‑granular product.
- **Gear rental** alongside spaces — orthogonal SKU Airbnb doesn't carry.
- **Split stays** as a first‑class concept — Airbnb only shows split itineraries indirectly.
- **Online/virtual sessions** — the recent map‑replacement card on virtual listings (commit `895b561`) is a nice differentiator.

When designing the map view (P0 #1), make sure these non‑geographic SKUs degrade gracefully (e.g., gear rentals show pickup point pin; online sessions hide the map entirely as already done).

---

## 5. Suggested next step

If you want, the natural follow‑up is to take items **P0 #1 (map view)** and **P0 #2 (full filter sheet)** as the first two PRs — they unblock #3 and #4 and are the clearest "this app feels like Airbnb now" signal for users.
