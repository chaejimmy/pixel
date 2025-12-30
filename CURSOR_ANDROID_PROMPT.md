# Android Cursor prompt (FULL — UI/UX parity + architecture + API integration)

You are building the Android PaceDream app to match the iOS PaceDream app’s **auth UI/UX and backend behavior** exactly. The current Android “Welcome Back” card login screen is **wrong**. Replace it with the iOS-style **AuthFlowSheet** chooser → sign-in/sign-up flow and match the backend/Auth0 session behavior.

#### iOS source of truth (mirror exactly)
- **UI/UX**: `PaceDream/Views/Authentication/AuthFlowSheet.swift`
- **Email/password behavior**: `PaceDream/Services/AuthService.swift`
- **Auth0→backend JWT exchange**: `PaceDream/Services/AuthExchangeService.swift`
- **Session bootstrap + refresh**: `PaceDream/ViewModels/AuthSession.swift`
- **Backend base URL rules**: `PaceDream/App/AppConfig.swift`
- **Auth endpoints list**: `PaceDream/Models/Networking/Endpoints/AuthEndpoint.swift`

---

## 1) UI/UX parity requirements (must match iOS)

### Auth entry is a sheet (not a full-page “welcome back card”)
Implement `AuthFlowSheet` as a **modal sheet** (Material3 `ModalBottomSheet` or full-height bottom sheet). It has three internal modes and **always opens in Chooser mode**.

### Modes
**Mode A: Chooser (default on open every time)**
- Header: title (26sp bold) + subtitle (15sp regular).
- Buttons in this order:
  - Primary filled: **Sign in**
  - Secondary tinted: **Create account**
  - Divider row: `— or —`
  - Outlined: **Continue with Google** (inline spinner when loading, disabled while loading)
  - Black filled: **Continue with Apple** (inline spinner, disabled while Apple/Google loading)
  - Text button: **Not now**
- Top-right toolbar action: **Done** (same behavior as Not now)

**Mode B: Sign in**
- Fields: **Email**, **Password** only.
- Button: **Continue** (primary filled)
  - Disabled if email or password empty.
  - Inline spinner left of text when loading.
- Inline **error banner** under fields (NOT snackbar/toast).
- Link: **Create an account** → switches to Sign up with quick animation.

**Mode C: Sign up**
- Fields:
  - Row: **First name** + **Last name**
  - Then **Email**
  - Then **Password**
- Button: **Continue** (primary filled)
  - Disabled if any required field empty.
  - Inline spinner.
- Inline error banner.
- Link: **Already have an account? Sign in** → switches back with quick animation.

### Visual details (match iOS styling)
- Container content: scrollable column; padding horizontal **20dp**, top **18dp**, bottom **24dp**
- Buttons: minHeight **50dp**, corner radius **16dp**, full-width.
- Divider lines: 1dp, black @ 10% alpha.
- Error banner: padding **12dp**, radius **14dp**, error bg @ 10%, warning icon + message.
- Fields: rounded outlined style; email no autocap/no autocorrect; password masked (no toggle in this sheet).
- Strip `"Server error 200: "` prefix from displayed errors (iOS does this in social login flow).

### Interaction rules
- On open: **reset to Chooser**, clear error, clear loading flags (keep text fields or clear them—prefer clearing to match iOS “fresh start” feel).
- Mode transitions: animate ~200ms easeInOut (`AnimatedContent`/`Crossfade` tween 200).
- Auth0 cancellation by user: **no error** (silent no-op).
- On success (any method): store tokens → `SessionManager.bootstrap()` → call `onSuccess()` → dismiss.

---

## 2) Architecture requirements (what to build)

Use **Jetpack Compose + Material3** with **MVVM**, coroutines, and a clean separation:

### Modules / packages (suggested)
- `ui/auth/`  
  `AuthFlowSheet.kt`, `AuthViewModel.kt`, `AuthUiState.kt`, `AuthMode.kt`
- `data/auth/`  
  `AuthRepository.kt`, `AuthApi.kt`, `AuthModels.kt`
- `data/session/`  
  `SessionManager.kt`, `TokenStore.kt`, `UserModels.kt`
- `data/network/`  
  `ApiClient.kt` (Retrofit), `AuthInterceptor.kt`, `TokenRefreshAuthenticator.kt` (or manual retry), `Result.kt`
- `navigation/`  
  `NavGraph.kt`, `Routes.kt`
- `config/`  
  `AppConfig.kt`

### State management
Create:
- `sealed class AuthMode { Chooser, SignIn, SignUp }`
- `data class AuthUiState(...)` with:
  - `mode`
  - `email`, `password`, `firstName`, `lastName`
  - `errorMessage: String?`
  - `isSubmittingEmail: Boolean`
  - `isGoogleLoading: Boolean`, `isAppleLoading: Boolean`
- `AuthViewModel` exposes `StateFlow<AuthUiState>`.

### Navigation integration (critical)
- Do not force a “login screen” as the first screen.
- App can open to main navigation, but **gated actions** (booking, wishlist, messaging, etc.) must present `AuthFlowSheet` when unauthenticated.
- Provide a single source of truth auth state from `SessionManager.sessionState: StateFlow<SessionState>` (authenticated/unauthenticated/bootstrapping).

### Bootstrapping behavior
On app launch:
- `SessionManager.bootstrap()` loads tokens from secure storage.
- If access token exists → set authenticated immediately, then attempt fetching profile.
- If profile fetch fails (non-401) → **stay authenticated** to avoid “login loops”.

---

## 3) Backend/API behavior (must match iOS)

