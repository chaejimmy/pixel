package com.shourov.apps.pacedream.core.network.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for AuthSession data models, AuthState, and User.
 * Does not require Android Context since these are pure data classes.
 */
class AuthSessionParsingTest {

    // ── AuthState sealed class ──────────────────────────────────────

    @Test
    fun `AuthState Unknown is distinct`() {
        val state: AuthState = AuthState.Unknown
        assertTrue(state is AuthState.Unknown)
    }

    @Test
    fun `AuthState Unauthenticated is distinct`() {
        val state: AuthState = AuthState.Unauthenticated
        assertTrue(state is AuthState.Unauthenticated)
    }

    @Test
    fun `AuthState Authenticated is distinct`() {
        val state: AuthState = AuthState.Authenticated
        assertTrue(state is AuthState.Authenticated)
    }

    @Test
    fun `AuthState AuthenticatedWithRefreshNeeded is distinct`() {
        val state: AuthState = AuthState.AuthenticatedWithRefreshNeeded
        assertTrue(state is AuthState.AuthenticatedWithRefreshNeeded)
    }

    // ── User model ──────────────────────────────────────────────────

    @Test
    fun `User displayName returns full name when both names present`() {
        val user = User(id = "1", firstName = "John", lastName = "Doe")
        assertEquals("John Doe", user.displayName)
    }

    @Test
    fun `User displayName returns firstName only when lastName missing`() {
        val user = User(id = "1", firstName = "John")
        assertEquals("John", user.displayName)
    }

    @Test
    fun `User displayName returns email prefix when no name`() {
        val user = User(id = "1", email = "john@example.com")
        assertEquals("john", user.displayName)
    }

    @Test
    fun `User displayName returns User when nothing available`() {
        val user = User(id = "1")
        assertEquals("User", user.displayName)
    }

    @Test
    fun `User displayName prefers name over email`() {
        val user = User(id = "1", firstName = "Jane", email = "jane@example.com")
        assertEquals("Jane", user.displayName)
    }

    @Test
    fun `User displayName ignores blank firstName`() {
        val user = User(id = "1", firstName = "", lastName = "", email = "test@test.com")
        assertEquals("test", user.displayName)
    }

    @Test
    fun `User preserves all optional fields`() {
        val user = User(
            id = "abc",
            email = "user@example.com",
            firstName = "Alice",
            lastName = "Bob",
            profileImage = "https://avatar.jpg",
            phone = "+1234567890"
        )
        assertEquals("abc", user.id)
        assertEquals("user@example.com", user.email)
        assertEquals("Alice", user.firstName)
        assertEquals("Bob", user.lastName)
        assertEquals("https://avatar.jpg", user.profileImage)
        assertEquals("+1234567890", user.phone)
    }

    // ── Request models ──────────────────────────────────────────────

    @Test
    fun `RefreshTokenRequest holds refresh_token`() {
        val req = RefreshTokenRequest("rt_abc123")
        assertEquals("rt_abc123", req.refresh_token)
    }

    @Test
    fun `Auth0CallbackRequest holds tokens`() {
        val req = Auth0CallbackRequest("at_token", "id_token")
        assertEquals("at_token", req.accessToken)
        assertEquals("id_token", req.idToken)
    }

    @Test
    fun `EmailLoginRequest holds credentials`() {
        val req = EmailLoginRequest("user@test.com", "password123")
        assertEquals("user@test.com", req.email)
        assertEquals("password123", req.password)
    }

    @Test
    fun `EmailRegisterRequest holds all fields`() {
        val req = EmailRegisterRequest("user@test.com", "pass", "John", "Doe")
        assertEquals("user@test.com", req.email)
        assertEquals("pass", req.password)
        assertEquals("John", req.firstName)
        assertEquals("Doe", req.lastName)
    }
}
