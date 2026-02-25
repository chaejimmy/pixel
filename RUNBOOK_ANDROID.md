# PaceDream Android — Release Runbook

**Date:** 2026-02-25
**Version:** 0.1.2 (versionCode 8)

---

## Prerequisites

### Required Secrets (must be set before building)

| Key | Location | Description |
|-----|----------|-------------|
| `auth0ClientId` | `local.properties` or CI env | Auth0 application client ID |
| `stripePublishableKey` | `local.properties` or CI env | Stripe publishable API key |
| Release keystore | `keystore.properties` | Play Store signing keystore (not debug) |
| `google-services.json` | `app/` | Firebase project config (per flavor) |
| Google Maps API key | `app/src/main/res/values/strings.xml` or Secrets Gradle Plugin | Maps SDK key |

### Required Infrastructure

- [ ] Firebase project configured (Crashlytics, Messaging, Analytics)
- [ ] `https://pacedream.com/.well-known/assetlinks.json` deployed for App Links
- [ ] Backend API `/v1/notifications/register-device` endpoint active
- [ ] Auth0 application configured with Android callback URLs:
  - `pacedream://callback`
  - `com.pacedream.app://auth0`

---

## Build Steps

### 1. Configure local.properties

```properties
# Add to local.properties (NOT committed to git)
auth0ClientId=<your-auth0-client-id>
stripePublishableKey=<your-stripe-publishable-key>
```

### 2. Build Release APK / AAB

```bash
# Clean build
./gradlew clean

# Build release AAB (for Play Store)
./gradlew :app:bundleProdRelease

# Output: app/build/outputs/bundle/prodRelease/app-prod-release.aab

# Build release APK (for testing)
./gradlew :app:assembleProdRelease

# Output: app/build/outputs/apk/prod/release/app-prod-release.apk
```

### 3. Verify Release Build

```bash
# Run unit tests
./gradlew :app:testProdReleaseUnitTest

# Verify dependency guard (classpath hasn't changed unexpectedly)
./gradlew :app:dependencyGuard

# Check baseline profile is included
./gradlew :app:generateProdReleaseBaselineProfile
```

### 4. Sign the Release (when release keystore is configured)

```bash
# If using Gradle signing config (recommended):
# The build.gradle.kts release block should reference the release keystore.

# If signing manually:
jarsigner -keystore release.keystore \
  -storepass <password> \
  app-prod-release.aab <alias>

# Verify signing
jarsigner -verify app-prod-release.aab
```

---

## Play Store Upload

### Internal Testing Track

1. Go to [Google Play Console](https://play.google.com/console)
2. Select **PaceDream** app
3. Navigate to **Testing → Internal testing**
4. Click **Create new release**
5. Upload `app-prod-release.aab`
6. Add release notes:
   ```
   v0.1.2 (8) — Internal test build
   - Auth0 login (Google, email/password)
   - Property browsing and search
   - Booking flow with Stripe checkout
   - Push notifications (messages, bookings)
   - Wishlist and inbox/chat
   - Host mode (preview)
   ```
7. Click **Review release** → **Start rollout to Internal testing**

### Closed Testing Track (Alpha)

1. After internal testing pass, promote to Closed testing
2. Add external testers group
3. Ensure Data Safety form is completed
4. Ensure content rating questionnaire is submitted

### Production Release

1. After closed testing pass, promote to Production
2. Set staged rollout percentage (recommend 10% → 25% → 50% → 100%)
3. Monitor Crashlytics dashboard for 24 hours at each stage

---

## Version Bumping

Update in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 9           // Increment by 1 for each release
    versionName = "0.1.3"     // Follow semver: X.Y.Z
}
```

**Convention:**
- `Z` (patch): Bug fixes, minor tweaks
- `Y` (minor): New features, non-breaking changes
- `X` (major): Breaking changes, major redesigns

---

## Rollback Procedure

### If critical bug found after Play Store release:

#### Option A — Halt Rollout (Preferred)
1. Go to Play Console → **Releases overview**
2. Click **Halt rollout** on the affected release
3. Users who haven't updated keep the previous version
4. Fix the bug, bump versionCode, upload new AAB
5. Resume rollout with the fixed version

#### Option B — Emergency Rollback
1. Go to Play Console → **Releases overview**
2. Click **Rollback** to the previous version
3. This only works if the previous version is still available
4. Users will receive the rollback as an "update"

#### Option C — Server-Side Kill Switch
1. Add a force-update check API endpoint (recommended for future)
2. Return minimum required version from backend
3. App shows blocking dialog if current version < minimum

### If crash is in release build (R8 related):
1. Check Crashlytics for deobfuscated stack trace
2. If mapping file wasn't uploaded, build locally with same commit:
   ```bash
   git checkout <release-tag>
   ./gradlew :app:assembleProdRelease
   # Mapping file: app/build/outputs/mapping/prodRelease/mapping.txt
   ```
3. Upload mapping to Firebase Console → Crashlytics → Mapping files

---

## Monitoring

### Post-Release Checklist

| Check | Tool | Frequency |
|-------|------|-----------|
| Crash-free rate > 99.5% | Firebase Crashlytics | Daily for 7 days |
| ANR rate < 0.5% | Play Console Vitals | Daily for 7 days |
| Push delivery rate | Firebase Cloud Messaging | Daily for 3 days |
| Auth success rate | Backend logs | Daily for 3 days |
| API error rate | Backend monitoring | Daily for 7 days |
| User reviews/ratings | Play Console | Daily for 14 days |
| Uninstall rate | Play Console | Weekly |

### Key Crashlytics Alerts to Configure

1. **New crash type** — alert immediately
2. **Crash-free rate drops below 99%** — alert immediately
3. **Velocity alert** — crash affects > 1% of sessions in 1 hour

---

## Hotfix Process

1. Branch from the release tag: `git checkout -b hotfix/0.1.3 v0.1.2`
2. Apply minimal fix
3. Bump versionCode (+1) and versionName (patch)
4. Build and test release APK locally
5. Upload to internal testing → verify fix
6. Promote to production with 100% rollout (hotfixes skip staged rollout)
7. Merge hotfix branch back to main

---

## CI/CD Pipeline (Recommended)

```yaml
# Suggested GitHub Actions workflow
name: Android Release

on:
  push:
    tags: ['v*']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > release.keystore

      - name: Build release AAB
        run: ./gradlew :app:bundleProdRelease
        env:
          auth0ClientId: ${{ secrets.AUTH0_CLIENT_ID }}
          stripePublishableKey: ${{ secrets.STRIPE_PUBLISHABLE_KEY }}

      - name: Run tests
        run: ./gradlew :app:testProdReleaseUnitTest

      - name: Upload to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT }}
          packageName: com.shourov.apps.pacedream
          releaseFiles: app/build/outputs/bundle/prodRelease/*.aab
          track: internal
```

---

## Environment Reference

| Environment | API Base URL | Auth0 Domain |
|-------------|-------------|--------------|
| Production | `https://pacedream-backend.onrender.com/v1/` | `pacedream.us.auth0.com` |
| Development | `https://pacedream-backend.onrender.com/v1/` | `dev-pacedream.us.auth0.com` |

---

## Contact

| Role | Contact |
|------|---------|
| Android Lead | — |
| Backend Lead | — |
| DevOps | — |
| Firebase Admin | — |
