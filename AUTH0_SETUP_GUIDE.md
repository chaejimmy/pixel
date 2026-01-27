# Auth0 Google OAuth Setup Guide

## Overview

The Android app uses Auth0 with Google OAuth for authentication, matching the web implementation at `pacedream.com`. This guide explains the current implementation and how to configure it.

## Current Implementation Status

✅ **Already Implemented:**
- Auth0 Android SDK integration
- Google OAuth connection support (`google-oauth2`)
- Callback URL handling (`pacedream://callback`)
- Backend token exchange (`/v1/auth/auth0/callback`)
- Secure token storage
- User profile fetching

✅ **Recently Fixed:**
- BuildConfig field generation from `secrets.defaults.properties`
- Core/network AuthSession now supports connection parameter
- Updated secrets file with Auth0 configuration template

## Required Configuration

### Step 1: Get Your Auth0 Credentials

1. Go to [Auth0 Dashboard](https://manage.auth0.com/)
2. Navigate to **Applications** → Your Application → **Settings**
3. Copy the following values:
   - **Domain**: e.g., `dev-pacedream.us.auth0.com`
   - **Client ID**: Your application's client ID

### Step 2: Configure Auth0 Dashboard

In your Auth0 Application settings, add these callback URLs:

**Allowed Callback URLs:**
```
pacedream://callback
com.pacedream.app://auth0
```

**Allowed Logout URLs:**
```
pacedream://callback
com.pacedream.app://auth0
```

**Note:** The app uses `pacedream://callback` as the primary callback URL, which is already configured in `AndroidManifest.xml`.

### Step 3: Add Auth0 Configuration to secrets.defaults.properties

Open `secrets.defaults.properties` in the project root and add your Auth0 credentials:

```properties
# Backend API URL
SERVICE_URL="https://pacedream-backend.onrender.com/v1/"

# Auth0 Configuration (REQUIRED)
AUTH0_DOMAIN=dev-pacedream.us.auth0.com
AUTH0_CLIENT_ID=your-actual-auth0-client-id-here
AUTH0_AUDIENCE=https://dev-pacedream.us.auth0.com/api/v2/
```

**Important:** Replace `your-actual-auth0-client-id-here` with your actual Auth0 Client ID from the Auth0 Dashboard.

### Step 4: Rebuild the App

After adding the Auth0 configuration, rebuild the app:

```bash
./gradlew clean build
```

This will:
1. Read the Auth0 configuration from `secrets.defaults.properties`
2. Generate BuildConfig fields (`AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, `AUTH0_AUDIENCE`)
3. Make these values available to the app at runtime

## How Authentication Works

### Flow Diagram

```
┌─────────────┐
│ Android App │
└──────┬──────┘
       │
       │ 1. User clicks "Login with Google"
       ▼
┌─────────────────────────────┐
│ SessionManager.loginWithAuth0() │
│ - Uses Auth0 SDK              │
│ - Sets connection=google-oauth2 │
│ - Opens Custom Tabs            │
└──────┬────────────────────────┘
       │
       │ 2. Redirect to Auth0
       ▼
┌─────────────────────────────┐
│ Auth0 Authorize Endpoint     │
│ - Shows Google login         │
│ - User authenticates         │
└──────┬────────────────────────┘
       │
       │ 3. Google OAuth
       ▼
┌─────────────────────────────┐
│ Google OAuth                │
│ - User selects account       │
│ - Grants permissions         │
└──────┬────────────────────────┘
       │
       │ 4. Redirect with code
       ▼
┌─────────────────────────────┐
│ MainActivity (callback)      │
│ - Receives pacedream://callback │
│ - Auth0 SDK handles exchange   │
└──────┬────────────────────────┘
       │
       │ 5. Exchange for backend JWT
       ▼
┌─────────────────────────────┐
│ Backend /v1/auth/auth0/callback │
│ - Validates Auth0 tokens     │
│ - Creates/updates user      │
│ - Returns backend JWT        │
└──────┬────────────────────────┘
       │
       │ 6. Store token & navigate
       ▼
┌─────────────┐
│ Main Screen │
└─────────────┘
```

### Code Locations

**Auth0 Login Implementation:**
- `app/src/main/kotlin/com/pacedream/app/core/auth/AuthSession.kt` - Main auth session manager
- `app/src/main/kotlin/com/pacedream/app/core/auth/Auth0Connection.kt` - Connection enum (Google, Apple)
- `core/network/src/main/java/com/shourov/apps/pacedream/core/network/auth/AuthSession.kt` - Core network auth session

**Configuration:**
- `core/network/build.gradle.kts` - BuildConfig field generation
- `core/network/src/main/java/com/shourov/apps/pacedream/core/network/config/AppConfig.kt` - Reads from BuildConfig
- `app/src/main/kotlin/com/pacedream/app/core/config/AppConfig.kt` - App-level config

**UI:**
- `app/src/main/kotlin/com/pacedream/app/ui/components/AuthBottomSheet.kt` - Login UI component

**AndroidManifest:**
- `app/src/main/AndroidManifest.xml` - Callback URL configuration

## Testing the Implementation

### 1. Verify BuildConfig Fields

After rebuilding, you can verify the BuildConfig fields are generated correctly:

```kotlin
// In a debug build, you can check:
Log.d("Auth0", "Domain: ${BuildConfig.AUTH0_DOMAIN}")
Log.d("Auth0", "Client ID: ${BuildConfig.AUTH0_CLIENT_ID}")
Log.d("Auth0", "Audience: ${BuildConfig.AUTH0_AUDIENCE}")
```

### 2. Test Google Login

1. Launch the app
2. Tap "Login with Google" button
3. You should see:
   - Custom Tabs opens with Auth0 login page
   - Google account selection
   - After selecting account, redirects back to app
   - User is authenticated and navigated to main screen

### 3. Check Logs

Monitor logcat for Auth0-related logs:

```bash
adb logcat | grep -i "auth0\|oauth"
```

Look for:
- Authorization URL (should contain your actual client ID, not `YOUR_AUTH0_CLIENT_ID`)
- Successful token exchange
- Backend JWT received

## Common Issues and Solutions

### Issue 1: `client_id=YOUR_AUTH0_CLIENT_ID` in Authorization URL

**Problem:** Auth0 client ID is not configured in BuildConfig.

**Solution:**
1. Verify `secrets.defaults.properties` has `AUTH0_CLIENT_ID` set
2. Rebuild the app: `./gradlew clean build`
3. Check BuildConfig fields are generated (see testing section above)

### Issue 2: Callback URL Mismatch

**Problem:** Auth0 returns error about invalid callback URL.

**Solution:**
1. In Auth0 Dashboard → Applications → Your App → Settings
2. Add to **Allowed Callback URLs**: `pacedream://callback`
3. Ensure AndroidManifest.xml has the correct intent filter (already configured)

### Issue 3: "Connection not found" Error

**Problem:** Auth0 doesn't recognize `google-oauth2` connection.

**Solution:**
1. In Auth0 Dashboard → Authentication → Social
2. Ensure Google OAuth connection is enabled
3. Verify connection name is exactly `google-oauth2`

### Issue 4: Backend Token Exchange Fails

**Problem:** Backend returns error when exchanging Auth0 tokens.

**Solution:**
1. Verify backend endpoint `/v1/auth/auth0/callback` is accessible
2. Check backend logs for specific error
3. Ensure `AUTH0_AUDIENCE` matches backend configuration
4. Verify Auth0 domain matches between Android app and backend

## Security Best Practices

1. **Never commit secrets.defaults.properties** - This file should be in `.gitignore`
2. **Use different Auth0 applications** for development and production
3. **Store tokens securely** - The app uses `EncryptedSharedPreferences` (already implemented)
4. **Validate state parameter** - Auth0 SDK handles this automatically
5. **Use HTTPS** - All API calls use HTTPS (already configured)

## Additional Resources

- [Auth0 Android SDK Documentation](https://auth0.com/docs/quickstart/native/android)
- [Auth0 OAuth 2.0 Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
- [Android Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs/)

## Next Steps

After configuring Auth0:

1. ✅ Add Auth0 credentials to `secrets.defaults.properties`
2. ✅ Rebuild the app
3. ✅ Test Google login flow
4. ✅ Verify user profile is fetched correctly
5. ✅ Test token refresh functionality
6. ✅ Test logout functionality

## Support

If you encounter issues:

1. Check the logs for specific error messages
2. Verify Auth0 Dashboard configuration
3. Ensure `secrets.defaults.properties` has correct values
4. Rebuild the app after making configuration changes
