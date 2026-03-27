package com.shourov.apps.pacedream.stability

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import timber.log.Timber

/**
 * Centralized crash-safety utilities for PaceDream.
 * Provides safe wrappers for navigation, click handlers, and error handling
 * to ensure the app never crashes from common failure modes.
 */

// ---------------------------------------------------------------------------
// 1. Click debouncing – prevents double-tap crashes and duplicate navigation
// ---------------------------------------------------------------------------

private const val DEBOUNCE_INTERVAL_MS = 400L

/**
 * Returns a click handler that ignores rapid successive clicks.
 * Use this for all navigation-triggering or action-triggering buttons.
 */
@Composable
fun debouncedClickHandler(onClick: () -> Unit): () -> Unit {
    val lastClickTime = remember { ClickTimeHolder() }
    return {
        val now = System.currentTimeMillis()
        if (now - lastClickTime.value > DEBOUNCE_INTERVAL_MS) {
            lastClickTime.value = now
            try {
                onClick()
            } catch (e: Exception) {
                Timber.e(e, "Debounced click handler caught exception")
            }
        }
    }
}

private class ClickTimeHolder(var value: Long = 0L)

/**
 * Modifier extension for debounced clicks.
 */
@Composable
fun Modifier.debouncedClickable(
    onClick: () -> Unit,
): Modifier {
    val handler = debouncedClickHandler(onClick)
    return this.clickable(onClick = handler)
}

// ---------------------------------------------------------------------------
// 2. Safe navigation – catches IllegalArgumentException from invalid routes
// ---------------------------------------------------------------------------

/**
 * Navigate safely, catching any navigation exceptions.
 * Returns true if navigation succeeded, false otherwise.
 */
fun NavController.safeNavigate(route: String, builder: (androidx.navigation.NavOptionsBuilder.() -> Unit)? = null): Boolean {
    return try {
        if (builder != null) {
            navigate(route, builder)
        } else {
            navigate(route)
        }
        true
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Safe navigation failed for route: $route")
        false
    } catch (e: IllegalStateException) {
        Timber.e(e, "Safe navigation failed (illegal state) for route: $route")
        false
    } catch (e: Exception) {
        Timber.e(e, "Safe navigation failed (unexpected) for route: $route")
        false
    }
}

/**
 * Pop back stack safely, catching any exceptions.
 */
fun NavController.safePopBackStack(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        Timber.e(e, "Safe popBackStack failed")
        false
    }
}

// ---------------------------------------------------------------------------
// 3. Safe coroutine execution
// ---------------------------------------------------------------------------

/**
 * Execute a suspending block safely, returning null on failure.
 */
suspend fun <T> safeSuspend(tag: String = "safeSuspend", block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e // never swallow cancellation
    } catch (e: Exception) {
        Timber.e(e, "safeSuspend [$tag] caught exception")
        null
    }
}

/**
 * Execute a block safely, returning null on failure.
 */
inline fun <T> safeCall(tag: String = "safeCall", block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        Timber.e(e, "safeCall [$tag] caught exception")
        null
    }
}

// ---------------------------------------------------------------------------
// 4. Null-safe display helpers
// ---------------------------------------------------------------------------

/** Returns the string if non-null and non-blank, otherwise the fallback. */
fun String?.orFallback(fallback: String = ""): String =
    if (this.isNullOrBlank()) fallback else this

/** Returns the string if non-null and non-blank, otherwise null. */
fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

/** Returns the list if non-null and non-empty, otherwise an empty list. */
fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/** Safe image URL: returns null for blank or obviously invalid URLs. */
fun String?.asSafeImageUrl(): String? =
    this?.takeIf { it.isNotBlank() && (it.startsWith("http") || it.startsWith("content://")) }

/** Safe price display: returns formatted price or fallback. */
fun Double?.formatPrice(currencySymbol: String = "$", fallback: String = ""): String {
    if (this == null) return fallback
    return if (this == this.toLong().toDouble()) {
        "$currencySymbol${this.toLong()}"
    } else {
        "$currencySymbol${"%.2f".format(this)}"
    }
}

// ---------------------------------------------------------------------------
// 5. Safe intent/extras helpers
// ---------------------------------------------------------------------------

/** Safely get a string from a bundle, returning empty string if missing. */
fun android.os.Bundle?.safeString(key: String, default: String = ""): String =
    try { this?.getString(key, default) ?: default } catch (_: Exception) { default }

/** Safely get an int from a bundle. */
fun android.os.Bundle?.safeInt(key: String, default: Int = 0): Int =
    try { this?.getInt(key, default) ?: default } catch (_: Exception) { default }
