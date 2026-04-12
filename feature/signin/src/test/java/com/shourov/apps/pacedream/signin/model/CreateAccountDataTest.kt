package com.shourov.apps.pacedream.signin.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Tests for CreateAccountData validation functions,
 * including the minimum-age (13) enforcement for COPPA compliance.
 */
class CreateAccountDataTest {

    // ── dateOfBirthValid ─────────────────────────────���─────────────

    @Test
    fun `dateOfBirthValid returns false when null`() {
        val data = CreateAccountData(dateOfBirthMillis = null)
        assertFalse(data.dateOfBirthValid())
    }

    @Test
    fun `dateOfBirthValid returns false for future date`() {
        val futureMillis = System.currentTimeMillis() + 86_400_000L // tomorrow
        val data = CreateAccountData(dateOfBirthMillis = futureMillis)
        assertFalse(data.dateOfBirthValid())
    }

    @Test
    fun `dateOfBirthValid returns false for user under 13`() {
        // 10 years ago
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -10)
        val data = CreateAccountData(dateOfBirthMillis = cal.timeInMillis)
        assertFalse(data.dateOfBirthValid())
    }

    @Test
    fun `dateOfBirthValid returns false for user born today`() {
        val data = CreateAccountData(dateOfBirthMillis = System.currentTimeMillis())
        assertFalse(data.dateOfBirthValid())
    }

    @Test
    fun `dateOfBirthValid returns true for user exactly 13`() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -13)
        val data = CreateAccountData(dateOfBirthMillis = cal.timeInMillis)
        assertTrue(data.dateOfBirthValid())
    }

    @Test
    fun `dateOfBirthValid returns true for adult user`() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -25)
        val data = CreateAccountData(dateOfBirthMillis = cal.timeInMillis)
        assertTrue(data.dateOfBirthValid())
    }

    // ── userProfileDetailsValid ────────────────────────────────────

    @Test
    fun `userProfileDetailsValid returns false when firstName empty`() {
        val data = CreateAccountData(firstName = "", lastName = "Doe", email = "a@b.com")
        assertFalse(data.userProfileDetailsValid())
    }

    @Test
    fun `userProfileDetailsValid returns false when email empty`() {
        val data = CreateAccountData(firstName = "John", lastName = "Doe", email = "")
        assertFalse(data.userProfileDetailsValid())
    }

    @Test
    fun `userProfileDetailsValid returns true when all present`() {
        val data = CreateAccountData(firstName = "John", lastName = "Doe", email = "a@b.com")
        assertTrue(data.userProfileDetailsValid())
    }
}
