# Android-iOS Parity Implementation Plan

**Goal**: Make the Android app feel visually and behaviorally almost identical to the iOS app across all major flows, while keeping basic Android conventions (system bars, back behavior). Also, align backend requests/responses so Android uses the same endpoints and contracts as iOS.

**Status**: 🚧 In Progress

---

## Executive Summary

Based on comprehensive analysis of the Android codebase and iOS parity documentation, this plan outlines specific changes needed to achieve full parity. The implementation is organized into three main categories:

1. **Visual Alignment** - UI/UX elements that need to match iOS
2. **Behavioral Alignment** - Interaction patterns and animations
3. **Backend API Alignment** - Ensure same endpoints and contracts

---

## Priority 1: Critical Visual Alignment (Immediate)

### 1.1 Hero Section Update
**Current State**: Android shows different text and missing CTA button
**Target State**: Match iOS exactly

**Changes Required**:
- Update headline: "Share it. Borrow it. Split it." → "One place to share it all"
- Update subtitle: "An all-in-one platform to share, book, and split what you need." → "Book, share, or split stays, time, and spaces—on one platform."
- Add "Get to know PaceDream" CTA button with purple gradient and play icon
- Replace placeholder gradient with actual hero background image

**Files to Modify**:
- `feature/home/src/main/kotlin/com/shourov/apps/pacedream/feature/home/HomeFeedScreen.kt`
- Add hero image asset to `app/src/main/res/drawable/`

**Implementation Steps**:
1. Update string resources for hero section
2. Add hero background image asset
3. Create "Get to know PaceDream" button component
4. Update HomeFeedScreen composable to use new text and button
5. Ensure button has play icon and purple gradient styling

---

### 1.2 Search Interface Enhancement
**Current State**: Single search bar, no tabs, missing multi-field search
**Target State**: Use/Borrow/Split tabs with WHAT/WHERE/DATES fields

**Changes Required**:
- Add tab selector with three options: "Use", "Borrow", "Split"
- Implement multi-field search interface:
  - **WHAT**: "Search or type keywords (e.g., meeting rooms, nap pods)"
  - **WHERE**: "City, address, landmark" with location pin icon + "Use my location" button
  - **DATES**: "Add dates" with calendar icon
- Add large purple search button with magnifying glass icon
- Maintain search state across tab switches

**Files to Modify**:
- `feature/search/src/main/kotlin/com/shourov/apps/pacedream/feature/search/SearchScreen.kt`
- Create new `EnhancedSearchBar.kt` component
- Create new `SearchTabSelector.kt` component
- Update `SearchViewModel.kt` to handle tab state

**Implementation Steps**:
1. Create SearchTab enum (Use, Borrow, Split)
2. Create SearchTabSelector composable
3. Create multi-field search interface (WHAT/WHERE/DATES)
4. Implement location picker with "Use my location" functionality
5. Implement date picker component
6. Update SearchViewModel to track selected tab
7. Wire up search logic to handle different search modes

---

### 1.3 Category Filters Completion
**Current State**: Missing Study Room, Short Stay, Apartment, Luxury Room
**Target State**: All categories from website present

**Changes Required**:
- Add missing categories to category list:
  - Study Room
  - Short Stay
  - Apartment
  - Luxury Room
- Ensure category order matches website
- Verify icons match website design

**Files to Modify**:
- `feature/home/src/main/kotlin/com/shourov/apps/pacedream/feature/home/components/CategoryFilter.kt`
- Add category icons to `common/src/main/java/com/pacedream/common/composables/icons/`
- Update category data model

**Implementation Steps**:
1. Add missing category enum values
2. Create/add icons for new categories
3. Update category filter UI to show all categories
4. Verify order matches website
5. Test category filtering functionality

---

### 1.4 FAQ Section Implementation
**Current State**: Data models exist but no UI implementation
**Target State**: Accessible FAQ section with expandable items

**Changes Required**:
- Create FAQ screen/component with expandable items
- Add FAQ questions from website:
  - How does hourly booking work?
  - What types of spaces can I book?
  - Is there a cancellation policy?
  - How do I contact support?
  - Are there any membership benefits?
  - How do I become a host?
- Add navigation to FAQ from Help/Settings

**Files to Create**:
- `feature/help/src/main/kotlin/com/shourov/apps/pacedream/feature/help/FaqScreen.kt`
- `feature/help/src/main/kotlin/com/shourov/apps/pacedream/feature/help/FaqItem.kt`
- `feature/help/src/main/kotlin/com/shourov/apps/pacedream/feature/help/FaqViewModel.kt`

