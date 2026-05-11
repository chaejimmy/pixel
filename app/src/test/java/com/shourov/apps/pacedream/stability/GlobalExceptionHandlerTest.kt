package com.shourov.apps.pacedream.stability

import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Proves the audit's P0 ship-blocker (`ANDROID_AUDIT_REPORT.md` § Finding 1)
 * stays fixed: an unhandled `RuntimeException` MUST reach the previously
 * installed default `Thread.UncaughtExceptionHandler`, otherwise Crashlytics
 * stops receiving fatal reports and the OS can't show its crash dialog.
 *
 * Also covers the non-fatal differentiation: background-thread crashes are
 * additionally reported via [GlobalExceptionHandler.NonFatalReporter], while
 * main-thread crashes are not (they'll be auto-recorded as fatals by the
 * default handler that the OS routes to Crashlytics).
 */
class GlobalExceptionHandlerTest {

    private lateinit var preExistingHandler: Thread.UncaughtExceptionHandler
    private lateinit var capturedDefault: RecordingHandler
    private lateinit var capturedReporter: RecordingReporter

    @Before
    fun setUp() {
        // Capture whatever JUnit/Gradle installed so we can restore it.
        preExistingHandler = Thread.getDefaultUncaughtExceptionHandler()
            ?: Thread.UncaughtExceptionHandler { _, _ -> /* no-op */ }

        capturedDefault = RecordingHandler()
        Thread.setDefaultUncaughtExceptionHandler(capturedDefault)

        capturedReporter = RecordingReporter()
        GlobalExceptionHandler.nonFatalReporter = capturedReporter
    }

    @After
    fun tearDown() {
        GlobalExceptionHandler.resetForTesting()
        Thread.setDefaultUncaughtExceptionHandler(preExistingHandler)
    }

    @Test
    fun `main-thread RuntimeException reaches the default handler`() {
        val mainThread = Thread.currentThread()
        GlobalExceptionHandler.installInternal(mainThread)

        val installed = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull("install() must set a default uncaught handler", installed)

        val crash = RuntimeException("boom on main")
        installed!!.uncaughtException(mainThread, crash)

        // The crash must have been forwarded to the previously installed
        // handler — anything less would be the audited "swallowing" bug.
        assertSame("default handler did not receive the throwable", crash, capturedDefault.lastThrowable.get())
        assertSame("default handler did not receive the thread", mainThread, capturedDefault.lastThread.get())

        // Main-thread crashes are not double-reported as non-fatals.
        assertNull(
            "main-thread crash must NOT be reported as a Crashlytics non-fatal",
            capturedReporter.lastThrowable.get()
        )
    }

    @Test
    fun `background-thread RuntimeException reaches default handler AND records non-fatal`() {
        val mainThread = Thread.currentThread()
        GlobalExceptionHandler.installInternal(mainThread)

        val installed = Thread.getDefaultUncaughtExceptionHandler()!!
        val workerThread = Thread({ /* no-op */ }, "test-worker")
        val crash = RuntimeException("boom on background")

        installed.uncaughtException(workerThread, crash)

        // Background-thread crashes still terminate the process via the
        // default handler — we only differentiate the Crashlytics tier.
        assertSame("default handler did not receive the throwable", crash, capturedDefault.lastThrowable.get())
        assertSame("default handler did not receive the thread", workerThread, capturedDefault.lastThread.get())

        // Non-fatal bucket should receive it so background coroutine crashes
        // are visible separately from auto-recorded fatal main-thread crashes.
        assertSame(
            "background-thread crash should be reported as a Crashlytics non-fatal",
            crash,
            capturedReporter.lastThrowable.get()
        )
    }

    @Test
    fun `install is idempotent`() {
        val mainThread = Thread.currentThread()
        GlobalExceptionHandler.installInternal(mainThread)
        val firstHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Second install() must be a no-op: if it captured the handler we
        // just installed as the new "default", we'd recurse / lose the
        // original chain.
        GlobalExceptionHandler.installInternal(mainThread)
        val secondHandler = Thread.getDefaultUncaughtExceptionHandler()

        assertSame("install() must be idempotent", firstHandler, secondHandler)

        // Throwables still reach the originally captured default handler.
        val crash = RuntimeException("boom")
        secondHandler!!.uncaughtException(mainThread, crash)
        assertSame(crash, capturedDefault.lastThrowable.get())
    }

    @Test
    fun `non-fatal reporter exception does not block default handler forwarding`() {
        val mainThread = Thread.currentThread()
        GlobalExceptionHandler.nonFatalReporter = GlobalExceptionHandler.NonFatalReporter {
            throw IllegalStateException("Crashlytics unavailable")
        }
        GlobalExceptionHandler.installInternal(mainThread)

        val workerThread = Thread({ /* no-op */ }, "test-worker")
        val crash = RuntimeException("boom on background")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(workerThread, crash)

        // Even if the reporter throws, the default handler must still see the
        // original crash — otherwise a broken Crashlytics integration would
        // silently swallow every background crash.
        assertSame(crash, capturedDefault.lastThrowable.get())
    }

    private class RecordingHandler : Thread.UncaughtExceptionHandler {
        val lastThread = AtomicReference<Thread?>()
        val lastThrowable = AtomicReference<Throwable?>()

        override fun uncaughtException(t: Thread, e: Throwable) {
            lastThread.set(t)
            lastThrowable.set(e)
        }
    }

    private class RecordingReporter : GlobalExceptionHandler.NonFatalReporter {
        val lastThrowable = AtomicReference<Throwable?>()

        override fun record(throwable: Throwable) {
            lastThrowable.set(throwable)
        }
    }
}
