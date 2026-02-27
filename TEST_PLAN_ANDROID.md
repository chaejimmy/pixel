# PaceDream Android — Test Plan

**Date:** 2026-02-25
**Version:** 0.1.2 (versionCode 8)

---

## Device Matrix

| Device | OS | Category |
|--------|----|----------|
| Pixel 7 | Android 14 (API 34) | Reference / Target SDK |
| Pixel 5 | Android 13 (API 33) | POST_NOTIFICATIONS gate |
| Samsung Galaxy S23 | Android 14 | OEM skin (One UI) |
| Samsung Galaxy A14 | Android 12 (API 31) | Low-end / budget |
| Pixel 4a | Android 12 | Mid-range baseline |
| OnePlus 9 | Android 13 | OEM skin (OxygenOS) |
| Emulator API 24 | Android 7.0 | Min SDK boundary |
| Emulator API 34 | Android 14 | CI pipeline |

---

## Test Cases

### 1. Authentication (AUTH)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| AUTH-01 | Auth0 Google login | Tap "Continue with Google" → select account → wait | Token exchanged, profile loaded, home visible | P0 |
| AUTH-02 | Auth0 login cancel | Tap Google → press back in browser | Returns to sign-in screen, no crash | P0 |
| AUTH-03 | Email/password login | Enter valid email + password → tap Login | Auth state = Authenticated, navigate to home | P0 |
| AUTH-04 | Email login – wrong password | Enter valid email + wrong password | Error message shown, stays on login | P0 |
| AUTH-05 | Email registration | Fill first/last name, email, password → tap Register | Account created, token stored, home visible | P0 |
| AUTH-06 | Duplicate email registration | Register with existing email | Server error message displayed | P1 |
| AUTH-07 | Session persistence – kill + relaunch | Login → force-stop → relaunch | Profile loads from cache, then refreshes | P0 |
| AUTH-08 | Token refresh on 401 | Wait for token expiry → make API call | Auto-refresh succeeds, original request retries | P0 |
| AUTH-09 | Token refresh failure → logout | Invalidate refresh token → make API call | User signed out, sign-in screen shown | P0 |
| AUTH-10 | Forgot password | Tap "Forgot Password" → enter email → submit | Success toast, check email for reset link | P1 |
| AUTH-11 | Logout | Tap Sign Out in settings | Tokens cleared, navigate to sign-in | P0 |
| AUTH-12 | Corrupt encrypted prefs recovery | Clear app data → relaunch | EncryptedSharedPreferences recreated cleanly | P1 |

### 2. Home / Browse (HOME)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| HOME-01 | Home feed loads | Launch app authenticated | Sections: Hourly Spaces, Rent Gear, Split Stays visible | P0 |
| HOME-02 | Pull to refresh | Pull down on home feed | Spinner shown, sections reload | P0 |
| HOME-03 | Category tabs switch | Tap "Restroom" → "EV Parking" tabs | Content updates for each category | P1 |
| HOME-04 | View All → section list | Tap "View All" on any section | Full list screen with cards | P1 |
| HOME-05 | Property card tap | Tap a property card | Navigate to listing detail screen | P0 |
| HOME-06 | Shimmer loading state | Slow network / airplane toggle | Shimmer placeholders visible during load | P1 |
| HOME-07 | Error state with retry | Kill network → pull refresh | Error banner shown with retry option | P1 |
| HOME-08 | Empty section handling | Backend returns empty section | Section hidden or shows empty state | P1 |

### 3. Search (SEARCH)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| SEARCH-01 | Search bar opens | Tap search bar on home | Search screen opens with input focused | P0 |
| SEARCH-02 | Keyword search | Type "New York" → search | Results filtered by location | P0 |
| SEARCH-03 | Category filter | Select "Meeting Room" category | Results filtered by category | P1 |
| SEARCH-04 | No results | Search "zzzznonexistent" | Empty state with message | P1 |
| SEARCH-05 | Search result → detail | Tap a search result card | Navigate to listing detail | P0 |

