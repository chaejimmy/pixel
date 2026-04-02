package com.shourov.apps.pacedream.feature.host.presentation

/**
 * Maps raw Stripe requirement keys and status strings to user-friendly labels.
 *
 * Stripe Connect sends requirement keys like "individual.address.city" and
 * disabled reasons like "requirements.past_due". These are internal API fields
 * that should never be shown to users. This mapper converts them into
 * polished, human-readable text suitable for a consumer-facing app.
 */
object StripeRequirementMapper {

    /**
     * Represents a grouped checklist item for the UI.
     * Multiple raw Stripe keys may collapse into a single item
     * (e.g., dob.day + dob.month + dob.year → "Date of birth").
     */
    data class ChecklistItem(
        val label: String,
        val iconType: IconType = IconType.DEFAULT
    )

    enum class IconType { BANK, IDENTITY, ADDRESS, PHONE, LEGAL, DEFAULT }

    // ── Requirement key → friendly label ─────────────────────────

    private val requirementLabels = mapOf(
        // Bank / external account
        "external_account" to ("Add bank account" to IconType.BANK),

        // Address
        "individual.address.city" to ("Add city" to IconType.ADDRESS),
        "individual.address.line1" to ("Add street address" to IconType.ADDRESS),
        "individual.address.line2" to ("Add address line 2" to IconType.ADDRESS),
        "individual.address.postal_code" to ("Add ZIP code" to IconType.ADDRESS),
        "individual.address.state" to ("Add state" to IconType.ADDRESS),
        "individual.address.country" to ("Add country" to IconType.ADDRESS),

        // Date of birth
        "individual.dob.day" to ("Add date of birth" to IconType.IDENTITY),
        "individual.dob.month" to ("Add date of birth" to IconType.IDENTITY),
        "individual.dob.year" to ("Add date of birth" to IconType.IDENTITY),

        // Identity
        "individual.first_name" to ("Add first name" to IconType.IDENTITY),
        "individual.last_name" to ("Add last name" to IconType.IDENTITY),
        "individual.email" to ("Add email address" to IconType.IDENTITY),
        "individual.phone" to ("Add phone number" to IconType.PHONE),
        "individual.id_number" to ("Add ID number" to IconType.IDENTITY),
        "individual.ssn_last_4" to ("Add last 4 digits of SSN" to IconType.IDENTITY),
        "individual.ssn_full" to ("Add Social Security number" to IconType.IDENTITY),

        // TOS
        "tos_acceptance.date" to ("Accept Terms of Service" to IconType.LEGAL),
        "tos_acceptance.ip" to ("Accept Terms of Service" to IconType.LEGAL),
        // Alternative format
        "tos.acceptance.date" to ("Accept Terms of Service" to IconType.LEGAL),
        "tos.acceptance.ip" to ("Accept Terms of Service" to IconType.LEGAL),

        // Business
        "business_profile.url" to ("Add business website" to IconType.DEFAULT),
        "business_profile.mcc" to ("Add business category" to IconType.DEFAULT),
        "business_type" to ("Select business type" to IconType.DEFAULT),

        // Verification documents
        "individual.verification.document" to ("Upload ID document" to IconType.IDENTITY),
        "individual.verification.additional_document" to ("Upload additional document" to IconType.IDENTITY),
    )

    /**
     * Convert a list of raw Stripe requirement keys into grouped, user-friendly
     * checklist items. Deduplicates items that map to the same label (e.g.,
     * dob.day/month/year all become one "Add date of birth" item).
     */
    fun mapRequirements(rawKeys: List<String>): List<ChecklistItem> {
        val seen = mutableSetOf<String>()
        val items = mutableListOf<ChecklistItem>()

        for (key in rawKeys) {
            val (label, iconType) = requirementLabels[key]
                ?: (friendlyFallback(key) to IconType.DEFAULT)

            if (seen.add(label)) {
                items.add(ChecklistItem(label, iconType))
            }
        }

        return items
    }

    /**
     * Best-effort fallback for unknown keys: strip prefixes, replace
     * dots/underscores with spaces, and title-case the result.
     */
    private fun friendlyFallback(key: String): String {
        val cleaned = key
            .removePrefix("individual.")
            .removePrefix("company.")
            .removePrefix("business_profile.")
            .replace(".", " ")
            .replace("_", " ")
            .trim()

        return "Add ${cleaned.replaceFirstChar { it.lowercase() }}"
    }

    // ── Disabled reason → friendly message ───────────────────────

    /**
     * Convert a raw Stripe disabled_reason string into a user-friendly message.
     */
    fun mapDisabledReason(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return disabledReasonLabels[raw.trim()] ?: friendlyReasonFallback(raw)
    }

    private val disabledReasonLabels = mapOf(
        "requirements.past_due" to "Some required information is overdue. Complete your setup to enable payouts.",
        "requirements.pending_verification" to "Your information is being verified by Stripe. This usually takes 1–2 business days.",
        "listed" to "Your account is under review.",
        "platform_paused" to "Payouts are temporarily paused.",
        "rejected.fraud" to "Your account was flagged for review. Please contact support.",
        "rejected.listed" to "Your account was flagged for review. Please contact support.",
        "rejected.terms_of_service" to "Your account was disabled due to a Terms of Service violation.",
        "rejected.other" to "Your account could not be verified. Please contact support.",
        "under_review" to "Your account is being reviewed. This usually takes 1–2 business days.",
        "other" to "There is an issue with your payout account. Please contact support."
    )

    private fun friendlyReasonFallback(raw: String): String {
        val cleaned = raw.replace("_", " ").replace(".", " ").trim()
        return "Payouts are paused: ${cleaned.replaceFirstChar { it.lowercase() }}. Complete your setup to resume."
    }

    // ── Status label mapping (for payout/transaction badges) ─────

    fun mapPayoutStatus(raw: String): String = when (raw.lowercase()) {
        "paid" -> "Deposited"
        "in_transit" -> "On the way"
        "pending" -> "Processing"
        "failed" -> "Failed"
        "canceled" -> "Canceled"
        else -> raw.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    fun mapTransactionStatus(raw: String?): String = when (raw?.lowercase()) {
        "transferred" -> "Transferred"
        "pending_settlement" -> "Settling"
        "held", "ready_for_transfer" -> "Ready"
        "released" -> "Transferred"
        "blocked" -> "On hold"
        "clawed_back" -> "Refunded"
        "paid_out" -> "Paid out"
        "cancelled" -> "Cancelled"
        "succeeded", "paid" -> "Paid"
        "processing" -> "Processing"
        else -> raw?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }
}
