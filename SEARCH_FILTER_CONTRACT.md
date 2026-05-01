# Android Search Filter Wire Contract

**Scope:** What the Android app emits to the backend search endpoints when the user applies filters in `FilterScreen`. This document is an authoritative *client-side* contract — backend support per key is **explicitly not verified** from this repository (the backend lives elsewhere) and is marked accordingly below.

**Source of truth:**
- Caller — `app/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchViewModel.kt` (`loadPage`)
- Wire builder — `SearchRepository.kt` (`buildPrimaryWireRequest`, `buildFallbackQueryParams`)
- Tests pinning the contract — `app/src/test/java/.../search/SearchFilterContractTest.kt`

If you change any serialization rule below, the test will fail. That is the point.

---

## Endpoints

The client tries one of two primary endpoints and falls back to a third:

| Endpoint | When | Path |
|---|---|---|
| **POC listings** | `webCategory` is `time-based` or `hourly-rental-gears` | `GET /v1/poc/listings` |
| **Generic listings** | every other category (`room-stays`, `find-roommates`, …) | `GET /v1/listings` |
| **Fallback** | primary returned `ApiResult.Failure` | `GET /v1/search` |

The fallback **forwards every Airbnb-parity filter** (verified by `fallback endpoint forwards every Airbnb-parity filter` test). A primary→fallback transition does not silently regress filter coverage.

---

## Baseline keys (always sent on primary)

| Key | Value | Notes |
|---|---|---|
| `status` | `published` | constant |
| `limit` | perPage as int string | default `24` |
| `skip_pagination` | `true` | only on `page0 == 0` |
| `skip` | `page0 * perPage` | only on `page0 > 0` (replaces `skip_pagination`) |
| `category` | `time_based` / `hourly_rental_gear` | only on POC endpoint |
| `shareType` | `USE` / `BORROW` / `SPLIT` | only on generic listings endpoint |

---

## Filter keys

Legend:
- ✅ **Wire-locked** — emitted exactly as documented; pinned by `SearchFilterContractTest`
- ❓ **Backend support unverified** — server may or may not honor; see "Backend support" below

| `FilterCriteria` field | Emitted as | Drop rule | Status | Backend |
|---|---|---|---|---|
| `checkInEpochDay`, `checkOutEpochDay` | `date=<startISO>,<endISO>` (or `date=<startISO>` when only start set) | both null → no key; end-only → no key | ✅ | ❓ |
| `adults + children` (`totalGuests`) | `guests=<int>` | 0 → no key | ✅ | ❓ |
| `infants` | **not sent** | always | ✅ | n/a — no documented key |
| `pets` | **not sent** | always | ✅ | n/a — no documented key |
| `propertyType` | `propertyType=<lowercase>` | blank/null → no key | ✅ | ❓ |
| `minPrice` | `minPrice=<int>` | ≤ 0 / null → no key | ✅ | ❓ |
| `maxPrice` | `maxPrice=<int>` | ≤ 0 / null → no key | ✅ | ❓ |
| `bedrooms` | `bedrooms=<int>` | ≤ 0 / null → no key | ✅ | ❓ |
| `beds` | `beds=<int>` | ≤ 0 / null → no key | ✅ | ❓ |
| `bathrooms` | `bathrooms=<int>` | ≤ 0 / null → no key | ✅ | ❓ |
| `instantBookOnly` | `instantBook=true` | `false` → no key (treated as "no constraint") | ✅ | ❓ |
| `amenities` | `amenities=<csv>` (lowercase, space→`_`) | empty set → no key | ✅ | ❓ |

### Bounding box (map "search this area")

`swLat`, `swLng`, `neLat`, `neLng` are sent as plain `Double.toString()` — but **only when all four are non-null**. Partial values are dropped so a half-formed geospatial query never reaches the backend. This is asserted by `bounding box is sent only when all four corners present`.

---

## Empty `FilterCriteria` ≡ pre-filter request

When the user has applied no filters, the wire request is byte-identical to the pre-FilterScreen behaviour. The contract tests pin this:

```
default FilterCriteria emits no Airbnb-parity filter keys on primary
default FilterCriteria emits no Airbnb-parity filter keys on fallback
```

Every structured filter key listed above is dropped. This means existing dashboards / metrics that bucket by query shape are unaffected for users who never open FilterScreen.

---

## Backend support

**The Android repo cannot determine whether the server honors any of the `❓` rows above.** The backend handler for `/v1/poc/listings`, `/v1/listings`, and `/v1/search` lives in a separate repository that is not vendored here. From the client's perspective:

- A server that recognises a key applies it as a filter — results shrink correctly.
- A server that ignores an unknown key returns the same set as if the key were absent — results do **not** shrink.

The Android UI cannot tell these apart. Each filter param is marked `NEEDS_BACKEND_SUPPORT` in `SearchRepository.buildPrimaryWireRequest` / `buildFallbackQueryParams` so future readers don't assume backend coverage.

### To verify backend support

Outside this repo. Two options:

1. Read the backend search handler and confirm each key is parsed and applied to its Mongo query.
2. Black-box probe — issue requests with one filter at a time against a test dataset and diff the response sets.

Until one of those happens, treat the `❓` rows as "**Android sends it; server may or may not act on it**".

---

## Debug logging

`SearchViewModel.applyFilters()` emits one Timber.d line per apply, formatted by `FilterCriteria.toDebugSummary()` (in `SearchFiltersStore.kt`). Example output:

```
applyFilters: FilterCriteria(guests=3 propertyType=Apartment min=30 max=400 bedrooms=2 beds=2 baths=1 instantBook=true amenities=WiFi,Pool)
```

This logging is **debug-only**:
- Timber's `DebugTree` is planted in `PaceDreamApplication` only when `BuildConfig.DEBUG` is true.
- R8 release config (commit `5aaab45`) strips `android.util.Log` calls from release builds as a second line of defence.

QA can correlate an unexpected result set with the exact filter shape that produced it without touching production logs.

---

## How to evolve the contract safely

1. Add the field to `FilterCriteria` (in `feature/home/presentation`).
2. Plumb it through `SearchViewModel.loadPage` into the new `repo.search()` argument.
3. Add the matching key to **both** `buildPrimaryWireRequest` and `buildFallbackQueryParams` in `SearchRepository.kt`. Decide the drop rule (null/empty/zero) and follow the existing pattern.
4. Add a `NEEDS_BACKEND_SUPPORT` comment unless backend support is confirmed.
5. Add a row to the table above and a test in `SearchFilterContractTest`.
6. Update `SearchUiState.activeFilterCount` if the field warrants its own bucket in the badge.

The wire-contract tests and this doc are the single point of truth for what Android sends — if they disagree, the test wins.
