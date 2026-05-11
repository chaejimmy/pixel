package com.shourov.apps.pacedream.stability

import android.os.Looper
import androidx.annotation.VisibleForTesting
import timber.log.Timber

/**
 * Global uncaught exception handler that logs uncaught exceptions and then
 * forwards them to the default handler (which routes the crash to Crashlytics
 * and lets the OS show its "app has stopped" dialog).
 *
 * We intentionally do NOT swallow main-thread exceptions: a half-rendered /
 * frozen UI is worse than a clean crash, and swallowing breaks Crashlytics
 * reporting for non-fatal Compose/NPE/IllegalState crashes — which masks real
 * bugs in production and was a Google Play stability risk (P0 in
 * `ANDROID_AUDIT_REPORT.md` § Finding 1).
 *
 * Background-thread exceptions are additionally recorded as Crashlytics
 * **non-fatals** via [NonFatalReporter] before being forwarded, so they show
 * up in a separate bucket from auto-recorded fatal main-thread crashes.
 *
 * Install in `Application.onCreate()` via [install].
 */
object GlobalExceptionHandler {

    /**
     * Records a throwable to Crashlytics (or any analytics target) as a
     * non-fatal. Injected so unit tests can substitute a fake.
     */
    fun interface NonFatalReporter {
        fun record(throwable: Throwable)
    }

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var mainThread: Thread? = null

    @VisibleForTesting
    internal var nonFatalReporter: NonFatalReporter = CrashlyticsNonFatalReporter

    /**
     * Installs the handler. Captures the current default handler and the
     * Android main thread so we can differentiate background crashes later.
     */
    fun install() {
        installInternal(Looper.getMainLooper().thread)
    }

    /**
     * Test-only entry point that accepts an explicit main thread so the
     * handler can be exercised on the JVM without an Android Looper.
     */
    @VisibleForTesting
    internal fun installInternal(mainThread: Thread) {
        if (defaultHandler != null) return // already installed
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        this.mainThread = mainThread

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Always log first so we keep a trace even if the default handler
            // terminates the process immediately.
            Timber.e(throwable, "Uncaught exception on thread ${thread.name}")

            // Background-thread crash: also file a non-fatal so the report
            // is distinguishable from the auto-recorded fatal crash that
            // the OS will produce once we forward. The check is reference
            // equality against the captured main thread (cheaper and Looper-
            // free in unit tests).
            if (thread !== this.mainThread) {
                runCatching { nonFatalReporter.record(throwable) }
            }

            // Always forward to the default handler. This is what routes the
            // crash to Crashlytics (as a fatal) and lets the OS terminate
            // the process cleanly.
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Restores the previously captured default handler. Only intended for
     * unit tests — production code installs once and leaves the handler
     * in place for the lifetime of the process.
     */
    @VisibleForTesting
    internal fun resetForTesting() {
        defaultHandler?.let { Thread.setDefaultUncaughtExceptionHandler(it) }
        defaultHandler = null
        mainThread = null
        nonFatalReporter = CrashlyticsNonFatalReporter
    }
}

/**
 * Production [GlobalExceptionHandler.NonFatalReporter] that forwards to
 * Firebase Crashlytics. Tolerates Firebase not being initialised (e.g., a
 * fresh-clone CI build with no `google-services.json`).
 */
private object CrashlyticsNonFatalReporter : GlobalExceptionHandler.NonFatalReporter {
    override fun record(throwable: Throwable) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics
                .getInstance()
                .recordException(throwable)
        } catch (_: Throwable) {
            // Firebase may not be initialised (no google-services.json) — swallow.
        }
    }
}
