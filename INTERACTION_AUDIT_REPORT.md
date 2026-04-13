# Android Interaction Audit Report

**Date:** 2026-04-13
**Scope:** Every Button, Clickable, Card, IconButton, navigation action, and form submission across 174 Composable files

---

## Executive Summary

Full audit of all interactive UI elements across the PaceDream Android app. Reviewed 625+ Kotlin files containing 664+ onClick handlers, 161 IconButtons, 122 onValueChange handlers, and all navigation routes.

**Total issues found: 53**

| Severity | Count | Description |
|----------|-------|-------------|
| CRITICAL | 11 | Broken core flows, dead primary CTAs, auth bypass |
| HIGH | 18 | Broken navigation, dead click regions, empty feature handlers |
| MEDIUM | 17 | Default empty lambdas on important callbacks, state bugs, missing confirmations |
| LOW | 7 | Default empty lambdas on secondary callbacks, dead code |

---

## CRITICAL Issues (11)

### C-01: Login success navigates to CREATE_ACCOUNT instead of dashboard
- **File:** `feature/signin/.../screens/signIn/SignInScreen.kt:145`
- **Composable:** `SignIn`
- **Element:** `ProcessButton` ("Continue")
- **Code:** `viewModel.login(onSuccess = { navController.navigate(route = SignInRoutes.CREATE_ACCOUNT.name) })`
- **Impact:** After successful login, users are sent to account creation instead of the main app. Core auth flow is broken.

### C-02: "Forgot Password" gives zero user feedback
- **File:** `feature/signin/.../screens/signIn/SignInScreen.kt:129`
- **Composable:** `SignIn`
- **Element:** `PrimaryTextButton` ("Forgot Password")
- **Code:** `viewModel.forgotPassword(onSuccess = { message -> })` -- empty lambda
- **Impact:** User taps "Forgot Password", API call fires, but no snackbar/toast/navigation occurs. No LaunchedEffect handles success in this file. User gets no confirmation that a reset email was sent.

### C-03: Phone OTP validation bypassed
- **File:** `core/ui/.../PhoneEntryScreen.kt:111-116`
- **Composable:** `PhoneEntryScreen`
- **Element:** `ProcessButton` ("Continue")
- **Code:** Calls `onNavigateToAccountSetup()` directly. The actual validation (`if (phoneNumberState.isValid) { onProceedToOtpVerification() }`) is commented out.
- **Impact:** Users proceed past phone entry without valid phone number or OTP verification. Authentication security bypass.

### C-04: BaseeScreen back button is a TODO stub
- **File:** `common/.../components/BaseeScreen.kt:55-57`
- **Composable:** `BaseeScreen`
- **Element:** `RoundIconButton` (back arrow)
- **Code:** `//TODO on click implementation, navigate back` -- empty body
- **Impact:** Any screen using `BaseeScreen` has a non-functional back button, trapping users.

### C-05: "Rent Now" button on DealsCard does nothing
- **File:** `feature/home/presentation/.../components/DealsCard.kt:156`
- **Composable:** `DealsCard`
- **Element:** `ProcessButton` ("Rent Now")
- **Code:** `onClick = {}` -- hardcoded empty
- **Impact:** Primary revenue CTA on room deal cards is dead. The card's outer `onClick` exists but the button ignores it entirely.

### C-06: "Book Now" button on LastMinuteDealCard does nothing
- **File:** `feature/home/presentation/.../components/DealsCard.kt:281`
- **Composable:** `LastMinuteDealCard`
- **Element:** `ProcessButton` ("Book Now")
- **Code:** `onClick = {}` -- hardcoded empty
- **Impact:** Booking CTA on last-minute deal cards is completely dead.

### C-07: "Rent Now" button on RentedGearDealsCard does nothing
- **File:** `feature/home/presentation/.../components/DealsCard.kt:349`
- **Composable:** `RentedGearDealsCard`
- **Element:** `ProcessButton` ("Rent Now")
- **Code:** `onClick = {}` -- hardcoded empty
- **Impact:** Rental CTA on gear cards is completely dead.

### C-08: HomeScreen notification bell hardcoded to empty lambda
- **File:** `app/.../feature/home/HomeScreen.kt:97`
- **Composable:** `HomeScreen` > `HeroHeaderSection`
- **Code:** `onNotificationClick = {}` -- hardcoded inside the composable, not a default parameter
- **Impact:** Even when the parent supplies all callbacks, the notification bell always does nothing. This is not overrideable by the caller.

