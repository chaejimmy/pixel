package com.shourov.apps.pacedream.feature.host.data

/**
 * Single source of truth for "does this listing need a physical address?".
 *
 * Service listings ship with a delivery-mode selector (Online / In person /
 * Both). When the host picks Online the wizard hides the address inputs and
 * the publish payload omits the location block entirely — guests never
 * travel to a place, so requiring a street address is wrong both in the UI
 * and on the wire. In-Person and Both still require an address. Listings
 * that don't surface the selector (physical-space, item rentals) keep the
 * existing always-address-required contract.
 */
object ListingAddressRule {

    /**
     * Whether the wizard should validate that the host entered a street
     * address before advancing past the location step.
     */
    fun isAddressRequired(
        schemaHasLocation: Boolean,
        isSplit: Boolean,
        hasSessionType: Boolean,
        sessionType: SessionType?,
    ): Boolean {
        if (!schemaHasLocation) return false
        if (isSplit) return false
        if (!hasSessionType) return true
        return sessionType == SessionType.IN_PERSON || sessionType == SessionType.BOTH
    }

    /**
     * Whether the publish/edit payload should omit the location block
     * entirely. Online-only services are the explicit exception — sending
     * a blank or partial location would otherwise be rejected by the
     * backend's city/state/country requirement.
     */
    fun shouldOmitLocation(
        hasSessionType: Boolean,
        sessionType: SessionType?,
    ): Boolean = hasSessionType && sessionType == SessionType.ONLINE
}
