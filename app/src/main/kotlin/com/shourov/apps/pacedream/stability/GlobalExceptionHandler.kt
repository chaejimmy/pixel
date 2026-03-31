package com.shourov.apps.pacedream.stability

import timber.log.Timber

/**
 * Global uncaught exception handler that logs and prevents app crashes
 * for non-fatal errors. Fatal errors (OOM, StackOverflow) are still
 * forwarded to the default handler (which sends to Crashlytics).
 *
 * Install in Application.onCreate() via GlobalExceptionHandler.install().
 */
object GlobalExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    /**
     * Installs a wrapper around the default uncaught exception handler.
     * Non-fatal UI exceptions are logged but do not kill the process.
     * Truly fatal errors (OOM, StackOverflow, etc.) are still forwarded.
     */
    fun install() {
        if (defaultHandler != null) return // already installed

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Always log
            Timber.e(throwable, "Uncaught exception on thread ${thread.name}")

            // Fatal errors that we should NOT swallow
            if (throwable is OutOfMemoryError ||
                throwable is StackOverflowError ||
                throwable is VirtualMachineError
            ) {
                defaultHandler?.uncaughtException(thread, throwable)
                return@setDefaultUncaughtExceptionHandler
            }

            // Background thread crashes: log but don't kill the app.
            // Main thread crashes: also attempt to swallow to prevent "app has a bug"
            // system dialog. Fatal errors (OOM etc.) are already forwarded above.
            if (thread.name == "main") {
                Timber.e(throwable, "Non-fatal main thread crash, swallowed to prevent app termination")
                // Do NOT forward to default handler - this prevents the system
                // "app has a bug" dialog and force-close. The app may be in a
                // partially broken state but won't hard-crash for the user.
            } else {
                Timber.e(throwable, "Non-fatal background thread crash on ${thread.name}, swallowed")
            }
        }
    }
}