**Implementation Steps**:
1. Create FAQ screen with expandable items
2. Implement expand/collapse animation (200ms easeInOut)
3. Add FAQ data (questions and answers)
4. Add navigation route to FAQ screen
5. Add "FAQ" option to Profile menu or Help section
6. Style to match iOS design

---

## Priority 2: Behavioral Alignment (High)

### 2.1 Animation Timing Verification
**Current State**: Need to verify all animations match iOS timing
**Target State**: 200ms easeInOut for most transitions

**Changes Required**:
- Verify auth flow mode transitions use 200ms easeInOut
- Verify FAQ expand/collapse uses 200ms easeInOut
- Verify page transitions use consistent timing
- Verify shimmer loading animation is 500ms

**Files to Check**:
- `ui/auth/AuthFlowSheet.kt`
- `feature/home/HomeFeedScreen.kt`
- `common/src/main/java/com/pacedream/common/composables/shimmerEffect.kt`

**Implementation Steps**:
1. Audit all animations in the app
2. Create animation constants file with standard durations
3. Update animations to use consistent 200ms easeInOut
4. Test animations feel identical to iOS

---

### 2.2 Error Handling Standardization
**Current State**: Need to verify inline banners everywhere
**Target State**: All errors show inline banners, not toasts/snackbars

**Changes Required**:
- Verify auth errors show inline banner (not snackbar)
- Verify booking errors show inline banner
- Verify search errors show inline banner
- Strip "Server error 200: " prefix from error messages
- Ensure banner styling: padding 12dp, radius 14dp, error bg @ 10%

**Files to Check**:
- `ui/auth/AuthFlowSheet.kt`
- `feature/booking/BookingFormScreen.kt`
- `feature/search/SearchScreen.kt`
- Create shared `ErrorBanner.kt` component if needed

**Implementation Steps**:
1. Create reusable ErrorBanner component
2. Replace all Snackbar/Toast with ErrorBanner
3. Add error message sanitization (strip prefix)
4. Verify styling matches iOS (padding, radius, colors)
5. Test error display across all flows

---

### 2.3 Optimistic UI Verification
**Current State**: Wishlist has optimistic remove, verify others
**Target State**: All applicable features use optimistic UI

**Changes Required**:
- Verify wishlist optimistic remove with rollback
- Verify inbox message sending shows immediately
- Verify booking confirmation shows immediately
- Add optimistic UI to any missing features

**Files to Check**:
- `feature/wishlist/WishlistViewModel.kt`
- `feature/inbox/ThreadViewModel.kt`
- `feature/booking/BookingViewModel.kt`

**Implementation Steps**:
1. Review all user actions that modify server state
2. Implement optimistic UI for each action
3. Implement rollback logic for failures
4. Test optimistic behavior matches iOS

---

## Priority 3: Backend API Alignment (High)

### 3.1 Endpoint Verification
**Current State**: Verify all endpoints match iOS exactly
**Target State**: Same endpoints and request/response format

**Changes Required**:
- Verify all auth endpoints match iOS AuthEndpoint.swift:
  - `POST /v1/auth/login/email`
  - `POST /v1/auth/signup/email`
  - `POST /v1/auth/auth0/callback`
  - `POST /v1/auth/refresh-token`
  - Fallback: `POST https://www.pacedream.com/api/proxy/auth/refresh-token`
- Verify profile bootstrap endpoints match iOS:
  - Prefer `GET /v1/account/me`
  - Fallback `GET /v1/users/get/profile`
  - Fallback `GET /v1/user/get/profile`
- Verify all other endpoints match iOS

**Files to Check**:
- `core/network/src/main/java/com/shourov/apps/pacedream/core/network/services/PaceDreamApiService.kt`
- `core/network/src/main/java/com/shourov/apps/pacedream/core/network/ApiClient.kt`

**Implementation Steps**:
1. Compare Android endpoints with iOS Endpoints files
2. Update any mismatched endpoints
3. Verify request body formats match iOS
4. Verify response parsing handles all iOS variants
5. Test endpoint calls match iOS behavior

---

### 3.2 Token Extraction Tolerance
**Current State**: Verify tolerant parsing of token fields
**Target State**: Handle all token field variants like iOS

**Changes Required**:
- Verify token extraction handles variants:
  - Access token: `accessToken`, `access_token`, `token`, `jwt`
  - Refresh token: `refreshToken`, `refresh_token`
- Verify token validation (must look like JWT with 3 segments)
- Verify refresh token may be absent for email login/signup

