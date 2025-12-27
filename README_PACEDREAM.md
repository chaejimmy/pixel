# PaceDream Android App

Android port of the PaceDream iOS app, built with Kotlin and Jetpack Compose. This app mirrors the iOS app's UI/UX, backend API behavior, and feature set.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with StateFlow
- **Navigation**: Navigation Compose
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp (with HttpUrl.Builder for URL building)
- **Serialization**: Kotlinx Serialization with tolerant parsing
- **Images**: Coil
- **Token Storage**: EncryptedSharedPreferences (Jetpack Security)
- **Web Flows**: Chrome Custom Tabs

## Configuration

### Environment Variables

Create or update `secrets.defaults.properties` in the project root with:

```properties
# Backend API URL - will be normalized to append /v1
BACKEND_BASE_URL=https://api.pacedream.com
# Alternative key (fallback)
PD_BACKEND_BASE_URL=https://api.pacedream.com

# Frontend URL - used for proxy endpoints and deep links (NO /v1)
FRONTEND_BASE_URL=https://www.pacedream.com
# Alternative key (fallback)
PD_FRONTEND_BASE_URL=https://www.pacedream.com
```

The `AppConfig` class normalizes URLs:
- `apiBaseUrl` = `https://<host>/v1` (appends `/v1` exactly once, removes trailing slashes)
- `frontendBaseUrl` = `https://<host>` (no `/v1`)

### Auth0 Configuration

Add Auth0 credentials to your `secrets.defaults.properties`:

```properties
AUTH0_DOMAIN=your-domain.auth0.com
AUTH0_CLIENT_ID=your-client-id
AUTH0_SCHEME=com.shourov.apps.pacedream
```

Callback URLs to configure in Auth0 Dashboard:
- **Allowed Callback URLs**: `com.shourov.apps.pacedream://auth0/callback`
- **Allowed Logout URLs**: `com.shourov.apps.pacedream://auth0/logout`

### Build Flavors

The app supports two flavors:
- `demo`: Uses demo/staging backend
- `prod`: Uses production backend

## Deep Links

The app handles the following deep links for Stripe checkout flow:

### Booking Success
```
https://www.pacedream.com/booking-success?session_id=<stripe_session_id>
```

### Booking Cancelled
```
https://www.pacedream.com/booking-cancelled
```

### Testing Deep Links

Use `adb` to test deep links:

```bash
# Test booking success
adb shell am start -a android.intent.action.VIEW \
  -d "https://www.pacedream.com/booking-success?session_id=cs_test_abc123" \
  com.shourov.apps.pacedream

# Test booking cancelled
adb shell am start -a android.intent.action.VIEW \
  -d "https://www.pacedream.com/booking-cancelled" \
  com.shourov.apps.pacedream
```

### App Links Verification

For verified app links, add the following to your `assetlinks.json` on your domain:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.shourov.apps.pacedream",
    "sha256_cert_fingerprints": ["YOUR_SIGNING_KEY_FINGERPRINT"]
  }
}]
```

Host this at: `https://www.pacedream.com/.well-known/assetlinks.json`

## Project Structure

```
app/
├── src/main/kotlin/com/shourov/apps/pacedream/
│   ├── MainActivity.kt           # Main activity with deep link handling
│   ├── PaceDreamApplication.kt   # Application class
│   ├── navigation/               # Navigation setup and bottom tabs
│   ├── feature/                  # Feature implementations (inline)
│   └── ui/                       # App-level UI components

core/
├── network/
│   ├── api/                      # ApiClient, ApiError, ApiResult
│   ├── auth/                     # AuthSession, TokenStorage
│   ├── config/                   # AppConfig with URL normalization
│   └── di/                       # Hilt modules
├── model/                        # Shared data models
├── designsystem/                 # Design tokens and theme
└── ui/                           # Shared UI components

feature/
├── wishlist/                     # Wishlist/Favorites with optimistic remove
├── inbox/                        # Messaging with tolerant parsing
├── webflow/                      # Stripe checkout and deep links
├── home/                         # Home screen sections
├── signin/                       # Authentication flow
└── ...                           # Other features
```

## Key Features

### Networking Layer (iOS Parity)

- **Timeouts**: Request 30s, Resource/Read 60s
- **Retry Logic**: GET requests only, 2 retries with 0.4s/0.8s backoff
- **HTML Hardening**: Detects HTML responses and shows friendly error messages
- **GET Deduplication**: Identical in-flight GET requests share results
- **URL Building**: All URLs built via `HttpUrl.Builder` (no string concatenation)

### Authentication

- **Token Storage**: EncryptedSharedPreferences for secure storage
- **Auto-refresh**: Attempts token refresh on 401, with fallback endpoint
- **Immediate Auth**: Marks authenticated immediately on app launch if token exists
- **Graceful Degradation**: Keeps user authenticated on non-401 errors

### Wishlist

- **Optimistic Remove**: Item removed from UI immediately
- **Rollback on Failure**: Restores item if API fails or returns unexpected `liked=true`
- **Routing Rules**: Routes to correct detail screen based on item type

### Messaging

- **Tolerant Parsing**: Extracts minimal fields if strict parsing fails
- **REST-first**: Works even if realtime is unavailable
- **Flexible Response Handling**: Supports various wrapper formats

### Stripe Checkout

- **Custom Tabs**: Opens checkout URL in Chrome Custom Tabs
- **Session Persistence**: Stores session ID for resume after app relaunch
- **Deep Link Handling**: Processes booking-success/booking-cancelled

## Building the App

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
./gradlew test
```

## API Endpoints Reference

### Authentication
- `POST /v1/auth/auth0/callback` - Exchange Auth0 tokens
- `POST /v1/auth/refresh-token` - Refresh access token
- `GET /v1/account/me` - Get current user profile

### Wishlist
- `GET /v1/account/wishlist` - Get wishlist items
- `POST /v1/account/wishlist/toggle` - Add/remove from wishlist

### Messaging
- `GET /v1/inbox/threads` - Get message threads
- `GET /v1/inbox/unread-counts` - Get unread message counts
- `GET /v1/inbox/threads/:id/messages` - Get thread messages
- `POST /v1/inbox/threads/:id/messages` - Send message
- `POST /v1/inbox/threads/:id/archive` - Archive thread

### Bookings
- `POST /v1/properties/bookings/timebased` - Create time-based booking
- `POST /v1/gear-rentals/book` - Create gear booking
- `GET /v1/properties/bookings/timebased/success/checkout` - Confirm time-based booking
- `GET /v1/gear-rentals/success/checkout` - Confirm gear booking

## Troubleshooting

### Deep Links Not Working
1. Verify `assetlinks.json` is correctly hosted
2. Check app is installed from Play Store or signed with correct key
3. Use `adb shell pm get-app-links com.shourov.apps.pacedream` to check verification status

### Token Refresh Failing
1. Check refresh token is valid
2. Verify network connectivity
3. Check server logs for refresh endpoint

### HTML Response Errors
If you see "Service is temporarily unavailable":
1. Check backend is running
2. Verify base URL is correct
3. Check for CDN/proxy issues returning HTML error pages

## License

Proprietary - PaceDream Inc.