### C-09: "Post a Roommate Listing" button is a TODO stub
- **File:** `app/.../feature/roommate/RoommateFinderScreen.kt:235`
- **Composable:** `RoommateFinderScreen`
- **Element:** `Button`
- **Code:** `onClick = { /* TODO: Navigate to post roommate listing */ }`
- **Impact:** Primary CTA for the entire Roommate Finder feature does nothing.

### C-10: RoommateFinderScreen filter icon is a TODO stub
- **File:** `app/.../feature/roommate/RoommateFinderScreen.kt:80`
- **Composable:** `RoommateFinderScreen`
- **Element:** `IconButton`
- **Code:** `onClick = { /* TODO: Open advanced filters */ }`
- **Impact:** Advanced filter button in the search bar does nothing.

### C-11: BookingDetailScreen "Write a Review" button is a no-op
- **File:** `app/.../feature/bookings/BookingDetailScreen.kt:67`
- **Composable:** `BookingDetailScreen`
- **Element:** `Button` ("Write a Review") visible on completed bookings
- **Code:** `onWriteReview` defaults to `{ _, _, _, _, _ -> }`. No call-site wiring found.
- **Impact:** Users see a styled review button on completed bookings; tapping it does nothing.

---

## HIGH Issues (18)

### H-01: Host mode property navigation silently fails
- **File:** `app/.../ui/PaceDreamApp.kt:62`
- **Composable:** `PaceDreamApp`
- **Code:** `onNavigateToProperty` navigates to `"listing/$propertyId"` on the top-level NavHost, but that route is only registered in the nested dashboard NavHost with a different pattern (`"DETAIL/{propertyId}"`).
- **Impact:** Navigating to a property from host mode silently fails (try-catch swallows the exception).

### H-02: Host mode booking navigation silently fails
- **File:** `app/.../ui/PaceDreamApp.kt:66`
- **Composable:** `PaceDreamApp`
- **Code:** `onNavigateToBooking` navigates to `"booking_details/$bookingId"` which is not registered as a composable destination anywhere.
- **Impact:** Navigating to a booking from host mode silently fails.

### H-03: "Apply Filters" button does nothing
- **File:** `app/.../navigation/DashboardNavigation.kt:746`
- **Composable:** `FilterScreen` composable route
- **Code:** `onApplyFilters = { /* Handle filter application */ }` -- empty lambda
- **Impact:** Users can configure filters but applying them has zero effect.

### H-04: Edit listing calendar navigation targets wrong NavHost
- **File:** `app/.../navigation/DashboardNavigation.kt:932`
- **Composable:** `EditListingScreen` composable route
- **Code:** `onManageCalendarClick` navigates to `"listing_calendar/$id"` in the dashboard NavHost, but that route only exists in the Host NavHost.
- **Impact:** Managing calendar from guest-mode edit listing will crash or silently fail.

### H-05: Terms of Service / Privacy Policy links not clickable (SignIn)
- **File:** `feature/signin/.../screens/signIn/SignInScreen.kt:165-182`
- **Composable:** `SignIn`
- **Element:** `Text` with styled "Terms of Service" and "Privacy Policy" (underlined, primary color)
- **Impact:** Text looks like clickable links but uses plain `Text` composable with no click handler. Potential compliance issue.

### H-06: Terms/Privacy links not clickable (CreateAccount)
- **File:** `feature/signin/.../screens/createAccount/CreateAccountScreen.kt:150-166`
- **Composable:** `CreateAccountScreen`
- **Element:** `Text` with `pushStringAnnotation` for URLs -- `uriHandler` is declared but never used
- **Impact:** Same compliance issue. Annotations pushed but never consumed.

### H-07: Chat "Call" and "Video Call" buttons are empty stubs
- **File:** `feature/chat/.../ChatScreen.kt:251,259`
- **Composable:** `ChatHeader`
- **Elements:** Two `IconButton` composables
- **Code:** `onClick = { /* Handle call */ }` and `onClick = { /* Handle video call */ }`
- **Impact:** Prominent header buttons that users will expect to work are entirely non-functional.

### H-08: SettingsRootScreen logout defaults to empty lambda
- **File:** `app/.../feature/settings/SettingsRootScreen.kt:71`
- **Composable:** `SettingsRootScreen`
- **Code:** `onLogoutClick: () -> Unit = {}`
- **Impact:** If caller forgets to supply handler, user goes through confirmation dialog, taps "Log out", dialog closes, but no logout occurs.

### H-09: SettingsRootScreen identity verification defaults to empty
- **File:** `app/.../feature/settings/SettingsRootScreen.kt:70`
- **Composable:** `SettingsRootScreen`
- **Code:** `onIdentityVerificationClick: () -> Unit = {}`
- **Impact:** Row renders with chevron implying navigation. If not wired, tapping does nothing.

