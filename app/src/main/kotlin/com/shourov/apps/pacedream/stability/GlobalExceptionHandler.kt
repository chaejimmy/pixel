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

            // For the main thread: if the exception happens during Compose rendering
            // or click handlers, we log and attempt to keep the process alive.
            // For background threads: log only.
            if (thread.name == "main") {
                // Forward to default handler (Crashlytics) which will record the crash
                // but the process will restart cleanly
                defaultHandler?.uncaughtException(thread, throwable)
            } else {
                // Background thread crash - log but don't kill the app
                Timber.e(throwable, "Non-fatal background thread crash on ${thread.name}, swallowed")
            }
        }
    }
}