### 4. Listing Detail (DETAIL)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| DETAIL-01 | Detail loads | Tap property card from any screen | Title, images, price, description, map visible | P0 |
| DETAIL-02 | Image gallery scroll | Swipe through images | Pager indicator updates, images load | P1 |
| DETAIL-03 | Map preview | Scroll to map section | Google Maps renders with pin at property location | P1 |
| DETAIL-04 | Book Now → checkout | Tap "Book Now" button | Navigate to booking form / checkout | P0 |
| DETAIL-05 | Add to wishlist | Tap heart icon | Heart fills, toast confirms | P0 |
| DETAIL-06 | Remove from wishlist | Tap filled heart icon | Heart empties, optimistic removal | P1 |
| DETAIL-07 | Share listing | Tap share button | Share sheet with deep link | P1 |
| DETAIL-08 | Reviews section | Scroll to reviews | Reviews displayed with ratings | P1 |

### 5. Booking / Checkout (BOOK)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| BOOK-01 | Booking form loads | Tap Book Now on listing | Form with dates, times, price summary | P0 |
| BOOK-02 | Date selection | Select check-in and check-out dates | Dates update, price recalculates | P0 |
| BOOK-03 | Time selection | Select start and end times | Hours calculated, total price updated | P0 |
| BOOK-04 | Price calculation | Set 3 hours at $10/hr | Total = $30 + $3 service fee = $33 | P0 |
| BOOK-05 | Past date rejection | Select date in the past | Validation error shown | P1 |
| BOOK-06 | Submit booking | Fill form → tap Book | Booking created, confirmation shown | P0 |
| BOOK-07 | Stripe checkout redirect | Tap Pay → Stripe checkout | WebView/CustomTab opens Stripe | P0 |
| BOOK-08 | Booking success deep link | Complete Stripe → redirect | App receives /booking-success, confirmation shown | P0 |
| BOOK-09 | Booking cancelled deep link | Cancel Stripe → redirect | App receives /booking-cancelled, returns to booking | P1 |
| BOOK-10 | View bookings list | Navigate to Bookings tab | List of bookings with status chips | P0 |
| BOOK-11 | Cancel booking | Tap Cancel on pending booking | Booking status changes to cancelled | P1 |

### 6. Payments (PAY)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| PAY-01 | Payment methods screen | Settings → Payment Methods | List of saved cards (or empty state) | P1 |
| PAY-02 | Stripe initialization | Verify Stripe SDK loads | No crash on app start, key loaded from BuildConfig | P0 |
| PAY-03 | Checkout with Stripe | Complete booking flow to payment | Stripe payment sheet or redirect works | P0 |

### 7. Push Notifications (PUSH)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| PUSH-01 | FCM token registration | Login → check server logs | Token registered via /notifications/register-device | P0 |
| PUSH-02 | Message notification | Send FCM "message" type | Notification appears in Messages channel | P0 |
| PUSH-03 | Booking notification | Send FCM "booking" type | Notification appears in Bookings channel | P0 |
| PUSH-04 | Notification tap → chat | Tap message notification | App opens to specific chat thread | P1 |
| PUSH-05 | Notification tap → booking | Tap booking notification | App opens to booking detail | P1 |
| PUSH-06 | Android 13 permission prompt | Fresh install on API 33+ | Runtime permission dialog shown | P0 |
| PUSH-07 | Permission denied | Deny POST_NOTIFICATIONS | No crash; notifications silently dropped | P0 |
| PUSH-08 | Token refresh | Force FCM token refresh | New token sent to server | P1 |

### 8. Wishlist (WISH)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| WISH-01 | View wishlist | Navigate to Wishlist tab | List of saved items (or empty/auth state) | P0 |
| WISH-02 | Remove from wishlist | Swipe or tap remove | Optimistic removal, restored on API failure | P1 |
| WISH-03 | Filter wishlist | Tap filter chips (All, Hourly, Gear) | List filters accordingly | P1 |
| WISH-04 | Wishlist requires auth | Unauthenticated → Wishlist tab | Auth prompt shown | P0 |

### 9. Inbox / Chat (INBOX)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| INBOX-01 | Inbox loads threads | Navigate to Inbox tab | Thread list with last message preview | P0 |
| INBOX-02 | Guest/Host mode toggle | Tap Guest/Host chips | Threads filter by mode, unread badges update | P1 |
| INBOX-03 | Open thread | Tap a thread | Message history loads, input bar visible | P0 |
| INBOX-04 | Send message | Type message → tap send | Message appears in bubble, sent to server | P0 |
| INBOX-05 | Pull to refresh | Pull down on thread list | Threads reload | P1 |
| INBOX-06 | Unread badge count | Receive message when not in inbox | Badge count increments on bottom nav | P1 |

