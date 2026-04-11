package com.shourov.apps.pacedream.stability

import timber.log.Timber

/**
 * Global uncaught exception handler that logs uncaught exceptions and then
 * forwards them to the default handler (which in turn lets Crashlytics
 * record the crash and lets the system show its "app has stopped" dialog).
 *
 * We intentionally do NOT swallow main-thread exceptions: a half-rendered /
 * frozen UI is worse than a clean crash, and swallowing breaks Crashlytics
 * reporting for non-fatal Compose/NPE/IllegalState crashes — which masks real
 * bugs in production and was a Google Play stability risk.
 *
 * Install in Application.onCreate() via GlobalExceptionHandler.install().
 */
object GlobalExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * Installs a thin logging wrapper around the default uncaught exception
     * handler. All exceptions are logged via Timber and then forwarded to the
     * default handler so Crashlytics and the OS can do their job.
     */
    fun install() {
        if (defaultHandler != null) return // already installed

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Always log first so we have a trace even if the default handler
            // decides to terminate the process immediately.
            Timber.e(throwable, "Uncaught exception on thread ${thread.name}")

            // Always forward to the default handler. This is what routes the
            // crash to Crashlytics and lets the OS show its standard dialog /
            // terminate the process cleanly.
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
