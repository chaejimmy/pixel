# PaceDream Android App

Android app with iOS parity for PaceDream - a platform for booking spaces and renting gear.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Serialization**: Kotlinx Serialization
- **Image Loading**: Coil
- **Authentication**: Auth0 Android SDK
- **Secure Storage**: EncryptedSharedPreferences
- **Web Views**: AndroidX Browser Custom Tabs

## Project Structure

```
app/src/main/kotlin/com/pacedream/app/
├── core/
│   ├── config/           # AppConfig - URL normalization, timeouts
│   ├── network/          # ApiClient, ApiError, interceptors
│   ├── auth/             # AuthSession, TokenStorage
│   ├── di/               # Hilt modules
│   └── util/             # Utility functions
├── ui/
│   ├── navigation/       # MainNavHost, NavRoutes, BottomBar
│   ├── components/       # Shared UI components
│   └── designsystem/     # Theme, colors, typography
└── feature/
    ├── home/             # HomeScreen, sections
    ├── wishlist/         # WishlistScreen, optimistic remove
    ├── inbox/            # InboxScreen, ThreadScreen
    ├── profile/          # ProfileScreen, Guest/Host modes
    └── webflow/          # Stripe checkout, booking confirmations
```

## Configuration

### 1. BuildConfig Fields

Add these to your `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        // Backend API URL (appends /v1 automatically)
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://pacedream-backend.onrender.com\"")
        
        // Frontend URL (for Auth0 redirect and deep links)
        buildConfigField("String", "FRONTEND_BASE_URL", "\"https://www.pacedream.com\"")
        
        // Auth0 Configuration
        buildConfigField("String", "AUTH0_DOMAIN", "\"your-tenant.us.auth0.com\"")
        buildConfigField("String", "AUTH0_CLIENT_ID", "\"your-client-id\"")
        buildConfigField("String", "AUTH0_AUDIENCE", "\"https://your-tenant.us.auth0.com/api/v2/\"")
        buildConfigField("String", "AUTH0_SCOPES", "\"openid profile email offline_access\"")
    }
}
```

### 2. Environment Variables (Optional)

You can also use environment variables or a `local.properties` file:

```properties
# local.properties
BACKEND_BASE_URL=https://pacedream-backend.onrender.com
PD_BACKEND_BASE_URL=https://pacedream-backend.onrender.com
FRONTEND_BASE_URL=https://www.pacedream.com
AUTH0_DOMAIN=your-tenant.us.auth0.com
AUTH0_CLIENT_ID=your-client-id
```

## Auth0 Setup

### 1. Create Auth0 Application

1. Go to [Auth0 Dashboard](https://manage.auth0.com/)
2. Create a new Native Application
3. Configure the following:

**Allowed Callback URLs:**
```
pacedream://callback,
com.pacedream.app://auth0
```

**Allowed Logout URLs:**
```
pacedream://callback,
com.pacedream.app://auth0
```

### 2. Update Android Manifest

The app uses custom scheme `pacedream://callback` for Auth0 redirects. This is already configured in the AndroidManifest.xml.

## Deep Links

### App Links Configuration

The app handles these deep links:

| URL Pattern | Destination |
|-------------|-------------|
| `https://www.pacedream.com/booking-success?session_id=...` | Booking Confirmation |
| `https://www.pacedream.com/booking-cancelled` | Booking Cancelled |
| `https://pacedream.com/booking-success?session_id=...` | Booking Confirmation |
| `https://pacedream.com/booking-cancelled` | Booking Cancelled |

### Testing Deep Links

```bash
# Test booking success
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.pacedream.com/booking-success?session_id=cs_test_123"

# Test booking cancelled
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://www.pacedream.com/booking-cancelled"

# Test Auth0 callback
adb shell am start -W -a android.intent.action.VIEW \
  -d "pacedream://callback?code=xxx"
```

### App Links Verification

For production, you need to host a `.well-known/assetlinks.json` file at:
- `https://www.pacedream.com/.well-known/assetlinks.json`
- `https://pacedream.com/.well-known/assetlinks.json`

Content:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.pacedream.app",
    "sha256_cert_fingerprints": ["YOUR_SHA256_FINGERPRINT"]
  }
}]
```

Get your SHA256 fingerprint:
```bash
keytool -list -v -keystore your-keystore.jks -alias your-alias
```

## API Configuration

### URL Normalization (iOS Parity)

The `AppConfig` class normalizes URLs to match iOS behavior:

1. Ensures `https://` scheme
2. Removes trailing slashes
3. Appends `/v1` exactly once for API base URL
4. Frontend URL does NOT get `/v1`

Example:
- Input: `pacedream-backend.onrender.com`
- Output: `https://pacedream-backend.onrender.com/v1`

### URL Building

All URLs are built using `HttpUrl.Builder` (no string concatenation):

```kotlin
// ✅ Correct
val url = appConfig.buildApiUrl("account", "wishlist")
// Result: https://pacedream-backend.onrender.com/v1/account/wishlist

// ❌ Wrong - Never concatenate strings
val url = appConfig.apiBaseUrl.toString() + "/account/wishlist"
```

## Networking Features

### Retry Logic (GET Only)

- 2 retries on timeout/transient errors
- Exponential backoff: 400ms, 800ms
- No retry for POST/PUT/PATCH

### HTML Hardening

HTML responses are treated as service errors and never surface to UI:
- Detects `Content-Type: text/html`
- Shows: "Service is temporarily unavailable. Please try again in a minute."

### In-Flight Request Deduplication

Identical GET requests are deduplicated to avoid redundant network calls.

## Running the App

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build and Run

```bash
# Debug build
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug

# Run tests
./gradlew :app:testDebug
```

### Useful Commands

```bash
# Check for lint errors
./gradlew :app:lintDebug

# Generate dependency graph
./gradlew :app:dependencies

# Clean build
./gradlew clean :app:assembleDebug
```

## iOS Parity Checklist

- [x] Stable bottom tabs (never disappear when logged out)
- [x] Auth is modal overlay, not full screen navigation
- [x] Same backend URL normalization (`/v1` appended once)
- [x] Same error handling (HTML → service unavailable)
- [x] Same retry semantics (GET only, 2 retries, 0.4s/0.8s backoff)
- [x] Same wishlist optimistic remove behavior
- [x] Same inbox tolerant decoding
- [x] Same Stripe checkout flow (Custom Tabs)
- [x] Same deep link handling for booking success/cancelled
- [x] Guest/Host mode persistence

## Troubleshooting

### Auth0 Login Not Working

1. Check that Auth0 callback URLs include your custom scheme
2. Verify the Auth0 domain and client ID in BuildConfig
3. Check logcat for Auth0 errors: `adb logcat | grep -i auth0`

### Deep Links Not Working

1. Verify App Links are verified: `adb shell am start -a android.intent.action.VIEW -d "https://www.pacedream.com/booking-success"`
2. Check Digital Asset Links: `https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://www.pacedream.com`
3. For testing, you may need to manually enable the app as default handler

### Network Errors

1. Check internet permission in AndroidManifest
2. Verify backend URL is correct
3. Check for HTML responses in logcat: `adb logcat | grep -i html`

## License

Proprietary - PaceDream Inc.

