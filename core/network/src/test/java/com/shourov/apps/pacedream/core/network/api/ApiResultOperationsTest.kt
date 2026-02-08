package com.shourov.apps.pacedream.core.network.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for ApiResult sealed class and its utility methods.
 * Covers: map, flatMap, onSuccess, onFailure, getOrThrow, getOrDefault,
 * isSuccess, isFailure, getOrNull, errorOrNull.
 */
class ApiResultOperationsTest {

    // ── isSuccess / isFailure ───────────────────────────────────────

    @Test
    fun `Success isSuccess returns true`() {
        val result: ApiResult<String> = ApiResult.Success("hello")
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `Failure isFailure returns true`() {
        val result: ApiResult<String> = ApiResult.Failure(ApiError.NotFound())
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    // ── getOrNull / errorOrNull ─────────────────────────────────────

    @Test
    fun `getOrNull returns data on Success`() {
        val result = ApiResult.Success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Failure`() {
        val result: ApiResult<Int> = ApiResult.Failure(ApiError.Timeout())
        assertNull(result.getOrNull())
    }

    @Test
    fun `errorOrNull returns error on Failure`() {
        val error = ApiError.Forbidden()
        val result: ApiResult<Int> = ApiResult.Failure(error)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun `errorOrNull returns null on Success`() {
        val result = ApiResult.Success("data")
        assertNull(result.errorOrNull())
    }

    // ── map ─────────────────────────────────────────────────────────

    @Test
    fun `map transforms Success value`() {
        val result = ApiResult.Success(5).map { it * 2 }
        assertTrue(result is ApiResult.Success)
        assertEquals(10, (result as ApiResult.Success).data)
    }

    @Test
    fun `map preserves Failure`() {
        val error = ApiError.Timeout()
        val result: ApiResult<Int> = ApiResult.Failure(error)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is ApiResult.Failure)
        assertEquals(error, (mapped as ApiResult.Failure).error)
    }

    // ── flatMap ─────────────────────────────────────────────────────

    @Test
    fun `flatMap chains Success`() {
        val result = ApiResult.Success(10).flatMap { ApiResult.Success(it + 5) }
        assertTrue(result is ApiResult.Success)
        assertEquals(15, (result as ApiResult.Success).data)
    }

    @Test
    fun `flatMap chains Success to Failure`() {
        val result = ApiResult.Success(10).flatMap<Int> {
            ApiResult.Failure(ApiError.ServerError("fail"))
        }
        assertTrue(result is ApiResult.Failure)
    }

    @Test
    fun `flatMap preserves Failure`() {
        val error = ApiError.NetworkError()
        val result: ApiResult<Int> = ApiResult.Failure(error)
        val chained = result.flatMap { ApiResult.Success(it + 1) }
        assertTrue(chained is ApiResult.Failure)
        assertEquals(error, (chained as ApiResult.Failure).error)
    }

    // ── onSuccess / onFailure ───────────────────────────────────────

    @Test
    fun `onSuccess runs action on Success`() {
        var captured = 0
        ApiResult.Success(42).onSuccess { captured = it }
        assertEquals(42, captured)
    }

    @Test
    fun `onSuccess skips action on Failure`() {
        var called = false
        val result: ApiResult<Int> = ApiResult.Failure(ApiError.Timeout())
        result.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onFailure runs action on Failure`() {
        var capturedError: ApiError? = null
        val error = ApiError.Unauthorized("bad token")
        val result: ApiResult<Int> = ApiResult.Failure(error)
        result.onFailure { capturedError = it }
        assertEquals(error, capturedError)
    }

    @Test
    fun `onFailure skips action on Success`() {
        var called = false
        ApiResult.Success("data").onFailure { called = true }
        assertFalse(called)
    }

    // ── getOrThrow ──────────────────────────────────────────────────

    @Test
    fun `getOrThrow returns data on Success`() {
        val result = ApiResult.Success("value")
        assertEquals("value", result.getOrThrow())
    }

    @Test(expected = ApiError.NotFound::class)
    fun `getOrThrow throws error on Failure`() {
        val result: ApiResult<String> = ApiResult.Failure(ApiError.NotFound())
        result.getOrThrow()
    }

    // ── getOrDefault ────────────────────────────────────────────────

    @Test
    fun `getOrDefault returns data on Success`() {
        val result = ApiResult.Success(99)
        assertEquals(99, result.getOrDefault(0))
    }

    @Test
    fun `getOrDefault returns default on Failure`() {
        val result: ApiResult<Int> = ApiResult.Failure(ApiError.Timeout())
        assertEquals(0, result.getOrDefault(0))
    }

    // ── chaining combinations ───────────────────────────────────────

    @Test
    fun `map then onSuccess chain works`() {
        var capturedValue = ""
        ApiResult.Success("hello")
            .map { it.uppercase() }
            .onSuccess { capturedValue = it }
        assertEquals("HELLO", capturedValue)
    }

    @Test
    fun `onFailure returns same result for chaining`() {
        val original: ApiResult<String> = ApiResult.Failure(ApiError.Timeout())
        var errorSeen = false
        val chained = original.onFailure { errorSeen = true }
        assertTrue(errorSeen)
        assertTrue(chained is ApiResult.Failure)
    }
}
