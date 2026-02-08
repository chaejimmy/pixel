package com.shourov.apps.pacedream.core.network.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Tests for the legacy ApiResult sealed class and its extension functions.
 * Also tests wrapIntoApiResult for basic success/failure paths.
 */
class ApiResultModelTest {

    // ── ApiResult variants ──────────────────────────────────────────

    @Test
    fun `Success wraps value`() {
        val result: ApiResult<String> = ApiResult.Success("hello")
        assertTrue(result is ApiResult.Success)
        assertEquals("hello", (result as ApiResult.Success).value)
    }

    @Test
    fun `GenericError wraps throwable`() {
        val error = RuntimeException("oops")
        val result: ApiResult<String> = ApiResult.GenericError(error)
        assertTrue(result is ApiResult.GenericError)
        assertEquals("oops", (result as ApiResult.GenericError).throwable?.message)
    }

    @Test
    fun `GenericError allows null throwable`() {
        val result: ApiResult<String> = ApiResult.GenericError()
        assertNull((result as ApiResult.GenericError).throwable)
    }

    @Test
    fun `NetworkError is singleton`() {
        val result: ApiResult<String> = ApiResult.NetworkError
        assertTrue(result is ApiResult.NetworkError)
    }

    // ── toString ────────────────────────────────────────────────────

    @Test
    fun `Success toString contains value`() {
        val result = ApiResult.Success("test")
        assertTrue(result.toString().contains("test"))
    }

    @Test
    fun `GenericError toString contains error message`() {
        val result = ApiResult.GenericError(RuntimeException("fail"))
        assertTrue(result.toString().contains("fail"))
    }

    @Test
    fun `NetworkError toString returns NetworkError`() {
        assertEquals("NetworkError", ApiResult.NetworkError.toString())
    }

    // ── doIfSuccess ─────────────────────────────────────────────────

    @Test
    fun `doIfSuccess calls callback on Success`() {
        var captured = ""
        ApiResult.Success("value").doIfSuccess { captured = it }
        assertEquals("value", captured)
    }

    @Test
    fun `doIfSuccess does not call callback on GenericError`() {
        var called = false
        ApiResult.GenericError(RuntimeException()).doIfSuccess<String> { called = true }
        assertFalse(called)
    }

    @Test
    fun `doIfSuccess does not call callback on NetworkError`() {
        var called = false
        ApiResult.NetworkError.doIfSuccess<String> { called = true }
        assertFalse(called)
    }

    // ── doIfGenericError ────────────────────────────────────────────

    @Test
    fun `doIfGenericError calls callback on GenericError`() {
        var capturedMessage: String? = null
        ApiResult.GenericError(RuntimeException("err")).doIfGenericError<String> {
            capturedMessage = it?.message
        }
        assertEquals("err", capturedMessage)
    }

    @Test
    fun `doIfGenericError does not call callback on Success`() {
        var called = false
        ApiResult.Success("ok").doIfGenericError { called = true }
        assertFalse(called)
    }

    // ── doIfNetworkError ────────────────────────────────────────────

    @Test
    fun `doIfNetworkError calls callback on NetworkError`() {
        var called = false
        ApiResult.NetworkError.doIfNetworkError<String> { called = true }
        assertTrue(called)
    }

    @Test
    fun `doIfNetworkError does not call callback on Success`() {
        var called = false
        ApiResult.Success("ok").doIfNetworkError { called = true }
        assertFalse(called)
    }

    // ── List extensions ─────────────────────────────────────────────

    @Test
    fun `doIfAnyNetworkError finds NetworkError in list`() {
        var called = false
        val results: List<ApiResult<String>> = listOf(
            ApiResult.Success("ok"),
            ApiResult.NetworkError,
            ApiResult.Success("also ok")
        )
        results.doIfAnyNetworkError { called = true }
        assertTrue(called)
    }

    @Test
    fun `doIfAnyNetworkError not called when no NetworkError`() {
        var called = false
        val results: List<ApiResult<String>> = listOf(
            ApiResult.Success("ok"),
            ApiResult.GenericError(RuntimeException())
        )
        results.doIfAnyNetworkError { called = true }
        assertFalse(called)
    }

    @Test
    fun `doIfAnyGenericError finds GenericError in list`() {
        var called = false
        val results: List<ApiResult<String>> = listOf(
            ApiResult.Success("ok"),
            ApiResult.GenericError(RuntimeException("test")),
            ApiResult.Success("also ok")
        )
        results.doIfAnyGenericError { called = true }
        assertTrue(called)
    }

    @Test
    fun `doIfAnyGenericError not called when no GenericError`() {
        var called = false
        val results: List<ApiResult<String>> = listOf(
            ApiResult.Success("ok"),
            ApiResult.NetworkError
        )
        results.doIfAnyGenericError { called = true }
        assertFalse(called)
    }

    // ── wrapIntoApiResult ───────────────────────────────────────────

    @Test
    fun `wrapIntoApiResult returns Success on successful call`() = runBlocking {
        val result = wrapIntoApiResult { "hello" }
        assertTrue(result is ApiResult.Success)
        assertEquals("hello", (result as ApiResult.Success).value)
    }

    @Test
    fun `wrapIntoApiResult returns NetworkError on IOException`() = runBlocking {
        val result = wrapIntoApiResult<String> { throw IOException("no network") }
        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `wrapIntoApiResult returns GenericError on RuntimeException`() = runBlocking {
        val result = wrapIntoApiResult<String> { throw RuntimeException("generic") }
        assertTrue(result is ApiResult.GenericError)
        assertTrue((result as ApiResult.GenericError).throwable?.message?.contains("generic") == true)
    }

    @Test
    fun `wrapIntoApiResult returns GenericError on IllegalStateException`() = runBlocking {
        val result = wrapIntoApiResult<String> { throw IllegalStateException("bad state") }
        assertTrue(result is ApiResult.GenericError)
    }
}
