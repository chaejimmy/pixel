package com.shourov.apps.pacedream.core.common.featureflags

import com.shourov.apps.pacedream.core.common.BuildConfig

/**
 * Compile-time feature gates for in-progress features. Anything that
 * would otherwise ship as a half-built screen with a TODO `onClick`
 * lives behind a flag here and is hidden from navigation when the flag
 * is `false`. Each flag should be flipped on by a real PR that wires
 * the full UI / backend, not by a silent default change.
 *
 * Most flags are `const val` so dead branches are stripped at compile
 * time. Flags that need to differ between debug and release builds
 * read from `BuildConfig` (a plain `val`, but `BuildConfig` fields are
 * `public static final` so R8 still strips the dead branches in the
 * release variant). If we need server-driven flags later, swap
 * individual properties to a `FeatureFlagsSource` injected via Hilt —
 * call sites stay the same because they read `FeatureFlags.<name>`
 * regardless.
 *
 * Audit references:
 *   - `ANDROID_AUDIT_REPORT.md` Finding 4 (Roommate Finder)
 *   - `ANDROID_AUDIT_REPORT.md` Finding 5 (Trip Planner)
 *   - `INTERACTION_AUDIT_REPORT.md` C-09 / C-10 / H-10 (Roommate)
 */
object FeatureFlags {

    /**
     * Roommate Finder feature — search, post listing, advanced filters.
     * The 2026-04-10 audit (Finding 4) found this screen had only a
     * static UI shell with TODO comments on every primary CTA, so it
     * was removed from the build (commit `49503db`, C-09/10).
     *
     * Backed by `BuildConfig.FEATURE_ROOMMATE_FINDER`: `false` in
     * release builds (the route stays hidden from Play Store users)
     * and `true` in debug builds (so a re-introduction can be wired
     * up and tested locally without flipping a default). The flag must
     * stay off in release until a real implementation PR ships the
     * full UI + backend wiring.
     */
    val ROOMMATE_FINDER: Boolean = BuildConfig.FEATURE_ROOMMATE_FINDER

    /**
     * Trip Planner — multi-stop trip composition with a backend.
     * Currently no backend integration (`ANDROID_AUDIT_REPORT.md` § Finding 5),
     * so callers should gate the entry on this flag and hide the nav
     * item until the integration lands.
     */
    const val TRIP_PLANNER: Boolean = false

    /**
     * Chat call / video-call buttons in `ChatScreen` header
     * (`INTERACTION_AUDIT_REPORT.md` H-07). Off until real call routing
     * exists; with this `false` the chat header should not render the
     * icons at all.
     */
    const val CHAT_VOICE_VIDEO: Boolean = false

    /**
     * Multi-field WHAT / WHERE / DATES search with a real date picker
     * and a "Use my location" button (`UI_UX_COMPARISON.md` § 2). The
     * Use / Borrow / Split *labels* are already on the segmented tabs
     * — this flag tracks the bigger structural rebuild of the search
     * card itself, which is not yet implemented. Off by default until
     * the multi-field UI + location flow lands.
     */
    const val MULTI_FIELD_SEARCH: Boolean = false

    /**
     * Send the `Idempotency-Key` header on the webflow booking-creation
     * endpoints (`POST /v1/properties/bookings/timebased`,
     * `POST /v1/gear-rentals/book`).  Persisted across process death so
     * a retry or relaunch reuses the same key and the backend can return
     * the existing booking + checkout url instead of creating a duplicate.
     *
     * REQUIRES backend support: server must dedupe by
     * `(authenticated_user, Idempotency-Key)` for at least 24h and
     * return the same `checkoutUrl` for the same key.  Until backend
     * confirms, leave this `false` — the header is silently dropped by
     * servers that do not honour it, but flipping it on without backend
     * support would give a false sense of security.
     *
     * Audit reference: `claude/audit-stripe-payments` Phase 0, F-02.
     */
    const val WEBFLOW_IDEMPOTENCY_KEY: Boolean = false
}