### H-10: RoommateFinderScreen search bar is disconnected
- **File:** `app/.../feature/roommate/RoommateFinderScreen.kt:73`
- **Composable:** `RoommateFinderScreen`
- **Element:** `OutlinedTextField` + `FilterChip`s
- **Impact:** `searchQuery` and `selectedFilter` stored in local state but never sent to any ViewModel/API. Search is purely cosmetic.

### H-11: DashboardContent -- all card onClick handlers are empty
- **File:** `feature/home/presentation/.../components/DashboardContent.kt`
- **Lines:** 123 (CategoryCard), 129 (RecentSearchCard), 140 (DestinationCard), 174 (DealsCard), 208 (RentedGearDealsCard)
- **Code:** All pass `{ }` as onClick
- **Impact:** The entire original DashboardContent screen is non-interactive. Every card tap does nothing.

### H-12: DashboardHeader search button is empty
- **File:** `feature/home/presentation/.../components/DashboardHeader.kt:156`
- **Composable:** `DashboardHeader`
- **Element:** `IconButton` (search icon)
- **Code:** `onClick = { }`
- **Impact:** Primary search trigger does nothing.

### H-13: DashboardHeader notification icon not clickable
- **File:** `feature/home/presentation/.../components/DashboardHeader.kt:126`
- **Composable:** `DashboardHeader`
- **Element:** `Image` (notification bell) -- plain Image, no `clickable` modifier
- **Impact:** Users see a notification icon but cannot interact with it.

### H-14: GeneralSearchBar traps search input
- **File:** `common/.../inputfields/GeneralSearchBar.kt:45-85`
- **Composable:** `GeneralSearchBar`
- **Impact:** Internal state only via `rememberSaveable`. No `onValueChange` callback parameter exists. Parent composable can never read search text.

### H-15: FilterScreen "Clear All" is a comment placeholder
- **File:** `feature/home/presentation/.../components/FilterScreen.kt:55`
- **Composable:** `FilterScreen`
- **Code:** `onActionClick = { /* Clear all filters */ }` -- comment-only body
- **Impact:** Users cannot reset their filter selections.

### H-16: FilterScreen price range slider is commented out
- **File:** `feature/home/presentation/.../components/FilterScreen.kt:108-114`
- **Composable:** `FilterScreen`
- **Impact:** `RangeSlider` is fully commented out. Static "$0 - $1000" text shows but cannot be adjusted.

### H-17: "View All" text is never clickable
- **File:** `common/.../texts/TitleText.kt:92-103`
- **Composable:** `ViewAllText`
- **Impact:** Plain `Text` with no `clickable` modifier or `onClick`. Every "View All" link on DashboardContent is non-functional.

### H-18: ProfileScreen 6 navigation rows default to empty lambdas
- **File:** `app/.../feature/profile/ProfileScreen.kt:49-54`
- **Composable:** `ProfileScreen`
- **Impact:** `onIdentityVerificationClick`, `onHelpClick`, `onAboutClick`, `onMyListsClick`, `onBookingsClick`, `onFavoritesClick` all default to `{}`. Visible tappable rows that may do nothing.

---

## MEDIUM Issues (17)

### M-01: ConfirmationScreen X-button contradicts BackHandler
- **File:** `app/.../feature/checkout/ConfirmationScreen.kt:56,63`
- **Composable:** `ConfirmationScreen`
- **Impact:** `BackHandler` intercepts system back to call `onDone()`, preventing return to checkout. But the X button calls `onBackClick` which may pop the nav stack back to checkout -- contradicting the BackHandler's intent.

### M-02: WriteReviewScreen post-submit navigation defaults to empty
- **File:** `app/.../feature/reviews/WriteReviewScreen.kt:241`
- **Composable:** `WriteReviewScreen`
- **Code:** `onReviewSubmitted: () -> Unit = {}`
- **Impact:** Review submits successfully but user may be stuck on success screen with no auto-navigation.

### M-03: CollectionsScreen delete has no confirmation dialog
- **File:** `app/.../feature/collections/CollectionsScreen.kt:311`
- **Composable:** `CollectionCard`
- **Impact:** `viewModel.deleteCollection(collection.id)` fires immediately on tap. Accidental taps permanently delete.

### M-04: TripPlannerScreen delete trip -- no confirmation, failure swallowed
- **File:** `app/.../feature/tripplanner/TripPlannerScreen.kt:255,351`
- **Composable:** `TripCard`
- **Impact:** `viewModel.deleteTrip(trip.id)` immediate. `is ApiResult.Failure -> {}` silently ignored.

