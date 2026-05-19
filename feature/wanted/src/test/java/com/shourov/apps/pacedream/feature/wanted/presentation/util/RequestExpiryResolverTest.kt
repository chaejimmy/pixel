package com.shourov.apps.pacedream.feature.wanted.presentation.util

import com.shourov.apps.pacedream.feature.wanted.model.RequestStatus
import com.shourov.apps.pacedream.feature.wanted.model.WantedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Locks the client-side fallback rules for request lifecycle:
 *
 *  - Future-dated requests stay Active. Spec acceptance:
 *    "Future-dated requests do not expire immediately."
 *  - Past `expiresAt` with a server-declared Active status falls through
 *    to Expired — the client never trusts a stale row.
 *  - Server-declared closed states (Fulfilled / Cancelled) win over any
 *    time-based check.
 *  - "Expiring soon" only ever triggers when the request is still Active
 *    and the auto-expiry is inside the urgency window.
 */
class RequestExpiryResolverTest {

    private val today: LocalDate = LocalDate.of(2026, 5, 18)

    @Test
    fun `future-dated active request stays Active`() {
        // Acceptance: future-dated requests do not expire immediately.
        val request = baseRequest.copy(
            requestStartDate = "2026-07-04",
            requestEndDate = "2026-07-10",
            expiresAt = "2026-07-10",
            status = RequestStatus.Active,
        )
        assertEquals(RequestStatus.Active, RequestExpiryResolver.effectiveStatus(request, today))
    }

    @Test
    fun `past expiresAt flips an Active record to Expired client-side`() {
        // Acceptance: expired requests don't get to receive new offers —
        // the lifecycle gate in [RequestsViewModel] relies on this fallback.
        val request = baseRequest.copy(
            expiresAt = "2026-05-01",
            status = RequestStatus.Active,
        )
        assertEquals(RequestStatus.Expired, RequestExpiryResolver.effectiveStatus(request, today))
    }

    @Test
    fun `server-declared Fulfilled outranks a still-valid expiresAt`() {
        // A request marked Fulfilled must stay Fulfilled even if its expiry
        // hasn't passed — we never want to "revive" a closed request via a
        // client-side computation.
        val request = baseRequest.copy(
            expiresAt = "2030-01-01",
            status = RequestStatus.Fulfilled,
        )
        assertEquals(RequestStatus.Fulfilled, RequestExpiryResolver.effectiveStatus(request, today))
    }

    @Test
    fun `Cancelled status takes precedence over any expiry`() {
        val request = baseRequest.copy(
            expiresAt = null,
            status = RequestStatus.Cancelled,
        )
        assertEquals(RequestStatus.Cancelled, RequestExpiryResolver.effectiveStatus(request, today))
    }

    @Test
    fun `resolveExpiry prefers expiresAt over end and start`() {
        val request = baseRequest.copy(
            requestStartDate = "2026-06-01",
            requestEndDate = "2026-06-10",
            expiresAt = "2026-07-01",
        )
        assertEquals(LocalDate.of(2026, 7, 1), RequestExpiryResolver.resolveExpiry(request))
    }

    @Test
    fun `resolveExpiry falls back to end then start when expiresAt is missing`() {
        val ended = baseRequest.copy(
            requestStartDate = "2026-06-01",
            requestEndDate = "2026-06-10",
            expiresAt = null,
        )
        assertEquals(LocalDate.of(2026, 6, 10), RequestExpiryResolver.resolveExpiry(ended))

        val openWindow = baseRequest.copy(
            requestStartDate = "2026-06-01",
            requestEndDate = null,
            expiresAt = null,
        )
        assertEquals(LocalDate.of(2026, 6, 1), RequestExpiryResolver.resolveExpiry(openWindow))
    }

    @Test
    fun `resolveExpiry returns null for legacy free-text dates`() {
        // Old records carry strings like "Sat 10:00 AM" — those mean
        // "open-ended" for our purposes.
        val request = baseRequest.copy(
            requestStartDate = "Sat 10:00 AM",
            requestEndDate = null,
            expiresAt = null,
        )
        assertNull(RequestExpiryResolver.resolveExpiry(request))
    }

    @Test
    fun `resolveExpiry accepts ISO-8601 instants`() {
        val request = baseRequest.copy(
            expiresAt = "2026-07-01T12:30:00Z",
        )
        assertEquals(LocalDate.of(2026, 7, 1), RequestExpiryResolver.resolveExpiry(request))
    }

    @Test
    fun `expiring soon true within the urgency window`() {
        val tomorrow = baseRequest.copy(
            expiresAt = today.plusDays(1).toString(),
            status = RequestStatus.Active,
        )
        assertTrue(RequestExpiryResolver.isExpiringSoon(tomorrow, today))
    }

    @Test
    fun `expiring soon false when expiry is well beyond the window`() {
        val distant = baseRequest.copy(
            expiresAt = today.plusDays(30).toString(),
            status = RequestStatus.Active,
        )
        assertFalse(RequestExpiryResolver.isExpiringSoon(distant, today))
    }

    @Test
    fun `expiring soon false for already-expired records`() {
        val past = baseRequest.copy(
            expiresAt = today.minusDays(1).toString(),
            status = RequestStatus.Active,
        )
        assertFalse(RequestExpiryResolver.isExpiringSoon(past, today))
    }

    @Test
    fun `expiring soon false for non-active records even inside the window`() {
        val fulfilled = baseRequest.copy(
            expiresAt = today.plusDays(1).toString(),
            status = RequestStatus.Fulfilled,
        )
        assertFalse(RequestExpiryResolver.isExpiringSoon(fulfilled, today))
    }

    @Test
    fun `activeUntilLabel is null when no expiry can be resolved`() {
        assertNull(RequestExpiryResolver.activeUntilLabel(baseRequest))
    }

    private val baseRequest = WantedRequest(
        id = "req_1",
        title = "Need a covered parking spot",
        description = "Looking for a covered spot for the long weekend.",
        type = "space",
        category = "parking",
        location = "San Francisco",
        budget = 100.0,
        imageUrl = null,
    )
}