**Files to Check**:
- `core/auth/AuthRepository.kt`
- `core/auth/TokenStorage.kt`
- `core/network/ApiClient.kt`

**Implementation Steps**:
1. Review token extraction code
2. Add support for all token field variants
3. Add JWT format validation
4. Handle missing refresh token gracefully
5. Test with various response formats

---

### 3.3 401 Handling Verification
**Current State**: Verify 401 triggers refresh + retry once
**Target State**: Match iOS refresh behavior exactly

**Changes Required**:
- Verify 401 triggers refresh token call (no auth header)
- Verify original request retried once if refresh succeeds
- Verify fallback to proxy endpoint if primary refresh fails
- Verify tokens cleared and user logged out if refresh fails
- Verify refresh requests are deduplicated

**Files to Check**:
- `core/network/ApiClient.kt`
- `core/auth/AuthSession.kt`

**Implementation Steps**:
1. Review 401 handling logic
2. Verify refresh attempt uses no auth header
3. Verify retry logic after successful refresh
4. Verify fallback endpoint order
5. Test 401 handling scenarios

---

### 3.4 HTML Response Hardening
**Current State**: Verify HTML responses treated as service unavailable
**Target State**: Match iOS HTML safety behavior

**Changes Required**:
- Verify HTML response detection:
  - `Content-Type: text/html`
  - Body starts with `<!doctype html` or `<html`
- Verify friendly error message: "Service is temporarily unavailable. Please try again in a minute."
- Verify never surface HTML to UI

**Files to Check**:
- `core/network/ApiClient.kt`

**Implementation Steps**:
1. Review HTML detection logic
2. Verify error message matches iOS
3. Test with HTML responses
4. Ensure HTML never shown to user

---

## Priority 4: Design System Verification (Medium)

### 4.1 Color Palette Verification
**Current State**: Verify colors match iOS 26 system palette
**Target State**: Exact color hex values match iOS

**Changes Required**:
- Verify primary colors:
  - PaceDream Primary: `#5527D7`
  - System Indigo: `#5856D6`
  - System Blue: `#007AFF`
  - System Purple: `#AF52DE`
- Verify neutral colors match iOS gray scale
- Verify status colors (success, warning, error, info)

**Files to Check**:
- `common/src/main/java/com/pacedream/common/composables/theme/Color.kt`
- Compare with iOS Colors.swift

**Implementation Steps**:
1. Extract iOS color hex values (if accessible)
2. Compare with Android color definitions
3. Update any mismatched colors
4. Verify in light and dark modes
5. Test visual appearance matches iOS

---

### 4.2 Typography Verification
**Current State**: Verify text styles match iOS HIG
**Target State**: Same font sizes and weights as iOS

**Changes Required**:
- Verify text styles match iOS naming and sizes:
  - Large Title: 34sp Bold
  - Title 1: 28sp Bold
  - Title 2: 22sp Bold
  - Title 3: 20sp SemiBold
  - Headline: 17sp SemiBold
  - Body: 17sp Normal
  - Callout: 16sp Normal
  - Subheadline: 15sp Normal
  - Caption: 12sp Normal

**Files to Check**:
- `common/src/main/java/com/pacedream/common/composables/theme/Type.kt`

**Implementation Steps**:
1. Compare text styles with iOS Typography
2. Update any mismatched sizes or weights
3. Verify line heights match iOS
4. Test text appearance matches iOS

---

### 4.3 Spacing and Corners Verification
**Current State**: Verify spacing and corner radius match iOS
**Target State**: Same values as iOS

**Changes Required**:
- Verify spacing system: 4dp, 8dp, 16dp, 24dp, 32dp, 48dp, 64dp
- Verify corner radius: 8dp, 12dp, 16dp, 20dp, 24dp
- Verify border/divider: 1dp thickness, black @ 10% alpha

**Files to Check**:
- `common/src/main/java/com/pacedream/common/composables/theme/Spacing.kt`
- `common/src/main/java/com/pacedream/common/composables/theme/Shape.kt`

**Implementation Steps**:
1. Compare spacing values with iOS
2. Compare corner radius values with iOS
3. Update any mismatched values
4. Test layout spacing matches iOS

---

## Priority 5: Additional Features (Medium)

### 5.1 Support/Contact Section Enhancement
**Current State**: Support exists in Profile menu
**Target State**: Dedicated support section matching website

**Changes Required**:
- Create dedicated support screen with:
  - "Need Help?" heading
  - Support description
  - "Message Support" button
  - "Shoot a Direct Mail" button