### Base URLs
- Backend base is **`<BACKEND_BASE_URL>/v1`**. Default host: `https://pacedream-backend.onrender.com`
- Frontend base for proxy fallback: `https://www.pacedream.com`
- Implement normalization: if configured base already ends with `/v1`, don’t double-append.

Android config:
- Provide `BACKEND_BASE_URL` and `FRONTEND_BASE_URL` via `BuildConfig` or `local.properties` / flavor.
- Ensure runtime base = `<backend>/v1`.

### Auth endpoints (prefix with `/v1/`)
From iOS `AuthEndpoint`:
- Email login: `POST /v1/auth/login/email`
- Email signup: `POST /v1/auth/signup/email`
- Auth0 exchange: `POST /v1/auth/auth0/callback`
- Refresh token: `POST /v1/auth/refresh-token` (note: this is what iOS session refresh uses)
- Refresh fallback: `POST https://www.pacedream.com/api/proxy/auth/refresh-token`

### Request/response contract and rules

**Important header rule**
- For ALL auth endpoints above: **do NOT send Authorization header**, even if a token exists.

**Email login**
- Body:
  ```json
  { "method": "email", "email": "<email>", "password": "<password>" }
  ```
- Response token extraction must be tolerant:
  - `token`, or `data.token`, or `data.data.token`, or `jwt`
- On success: store access token; refresh token may be absent (iOS sets refreshToken nil for email login/signup).

**Email signup**
- Body (match iOS defaults):
  ```json
  { "email": "<email>", "firstName": "<first>", "lastName": "<last>", "password": "<password>", "dob": "1990-01-01", "gender": "unspecified" }
  ```
- Extract token same way.

**Auth0 exchange (Google/Apple)**
- After Auth0 hosted login returns `accessToken` + `idToken`, call:
  - `POST /v1/auth/auth0/callback`
  - Body:
    ```json
    { "accessToken": "<auth0AccessToken>", "idToken": "<auth0IdToken>" }
    ```
- Expect envelope (tolerate both `success` and `status`):
  - `{ success:true, data:{ accessToken, refreshToken, user } }`
- Validate token looks like JWT (3 segments) before accepting.
- Store access+refresh.

**Session bootstrap “me”**
- On bootstrap (and after login):
  - Prefer `GET /v1/account/me`
  - Fallback `GET /v1/users/get/profile`
  - Fallback `GET /v1/user/get/profile`
- If `GET /account/me` returns 401:
  - attempt refresh once using refresh token flow below
  - retry `GET /account/me` once if refresh succeeded
  - if refresh fails → clear tokens and set unauthenticated

**Refresh token behavior**
- Request body:
  ```json
  { "refresh_token": "<refreshToken>" }
  ```
- Try in order:
  1) `POST /v1/auth/refresh-token`
  2) `POST https://www.pacedream.com/api/proxy/auth/refresh-token`
- Token fields can be variants: `accessToken`, `access_token`, `token`, or `jwt` and refresh variants similarly.

**HTML safety for proxy**
- If proxy responds with HTML (`Content-Type: text/html` or body starts with `<!doctype html` / `<html`) treat as service unavailable with friendly message.

### Token storage
- Use secure storage (EncryptedSharedPreferences + MasterKey).
- Store:
  - `accessToken`
  - `refreshToken` (nullable)
- Never store JWT in plain SharedPreferences.

---

## 4) Auth0 on Android (implementation guidance)
- Use Auth0 hosted login (Universal Login).
- For Google: specify connection **`google-oauth2`**.
- For Apple: specify connection **`apple`**.
- On cancel: treat as no-op.
- On success: exchange Auth0 tokens for backend JWT via `/v1/auth/auth0/callback`.

---

## 5) Networking implementation (Retrofit/OkHttp)
- Build Retrofit with base URL = `<backend>/v1/`.
- Implement:
  - **AuthInterceptor**: adds Bearer token for non-auth endpoints.
  - **NoAuth annotation** or separate Retrofit instance for auth endpoints to ensure no Authorization header.
  - **401 retry policy**: refresh once + retry once.
- Normalize errors into user-friendly messages; surface them via the inline banner.

---

## 6) Testing & acceptance criteria
### Acceptance criteria (must pass)
- Opening auth always shows Chooser sheet (not email/password card).
- Chooser shows: Sign in, Create account, Continue with Google, Continue with Apple, Not now, Done.
- Sign-in/up continue buttons disable correctly and show inline spinners.
- Errors show inline banner (not snackbar).
- Google/Apple cancel does not show error.
- Email login/signup calls correct endpoints with **no Authorization header**.
- Auth0 exchange stores backend JWT + refresh token and bootstraps profile.
- App remains authenticated if profile fetch fails (non-401).
- 401 triggers refresh flow and retries once; if refresh fails → logged out.

### Minimal tests to add
- Unit tests for token extraction (email login/signup + refresh variants).
- Unit tests for refresh fallback order.
- Compose previews for each mode; one screenshot test optional.

---

## 7) Implementation steps (do these in order)
1) Create `AppConfig.kt` with backend/frontend base URLs and `/v1` normalization.
2) Implement `TokenStore` (secure).
3) Implement `SessionManager` (`bootstrap`, `completeSignIn`, `signOut`, `refreshIfPossible`).
4) Implement Retrofit clients:
   - `AuthApi` (no auth header)
   - `Api` (auth header)
5) Implement `AuthRepository` methods:
   - emailLogin, emailSignup, auth0Exchange, refreshToken (backend + proxy fallback)
6) Implement `AuthViewModel` to drive `AuthFlowSheet`.
7) Replace current login UI with `AuthFlowSheet` entry and gating logic across app.

Proceed to implement now, deleting/replacing the incorrect Android login UI.

