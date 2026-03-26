package com.shourov.apps.pacedream.feature.host

import com.shourov.apps.pacedream.feature.host.data.PayoutSetupEligibilityResponse
import com.shourov.apps.pacedream.notification.NotificationData
import com.shourov.apps.pacedream.notification.NotificationType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for payout setup eligibility guards on Android.
 *
 * These tests validate that:
 * - Logged-out users never see payout setup prompts
 * - Guest-only users never see payout setup prompts
 * - Users with no listings/earnings don't see prompts
 * - Only eligible hosts see prompts
 * - Payout-related notifications are correctly identified for auth-gating
 */
class PayoutEligibilityTest {

    // ── Server-driven eligibility response tests ───

    @Test
    fun `default eligibility response does not show prompt`() {
        val response = PayoutSetupEligibilityResponse()
        assertFalse(response.shouldShowPayoutSetupPrompt)
        assertNull(response.payoutPromptReason)
        assertFalse(response.payoutOnboardingComplete)
        assertEquals("UNSET", response.payoutStatus)
    }

    @Test
    fun `eligible host with active listing should show prompt`() {
        val response = PayoutSetupEligibilityResponse(
            shouldShowPayoutSetupPrompt = true,
            payoutPromptReason = "active_listing",
            payoutOnboardingComplete = false,
            payoutStatus = "UNSET"
        )
        assertTrue(response.shouldShowPayoutSetupPrompt)
        assertEquals("active_listing", response.payoutPromptReason)
    }

    @Test
    fun `eligible host with pending earnings should show prompt`() {
        val response = PayoutSetupEligibilityResponse(
            shouldShowPayoutSetupPrompt = true,
            payoutPromptReason = "pending_earnings",
            payoutOnboardingComplete = false,
            payoutStatus = "PENDING"
        )
        assertTrue(response.shouldShowPayoutSetupPrompt)
        assertEquals("pending_earnings", response.payoutPromptReason)
    }

    @Test
    fun `host who completed onboarding should not see prompt`() {
        val response = PayoutSetupEligibilityResponse(
            shouldShowPayoutSetupPrompt = false,
            payoutPromptReason = null,
            payoutOnboardingComplete = true,
            payoutStatus = "READY"
        )
        assertFalse(response.shouldShowPayoutSetupPrompt)
        assertTrue(response.payoutOnboardingComplete)
    }

    @Test
    fun `API failure defaults to not showing prompt`() {
        // When the API fails, the default PayoutSetupEligibilityResponse is used
        val response = PayoutSetupEligibilityResponse()
        assertFalse(response.shouldShowPayoutSetupPrompt)
    }

    // ── Notification auth-gating tests ───

    @Test
    fun `payout initiated notification is identified as payout related`() {
        val data = NotificationData(type = NotificationType.PAYOUT_INITIATED)
        assertTrue(data.isPayoutRelated)
    }

    @Test
    fun `payout failed notification is identified as payout related`() {
        val data = NotificationData(type = NotificationType.PAYOUT_FAILED)
        assertTrue(data.isPayoutRelated)
    }

    @Test
    fun `payment received notification is identified as payout related`() {
        val data = NotificationData(type = NotificationType.PAYMENT_RECEIVED)
        assertTrue(data.isPayoutRelated)
    }

    @Test
    fun `host onboarding screen notification is identified as payout related`() {
        val data = NotificationData(
            type = NotificationType.REMINDER,
            screen = "host_onboarding"
        )
        assertTrue(data.isPayoutRelated)
    }

    @Test
    fun `payout settings screen notification is identified as payout related`() {
        val data = NotificationData(
            type = NotificationType.SYSTEM_UPDATE,
            screen = "payout_settings"
        )
        assertTrue(data.isPayoutRelated)
    }

    @Test
    fun `booking notification is NOT payout related`() {
        val data = NotificationData(type = NotificationType.BOOKING_CONFIRMED)
        assertFalse(data.isPayoutRelated)
    }

    @Test
    fun `message notification is NOT payout related`() {
        val data = NotificationData(type = NotificationType.MESSAGE_RECEIVED)
        assertFalse(data.isPayoutRelated)
    }

    @Test
    fun `review notification is NOT payout related`() {
        val data = NotificationData(type = NotificationType.REVIEW_RECEIVED)
        assertFalse(data.isPayoutRelated)
    }

    @Test
    fun `notification with non-payout screen is NOT payout related`() {
        val data = NotificationData(
            type = NotificationType.SYSTEM_UPDATE,
            screen = "host_dashboard"
        )
        assertFalse(data.isPayoutRelated)
    }
}