- Link from Profile menu
- Implement support messaging functionality

**Files to Create**:
- `feature/help/src/main/kotlin/com/shourov/apps/pacedream/feature/help/SupportScreen.kt`

**Implementation Steps**:
1. Create support screen UI
2. Implement "Message Support" functionality
3. Implement direct mail functionality
4. Add navigation route
5. Link from Profile menu
6. Style to match iOS

---

### 5.2 Email Subscription (Optional)
**Current State**: Not implemented
**Target State**: Optional for mobile, can be added later

**Decision**: Low priority for mobile app, can be added in future iteration

---

## Testing Checklist

### Visual Testing
- [ ] Hero section matches iOS design and text
- [ ] Search interface has Use/Borrow/Split tabs
- [ ] Multi-field search (WHAT/WHERE/DATES) works
- [ ] All category filters present and match iOS
- [ ] FAQ section accessible and matches iOS
- [ ] Colors match iOS exactly (compare side by side)
- [ ] Typography matches iOS (sizes, weights, line heights)
- [ ] Spacing and layout match iOS feel
- [ ] Corner radius matches iOS (buttons, cards, modals)
- [ ] Error banners match iOS styling

### Behavioral Testing
- [ ] Auth flow mode transitions use 200ms easeInOut
- [ ] FAQ expand/collapse uses 200ms easeInOut
- [ ] Page transitions use consistent timing
- [ ] Shimmer loading is 500ms
- [ ] All errors show inline banners (no toasts)
- [ ] Error messages have "Server error 200:" prefix stripped
- [ ] Wishlist optimistic remove with rollback works
- [ ] Inbox messages show optimistically
- [ ] Booking confirmation shows immediately

### Backend API Testing
- [ ] All auth endpoints match iOS
- [ ] Profile bootstrap endpoints match iOS
- [ ] Token extraction handles all variants
- [ ] Refresh token may be absent for email login
- [ ] 401 triggers refresh + retry once
- [ ] Refresh fallback endpoints work
- [ ] HTML responses show friendly error
- [ ] In-flight GET requests deduplicated
- [ ] Retry logic: GET only, 2 retries, 0.4s/0.8s backoff
- [ ] URL normalization appends /v1 exactly once

### Cross-Platform Testing
- [ ] Compare Android and iOS side by side
- [ ] Test same user flows on both platforms
- [ ] Verify visual appearance is nearly identical
- [ ] Verify behavior feels the same
- [ ] Verify error messages match
- [ ] Verify loading states match
- [ ] Verify animation timing matches

---

## Implementation Timeline

### Phase 1: Critical Visual Alignment (2-3 days)
- Hero section update
- Search interface enhancement
- Category filters completion
- FAQ section implementation

### Phase 2: Behavioral Alignment (1-2 days)
- Animation timing verification
- Error handling standardization
- Optimistic UI verification

### Phase 3: Backend API Alignment (1-2 days)
- Endpoint verification
- Token extraction tolerance
- 401 handling verification
- HTML response hardening

### Phase 4: Design System Verification (1 day)
- Color palette verification
- Typography verification
- Spacing and corners verification

### Phase 5: Additional Features (1-2 days)
- Support/contact section enhancement
- Final testing and polish

**Total Estimated Time**: 6-10 days

---

## Success Criteria

The Android app will be considered to have full iOS parity when:

1. **Visual Parity**: Side-by-side comparison shows nearly identical UI
2. **Behavioral Parity**: Interactions feel the same (animations, transitions, error handling)
3. **Backend Parity**: Same endpoints, same request/response formats, same error handling
4. **User Experience**: Users can't tell which platform they're on (except system-level differences)
5. **All Tests Pass**: Testing checklist above is 100% complete

---

## Notes

- **Android Conventions**: Keep system bars, back button behavior, and other Android-specific patterns
- **Performance**: Ensure changes don't negatively impact performance
- **Accessibility**: Maintain or improve accessibility features
- **Testing**: Add unit tests for new logic, UI tests for new screens
- **Documentation**: Update documentation as changes are made

---

## Next Steps

1. Review this plan with team
2. Get approval for implementation approach
3. Create feature branches for each priority area
4. Begin implementation in priority order
5. Conduct regular testing and validation
6. Document any deviations or challenges
7. Final QA and cross-platform comparison
8. Release and monitor

---

**Document Version**: 1.0
**Last Updated**: 2026-02-10
**Owner**: Development Team
**Status**: ✅ Plan Complete, Ready for Implementation
