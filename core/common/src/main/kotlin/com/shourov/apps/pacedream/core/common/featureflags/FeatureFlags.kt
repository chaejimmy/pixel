package com.shourov.apps.pacedream.core.common.featureflags

/**
 * Compile-time feature gates for in-progress features. Anything that
 * would otherwise ship as a half-built screen with a TODO `onClick`
 * lives behind a flag here and is hidden from navigation when the flag
 * is `false`. Each flag should be flipped on by a real PR that wires
 * the full UI / backend, not by a silent default change.
 *
 * Today these are `const val` so dead branches are stripped at compile
 * time. If we need server-driven flags later, swap individual
 * properties to a `FeatureFlagsSource` injected via Hilt — call sites
 * stay the same because they read `FeatureFlags.<name>` regardless.
 *
 * Audit references:
 *   - `ANDROID_AUDIT_REPORT.md` Finding 4 (Roommate Finder)
 *   - `ANDROID_AUDIT_REPORT.md` Finding 5 (Trip Planner)
 *   - `INTERACTION_AUDIT_REPORT.md` C-09 / C-10 / H-10 (Roommate)
 */
object FeatureFlags {

    /**
     * Roommate Finder feature — search, post listing, advanced filters.
     * The 2026-04-10 audit found this screen had only a static UI shell
     * with TODO comments on every primary CTA. The screen has since
     * been removed from the build entirely, so this flag is `false` and
     * any future re-introduction must flip it to `true` from a real
     * implementation PR rather than leaving a half-wired screen behind.
     */
    const val ROOMMATE_FINDER: Boolean = false

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
}