### M-05: TripPlannerScreen TourCards are not clickable
- **File:** `app/.../feature/tripplanner/TripPlannerScreen.kt:361-383`
- **Composable:** `TourCard`
- **Impact:** Cards display tour info but have no `clickable` modifier -- users cannot view details or book.

### M-06: SettingsHelpSupportScreen email intent silently caught
- **File:** `app/.../feature/settings/help/SettingsHelpSupportScreen.kt:127-136,148-158`
- **Composable:** `SettingsHelpSupportScreen`
- **Impact:** On devices without email clients, "Email Support" and "Report an Issue" silently fail.

### M-07: RoommateFinderScreen preference chips non-interactive
- **File:** `app/.../feature/roommate/RoommateFinderScreen.kt:162-178`
- **Composable:** `RoommatePreferenceChip`
- **Impact:** "Budget", "Location", "Move-in", "Lifestyle" chips are `Surface` with no `clickable` modifier.

### M-08: AboutUsScreen link items have small tap targets
- **File:** `app/.../feature/about/AboutUsScreen.kt:239-261`
- **Composable:** `AboutLinkItem`
- **Impact:** Only the "Open" TextButton is tappable; the full row (icon + title) is not clickable.

### M-09: DashboardNavigation -- destinationId hardcoded empty
- **File:** `app/.../navigation/DashboardNavigation.kt:897`
- **Code:** `DestinationListingsScreen(destinationId = "")`
- **Impact:** Destinations screen from Profile tab always loads with empty ID, showing wrong/empty content.

### M-10: CustomInputTextField state desynchronization
- **File:** `common/.../inputfields/CustomInputTextField.kt:90,116`
- **Composable:** `CustomInputTextField`
- **Impact:** `remember { mutableStateOf(value) }` captures initial value only. Parent updates to `value` are ignored after first composition. Clear button visibility checks the stale parameter.

### M-11: CustomPasswordField same stale state bug
- **File:** `common/.../inputfields/CustomPasswordField.kt:71`
- **Composable:** `CustomPasswordField`
- **Impact:** Same issue as M-10. Password field won't reflect external state changes.

### M-12: HostInboxScreen onThreadClick is a dead parameter
- **File:** `app/.../feature/host/presentation/HostInboxScreen.kt:17`
- **Composable:** `HostInboxScreen`
- **Impact:** Parameter accepted but never used inside the composable. Callers are misled.

### M-13: HostListingsScreen onDeleteListingClick defaults to empty
- **File:** `app/.../feature/host/presentation/HostListingsScreen.kt:35`
- **Composable:** `HostListingsScreen`
- **Impact:** Delete icon button on each listing card silently does nothing if not wired. No confirmation dialog either.

### M-14: HostProfileScreen multiple settings rows default to empty
- **File:** `app/.../feature/host/presentation/HostProfileScreen.kt:45-52`
- **Composable:** `HostProfileScreen`
- **Impact:** `onEditProfileClick`, `onEditPhotoClick`, `onAccountSettingsClick`, `onPersonalInfoClick` all default to `{}`.

### M-15: HostSettingsScreen notification/help rows default to empty
- **File:** `app/.../feature/host/presentation/HostSettingsScreen.kt:36-37`
- **Composable:** `HostSettingsScreen`
- **Impact:** `onNotificationsClick` and `onHelpClick` default to `{}`.

### M-16: BookingScreen notification header button empty
- **File:** `feature/booking/.../BookingScreen.kt:73`
- **Composable:** `BookingScreen`
- **Code:** `PaceDreamHeroHeader(onNotificationClick = { /* Handle notification */ })`
- **Impact:** Notification bell in header does nothing.

### M-17: ChatListScreen notification header button empty
- **File:** `feature/chat/.../ChatListScreen.kt:65`
- **Composable:** `ChatListScreen`
- **Code:** `PaceDreamHeroHeader(onNotificationClick = { /* Handle notification */ })`
- **Impact:** Notification bell in header does nothing.

---

## LOW Issues (7)

### L-01: PaceDreamApp navigation icon mislabeled
- **File:** `app/.../ui/PaceDreamApp.kt:102`
- **Impact:** `navigationIconContentDescription = "Open navigation menu"` but action is `popBackStack()`.

### L-02: PaceDreamApp dead callbacks for host navigation
- **File:** `app/.../ui/PaceDreamApp.kt:70-83`
- **Impact:** `onNavigateToAddListing`, `onNavigateToEditListing`, `onNavigateToAnalytics`, `onNavigateToWithdraw` are dead code (overridden by HostModeScreen internally).