### 10. Host Mode (HOST)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| HOST-01 | Switch to host mode | Toggle host mode switch | Host dashboard visible with navigation | P1 |
| HOST-02 | Host dashboard loads | Enter host mode | Dashboard with listings, earnings, bookings | P1 |
| HOST-03 | View host bookings | Navigate to host bookings | List of guest bookings with accept/decline | P1 |
| HOST-04 | Create listing | Tap create listing | Listing creation form opens | P1 |

### 11. Profile / Settings (PROF)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| PROF-01 | Profile screen loads | Navigate to Profile tab | User name, email, avatar displayed | P0 |
| PROF-02 | Edit personal info | Settings → Personal Info → edit name | Name updates on profile | P1 |
| PROF-03 | Notification settings | Settings → Notifications | Toggle switches functional | P1 |
| PROF-04 | Account deletion | Settings → Delete Account | Confirmation dialog, account deleted | P1 |

### 12. Deep Links (LINK)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| LINK-01 | Booking success link | Open `https://pacedream.com/booking-success?session_id=x` | App opens to confirmation | P0 |
| LINK-02 | Booking cancelled link | Open `https://pacedream.com/booking-cancelled` | App opens to booking screen | P1 |
| LINK-03 | Auth0 callback | Complete Auth0 login → callback | `pacedream://callback` handled, login completes | P0 |
| LINK-04 | Cold start deep link | Kill app → open deep link | App starts and navigates to correct screen | P1 |

### 13. Stability / Edge Cases (STAB)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| STAB-01 | Airplane mode → API call | Enable airplane mode → pull refresh | Friendly error message, no crash | P0 |
| STAB-02 | Slow network | Throttle to 2G → browse | Loading states visible, no ANR | P1 |
| STAB-03 | Process death recovery | Put in background → kill process → resume | State restored, no blank screen | P0 |
| STAB-04 | Rotation / config change | Rotate during load | Content preserved, no duplicate requests | P1 |
| STAB-05 | Back navigation | Press back from every screen | Correct back stack behavior | P0 |
| STAB-06 | Release build smoke test | Install prodRelease APK | No R8 crashes, all flows functional | P0 |
| STAB-07 | Memory pressure | Open multiple images → check profiler | No OOM, Coil releases bitmaps | P1 |
| STAB-08 | Rapid tap protection | Double-tap Book Now quickly | Only one booking created | P1 |

### 14. Location (LOC)

| # | Case | Steps | Expected | Priority |
|---|------|-------|----------|----------|
| LOC-01 | Location permission grant | App requests location → grant | Current location populated in search | P1 |
| LOC-02 | Location permission deny | App requests location → deny | Falls back to manual location entry | P1 |
| LOC-03 | Location permission revoke | Grant → revoke in settings → relaunch | Graceful fallback, no crash | P1 |

---

## Automated Test Coverage

| Module | Unit Tests | Instrumented Tests |
|--------|-----------|-------------------|
| core:network | ApiErrorTest, ApiResultOperationsTest, RefreshWithFallbackTest, AuthSessionParsingTest, AppConfigTest | ExampleInstrumentedTest |
| feature:home | HomeFeedModelTest, HomeFeedRepositoryParsingTest, SearchRepositoryParsingTest | — |
| app | — | Compose UI tests (testManifest) |

**Recommended additions before launch:**
- Token refresh integration test (mock 401 → refresh → retry)
- Booking form validation unit tests
- Wishlist optimistic removal unit tests
- Screenshot regression tests via Roborazzi (framework already configured)

---

## Test Execution Checklist

- [ ] All P0 cases pass on Pixel 7 (API 34)
- [ ] All P0 cases pass on Samsung Galaxy A14 (API 31)
- [ ] POST_NOTIFICATIONS permission flow verified on API 33+ device
- [ ] Release APK (prodRelease) smoke tested — no R8 crashes
- [ ] Deep links verified with `adb shell am start`
- [ ] FCM token appears in server logs after login
- [ ] Stripe checkout completes end-to-end
- [ ] Auth0 Google login works on fresh install
- [ ] Process death recovery verified
- [ ] Airplane mode error handling verified
