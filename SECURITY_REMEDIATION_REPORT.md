# Security Remediation Report — pixel (PaceDream Android)

**Date:** 2026-03-18
**Repository:** chaejimmy/pixel
**Audit Scope:** All source code, Gradle configs, properties files, docs

---

## Summary

The pixel (Android) repository had Auth0 development credentials hardcoded as fallback defaults across multiple build config and source files. These have been replaced with empty/placeholder values.

---

## Findings & Changes

### 1. Auth0 Credentials in secrets.defaults.properties
- **File:** `secrets.defaults.properties`
- **Issue:** Real Auth0 domain (`dev-ygmeh25wmmszid8u.us.auth0.com`), client ID (`3DCwI5MfeTuL0SETFnNEGoRmRyJGpEDp`), and audience URL
- **Action:** Replaced with safe placeholders (`your-auth0-domain.auth0.com`, `your-auth0-client-id`)

### 2. Auth0 Hardcoded Defaults in AppConfig.kt (app module)
- **File:** `app/src/main/kotlin/com/pacedream/app/core/config/AppConfig.kt`
- **Issue:** `DEFAULT_AUTH0_DOMAIN` and `DEFAULT_AUTH0_CLIENT_ID` constants with real values
- **Action:** Replaced with empty strings; added comment to set via `local.properties` or CI

### 3. Auth0 Hardcoded Defaults in AppConfig.kt (core/network module)
- **File:** `core/network/src/main/java/com/shourov/apps/pacedream/core/network/config/AppConfig.kt`
- **Issue:** Same hardcoded Auth0 credentials as defaults
- **Action:** Replaced with empty strings

### 4. Auth0 Fallbacks in app/build.gradle.kts
- **File:** `app/build.gradle.kts`
- **Issue:** Hardcoded Auth0 domain in `manifestPlaceholders` and client ID in `buildConfigField` fallbacks
- **Action:** Replaced fallback values with empty strings

### 5. Auth0 Fallbacks in core/network/build.gradle.kts
- **File:** `core/network/build.gradle.kts`
- **Issue:** Hardcoded Auth0 credentials as fallback defaults
- **Action:** Replaced fallback values with empty strings

### 6. .gitignore Improvements
- **File:** `.gitignore`
- **Action:** Added rules for `secrets.properties`, `secrets.local.properties`, `.env` files

---

## Secrets That Must Be Rotated Outside the Repo

| Secret | Service | Action Required |
|--------|---------|-----------------|
| Auth0 Client ID `3DCwI5MfeTuL0SETFnNEGoRmRyJGpEDp` | Auth0 | Rotate in Auth0 dashboard — this was exposed in git history |
| Auth0 Dev Domain `dev-ygmeh25wmmszid8u.us.auth0.com` | Auth0 | Review tenant security settings |

---

## Positive Findings (Already Secure)

- `local.properties` is properly `.gitignore`d
- `google-services.json` is properly `.gitignore`d
- No Stripe secret keys found in source
- No Firebase service account keys found
- Google Maps API key field is empty (correctly left blank)
- Keystore files (`*.keystore`, `*.jks`) are `.gitignore`d

---

## Developer Notes

After this remediation, developers must supply Auth0 credentials via:
1. `local.properties` (for local development)
2. `secrets.defaults.properties` (populated locally, not committed)
3. CI/CD environment variables (for builds)

The app will not authenticate until valid Auth0 values are provided.