### L-03: ProfileTabScreen 17 callbacks default to empty
- **File:** `app/.../feature/profile/presentation/ProfileTabScreen.kt:32-49`
- **Impact:** Standard Compose pattern but high risk of missed wiring with 17 default empty lambdas.

### L-04: BiddingScreen onBackClick defaults to empty
- **File:** `app/.../feature/bidding/BiddingScreen.kt:216`
- **Impact:** Back arrow does nothing if caller omits parameter.

### L-05: SplitBookingScreen onBackClick/onSplitClick default to empty
- **File:** `app/.../feature/splitbooking/SplitBookingScreen.kt:262,464`
- **Impact:** Navigation callbacks default to no-ops.

### L-06: Blog/Destination screens callbacks default to empty
- **File:** `app/.../feature/blog/BlogScreen.kt:224-225,407` and `app/.../feature/destination/DestinationScreen.kt:231,331`
- **Impact:** `onBackClick`, `onPostClick`, `onListingClick` default to `{}`.

### L-07: TripPlannerScreen onBackClick defaults to empty
- **File:** `app/.../feature/tripplanner/TripPlannerScreen.kt:207`
- **Impact:** Back arrow does nothing if caller omits parameter.

---

## Screens Verified as Correctly Implemented

The following screens had all interactive elements properly audited and found to be functional:

- **CheckoutScreen** -- Pay, Retry, Back, View Booking all wired to ViewModel
- **BookingsScreen** -- Tab picker, Cancel, Confirm, View Details, Pull-to-refresh all functional
- **EditProfileScreen** -- Photo picker, form fields, Save all wired (email readOnly `onValueChange = {}` is intentional)
- **WishlistScreen** -- Filters, item click, remove, pull-to-refresh all functional
- **InboxScreen** -- Mode toggle, thread click, archive swipe, pull-to-refresh all functional
- **ThreadScreen** -- Send message, attachments, block/unblock, report all functional
- **ReportBlockSheet** -- Report submission, block user with proper error handling
- **SettingsNotificationsScreen** -- All 11 toggles connected to API calls
- **SettingsPersonalInfoScreen** -- All fields update ViewModel, Save works
- **SettingsPreferencesScreen** -- Dropdowns and Save properly connected
- **SettingsLoginSecurityScreen** -- Password change, deactivate, delete with confirmations
- **SettingsPaymentMethodsScreen** -- Add (Stripe), default, delete with confirmation
- **IDVerificationScreen** -- Image pickers, submit, loading states
- **PhoneVerificationScreen** -- Send code, OTP, verify, resend with cooldown
- **FAQScreen** -- Expandable/collapsible items functional
- **OnBoardingScreen** -- Create Account, Sign In buttons navigate correctly
- **ForgotPasswordScreen** -- Send Reset Link, Resend, Back all wired
- **OtpVerificationScreen** -- Verify, Resend, Change phone all functional
- **CreateListingScreen** -- Full wizard flow with validation, draft management, publish
- **EditListingScreen** -- All fields, image management, save with upload
- **ListingCalendarScreen** -- Date selection, time blocking/unblocking, bottom sheet
- **HostEarningsScreen** -- Stripe onboarding, payout requests, tab selection
- **HostBookingsScreen** -- Accept/Decline/Cancel properly delegate to ViewModel
- **HostDashboardScreen** -- All cards, quick actions, sign out with confirmation
- **HomeFeedScreen** (Dashboard) -- Listing click, search, see all, notifications all wired
- **PropertyDetailScreen** -- Back, Share, Favorite, Book, Contact host all functional
- **SearchScreen** (Dashboard) -- Search, sort, categories, listings all functional
- **BookingConfirmationScreen** -- Close, View Booking, Go Home all functional
- **AppBottomNavigation** -- All 5 tabs properly navigate
- **HostBottomNavigation** -- All 5 tabs properly delegate

---

## Top Priority Recommendations

1. **Fix C-01 immediately** -- Login navigating to CREATE_ACCOUNT is the most user-facing critical bug
2. **Fix C-03** -- OTP bypass is a security issue that allows unverified accounts
3. **Wire C-05/C-06/C-07** -- Three revenue-critical "Rent Now"/"Book Now" CTAs are dead
4. **Fix H-01/H-02** -- Host mode cross-navigation is architecturally broken (route mismatch between NavHosts)
5. **Connect H-03** -- Filter application is a core search feature that does nothing
6. **Fix H-05/H-06** -- Terms/Privacy non-clickable links are a compliance risk
7. **Remove or mark C-09/C-10** -- RoommateFinderScreen is essentially a static mockup; either complete it or mark as "Coming Soon"
