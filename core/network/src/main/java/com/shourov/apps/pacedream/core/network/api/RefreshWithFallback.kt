package com.shourov.apps.pacedream.core.network.api

/**
 * Shared helper to enforce iOS-parity refresh behavior:
 * - Try primary refresh endpoint first
 * - If it fails (no body or can't parse/store tokens), try fallback (frontend proxy) once
 */
internal suspend fun refreshWithFallback(
    primaryCall: suspend () -> String?,
    fallbackCall: suspend () -> String?,
    parseAndStore: (String) -> Boolean
): Boolean {
    val primaryBody = primaryCall()
    if (primaryBody != null && parseAndStore(primaryBody)) return true

    val fallbackBody = fallbackCall()
    return fallbackBody != null && parseAndStore(fallbackBody)
}

