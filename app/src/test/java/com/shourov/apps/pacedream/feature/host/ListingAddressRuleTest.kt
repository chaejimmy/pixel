package com.shourov.apps.pacedream.feature.host

import com.shourov.apps.pacedream.feature.host.data.ListingAddressRule
import com.shourov.apps.pacedream.feature.host.data.SessionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the rule that the host must NOT be required to enter a
 * physical address when they have explicitly chosen to deliver the
 * listing online. In-Person and Both still require an address.
 */
class ListingAddressRuleTest {

    // ── isAddressRequired ─────────────────────────────────────────

    @Test
    fun `online service listing does not require an address`() {
        assertFalse(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = false,
                hasSessionType = true,
                sessionType = SessionType.ONLINE,
            )
        )
    }

    @Test
    fun `in-person service listing requires an address`() {
        assertTrue(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = false,
                hasSessionType = true,
                sessionType = SessionType.IN_PERSON,
            )
        )
    }

    @Test
    fun `both online and in-person service listing requires an address`() {
        assertTrue(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = false,
                hasSessionType = true,
                sessionType = SessionType.BOTH,
            )
        )
    }

    @Test
    fun `service listing with no session type chosen yet does not require an address`() {
        // Session-type pickers are validated separately; the address
        // rule treats "not chosen" as "don't enforce yet" so the wizard
        // surfaces the session-type prompt first instead of an address
        // error the host can't act on.
        assertFalse(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = false,
                hasSessionType = true,
                sessionType = null,
            )
        )
    }

    @Test
    fun `physical-space listing without session type still requires an address`() {
        assertTrue(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = false,
                hasSessionType = false,
                sessionType = null,
            )
        )
    }

    @Test
    fun `split listings never require an address regardless of session type`() {
        assertFalse(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = true,
                hasSessionType = true,
                sessionType = SessionType.IN_PERSON,
            )
        )
        assertFalse(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = true,
                isSplit = true,
                hasSessionType = false,
                sessionType = null,
            )
        )
    }

    @Test
    fun `subcategory schema without LOCATION never requires an address`() {
        assertFalse(
            ListingAddressRule.isAddressRequired(
                schemaHasLocation = false,
                isSplit = false,
                hasSessionType = false,
                sessionType = null,
            )
        )
    }

    // ── shouldOmitLocation ────────────────────────────────────────

    @Test
    fun `online service omits the location payload`() {
        assertTrue(
            ListingAddressRule.shouldOmitLocation(
                hasSessionType = true,
                sessionType = SessionType.ONLINE,
            )
        )
    }

    @Test
    fun `in-person and both still send a location payload`() {
        assertFalse(
            ListingAddressRule.shouldOmitLocation(
                hasSessionType = true,
                sessionType = SessionType.IN_PERSON,
            )
        )
        assertFalse(
            ListingAddressRule.shouldOmitLocation(
                hasSessionType = true,
                sessionType = SessionType.BOTH,
            )
        )
    }

    @Test
    fun `physical-space listings never omit the location payload`() {
        assertFalse(
            ListingAddressRule.shouldOmitLocation(
                hasSessionType = false,
                sessionType = null,
            )
        )
    }

    @Test
    fun `service listing without a chosen session type sends a location payload`() {
        // Until the host picks Online, the wizard keeps the location
        // block in the payload — the validation step blocks the publish
        // call before this code path runs in practice.
        assertFalse(
            ListingAddressRule.shouldOmitLocation(
                hasSessionType = true,
                sessionType = null,
            )
        )
    }
}
