package com.shourov.apps.pacedream.feature.wanted.data.dto

import com.shourov.apps.pacedream.feature.wanted.model.ModerationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Locks the wire mapping for the moderation columns introduced alongside
 * structured location:
 *
 *  - `moderationStatus` is the canonical field for new payloads.
 *  - `status` is consulted as a legacy fallback only when `moderationStatus`
 *    is missing. Lifecycle values like `open` map to [ModerationStatus.Approved]
 *    so old data stays visible on the public feed.
 *  - Whitespace-only or null `moderationReason` collapses to null so the
 *    detail screen doesn't render an empty "rejected" banner body.
 */
class RequestDtoModerationTest {

    @Test
    fun `moderationStatus maps to the canonical enum`() {
        val dto = baseDto.copy(moderationStatus = "pending_review")
        assertEquals(ModerationStatus.PendingReview, dto.toDomain().moderationStatus)
    }

    @Test
    fun `legacy status field is used only when moderationStatus is missing`() {
        // A pure legacy payload — no new column. The legacy `status`
        // column carries `rejected`, which the app must honour.
        val legacy = baseDto.copy(moderationStatus = null, status = "rejected")
        assertEquals(ModerationStatus.Rejected, legacy.toDomain().moderationStatus)

        // Mixed payload — both fields present, new column wins.
        val mixed = baseDto.copy(moderationStatus = "approved", status = "rejected")
        assertEquals(ModerationStatus.Approved, mixed.toDomain().moderationStatus)
    }

    @Test
    fun `lifecycle values in the legacy status column do not hide a record`() {
        // Old feeds repurposed `status` for lifecycle ("open", "matched");
        // those values must not be misread as a moderation verdict that
        // would hide the record from the public feed.
        listOf("open", "matched", "in_progress", "completed", null, "")
            .forEach { value ->
                val dto = baseDto.copy(moderationStatus = null, status = value)
                assertEquals(
                    "lifecycle value '$value' must fall through to Approved",
                    ModerationStatus.Approved,
                    dto.toDomain().moderationStatus,
                )
            }
    }

    @Test
    fun `blank moderationReason collapses to null`() {
        val dto = baseDto.copy(
            moderationStatus = "rejected",
            moderationReason = "   ",
            rejectionReason = "",
        )
        assertNull(dto.toDomain().moderationReason)
    }

    @Test
    fun `rejectionReason is the fallback for moderationReason`() {
        val dto = baseDto.copy(
            moderationStatus = "rejected",
            moderationReason = null,
            rejectionReason = "Title looks like spam",
        )
        assertEquals("Title looks like spam", dto.toDomain().moderationReason)
    }

    private val baseDto = RequestDto(
        id = "req_1",
        title = "Need parking near downtown",
        description = "Looking for a covered spot for two weeks.",
        type = "space",
        category = "parking",
    )
}
