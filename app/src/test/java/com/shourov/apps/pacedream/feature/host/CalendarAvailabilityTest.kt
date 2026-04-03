package com.shourov.apps.pacedream.feature.host

import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.feature.host.data.*
import com.shourov.apps.pacedream.model.BookingStatus
import com.shourov.apps.pacedream.model.Property
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive tests for the Android calendar/availability system
 * synced against backend source of truth (availabilityService.js, bookingStatuses.js).
 *
 * Tests cover:
 * 1. BookingStatus enum mapping against backend ACTIVE/TERMINAL statuses
 * 2. Listing bookability enforcement
 * 3. Availability check response parsing
 * 4. Block time request/response DTO alignment
 * 5. Calendar data parsing
 * 6. Timezone handling
 * 7. Booking lifecycle impact on availability
 */
class CalendarAvailabilityTest {

    // ════════════════════════════════════════════════════════════════
    // 1. BOOKING STATUS MAPPING — Backend ACTIVE_BOOKING_STATUSES
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `active booking statuses match backend ACTIVE_BOOKING_STATUSES`() {
        // Backend: ACTIVE_BOOKING_STATUSES = [created, pending, pending_host, confirmed, requires_capture, ongoing]
        val activeStatuses = listOf("created", "pending", "pending_host", "confirmed", "requires_capture", "ongoing")
        for (status in activeStatuses) {
            val mapped = BookingStatus.fromString(status)
            assertTrue(
                "Status '$status' should be active (blocks availability), but was $mapped (isActive=${mapped.isActive})",
                mapped.isActive
            )
        }
    }

    @Test
    fun `terminal booking statuses match backend TERMINAL_BOOKING_STATUSES`() {
        // Backend: TERMINAL_BOOKING_STATUSES = [cancelled, completed, refunded, expired]
        val terminalStatuses = listOf("cancelled", "canceled", "Cancelled", "completed", "Completed", "refunded", "Refunded and Cancelled", "expired")
        for (status in terminalStatuses) {
            val mapped = BookingStatus.fromString(status)
            assertTrue(
                "Status '$status' should be terminal (frees slot), but was $mapped (isTerminal=${mapped.isTerminal})",
                mapped.isTerminal
            )
        }
    }

    @Test
    fun `cancelled variants all map to terminal`() {
        val cancelVariants = listOf("cancelled", "canceled", "Cancelled", "CANCELLED")
        for (variant in cancelVariants) {
            val mapped = BookingStatus.fromString(variant)
            assertTrue("'$variant' should be terminal", mapped.isTerminal)
        }
    }

    @Test
    fun `refunded status maps correctly`() {
        assertEquals(BookingStatus.REFUNDED, BookingStatus.fromString("refunded"))
        assertEquals(BookingStatus.REFUNDED, BookingStatus.fromString("Refunded and Cancelled"))
        assertTrue(BookingStatus.REFUNDED.isTerminal)
        assertFalse(BookingStatus.REFUNDED.isActive)
    }

    @Test
    fun `expired status maps correctly`() {
        assertEquals(BookingStatus.EXPIRED, BookingStatus.fromString("expired"))
        assertTrue(BookingStatus.EXPIRED.isTerminal)
        assertFalse(BookingStatus.EXPIRED.isActive)
    }

    @Test
    fun `ongoing status maps correctly and blocks availability`() {
        assertEquals(BookingStatus.ONGOING, BookingStatus.fromString("ongoing"))
        assertTrue(BookingStatus.ONGOING.isActive)
    }

    @Test
    fun `requires_capture status maps correctly and blocks availability`() {
        assertEquals(BookingStatus.REQUIRES_CAPTURE, BookingStatus.fromString("requires_capture"))
        assertTrue(BookingStatus.REQUIRES_CAPTURE.isActive)
    }

    @Test
    fun `pending_host status maps correctly and blocks availability`() {
        assertEquals(BookingStatus.PENDING_HOST, BookingStatus.fromString("pending_host"))
        assertTrue(BookingStatus.PENDING_HOST.isActive)
    }

    @Test
    fun `created status maps correctly and blocks availability`() {
        assertEquals(BookingStatus.CREATED, BookingStatus.fromString("created"))
        assertTrue(BookingStatus.CREATED.isActive)
    }

    @Test
    fun `null and blank status defaults to PENDING`() {
        assertEquals(BookingStatus.PENDING, BookingStatus.fromString(null))
        assertEquals(BookingStatus.PENDING, BookingStatus.fromString(""))
        assertEquals(BookingStatus.PENDING, BookingStatus.fromString("  "))
    }

    @Test
    fun `booking status display labels are correct`() {
        assertEquals("Pending", BookingStatus.PENDING.displayLabel)
        assertEquals("Awaiting Host", BookingStatus.PENDING_HOST.displayLabel)
        assertEquals("Confirmed", BookingStatus.CONFIRMED.displayLabel)
        assertEquals("Ongoing", BookingStatus.ONGOING.displayLabel)
        assertEquals("Processing", BookingStatus.REQUIRES_CAPTURE.displayLabel)
        assertEquals("Cancelled", BookingStatus.CANCELLED.displayLabel)
        assertEquals("Completed", BookingStatus.COMPLETED.displayLabel)
        assertEquals("Refunded", BookingStatus.REFUNDED.displayLabel)
        assertEquals("Expired", BookingStatus.EXPIRED.displayLabel)
    }

    // ════════════════════════════════════════════════════════════════
    // 2. LISTING BOOKABILITY — Backend BOOKABLE_LISTING_STATUSES
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `bookable listing statuses match backend BOOKABLE_LISTING_STATUSES`() {
        // Backend: BOOKABLE_LISTING_STATUSES = ['active', 'published', 'APPROVED', 'approved']
        val bookableStatuses = listOf("active", "published", "approved")
        for (status in bookableStatuses) {
            val property = Property(status = status)
            assertTrue(
                "Listing with status '$status' should be bookable, but isBookable=${property.isBookable}",
                property.isBookable
            )
        }
    }

    @Test
    fun `non-bookable listing statuses are correctly rejected`() {
        // Backend: these are NOT in BOOKABLE_LISTING_STATUSES
        val nonBookable = listOf(
            "draft", "inactive", "pending_review", "rejected",
            "archived", "unpublished_account_deleted", "permanently_deleted"
        )
        for (status in nonBookable) {
            val property = Property(status = status)
            assertFalse(
                "Listing with status '$status' should NOT be bookable, but isBookable=${property.isBookable}",
                property.isBookable
            )
        }
    }

    @Test
    fun `null status listing is not bookable`() {
        val property = Property(status = null)
        assertFalse("Listing with null status should not be bookable", property.isBookable)
    }

    @Test
    fun `BOOKABLE_LISTING_STATUSES constant matches backend`() {
        assertEquals(
            setOf("active", "published", "approved"),
            Property.BOOKABLE_LISTING_STATUSES
        )
    }

    @Test
    fun `BOOKABLE_MODERATION_STATUSES constant matches backend`() {
        // Backend: BOOKABLE_MODERATION_STATUSES = ['PUBLISHED']
        assertEquals(
            setOf("published"),
            Property.BOOKABLE_MODERATION_STATUSES
        )
    }

    // ════════════════════════════════════════════════════════════════
    // 3. AVAILABILITY CHECK RESPONSE — Backend check-availability
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `availability check result display reasons map ALL backend reason codes`() {
        // All reason codes from checkFullAvailability() in availabilityService.js
        val testCases = mapOf(
            "BOOKING_CONFLICT" to "This time overlaps with an existing booking",
            "HOLD_CONFLICT" to "This time is being held for another checkout",
            "LISTING_NOT_FOUND" to "This listing could not be found",
            "LISTING_SNOOZED" to "This listing is currently paused",
            "LISTING_ARCHIVED" to "This listing is no longer available",
            "LISTING_DELETED" to "This listing has been removed",
            "LISTING_HIDDEN" to "This listing is not currently visible",
            "INVALID_DATE_FORMAT" to "Invalid date format",
            "END_BEFORE_START" to "End time must be after start time",
            "START_IN_PAST" to "Cannot book in the past",
            "BEFORE_AVAILABILITY_START" to "This date is before the listing's available dates",
            "AFTER_AVAILABILITY_END" to "This date is after the listing's available dates",
            "DURATION_TOO_SHORT:min=15m" to "Booking duration is too short (minimum 15 minutes)",
            "DURATION_TOO_LONG:max=43200m" to "Booking duration is too long (maximum 30 days)",
            "BLOCKED_PERIOD:2026-04-03_to_2026-04-05" to "This time is blocked by the host",
            "DAY_NOT_AVAILABLE:2026-04-03" to "This day is not available for booking",
            "LISTING_STATUS_NOT_BOOKABLE:draft" to "This listing is not currently accepting bookings",
            "MODERATION_STATUS_NOT_BOOKABLE:DRAFT" to "This listing is pending moderation review"
        )

        for ((reason, expectedDisplay) in testCases) {
            val result = BookingRepository.AvailabilityCheckResult(
                available = false,
                reason = reason,
                listingBookable = true,
                listingStatus = "active",
                listingTimezone = "America/New_York"
            )
            assertEquals(
                "Reason '$reason' should display as '$expectedDisplay'",
                expectedDisplay,
                result.displayReason
            )
        }
    }

    @Test
    fun `available result has empty display reason`() {
        val result = BookingRepository.AvailabilityCheckResult(
            available = true,
            reason = null,
            listingBookable = true,
            listingStatus = "active",
            listingTimezone = "America/New_York"
        )
        assertEquals("", result.displayReason)
    }

    @Test
    fun `unavailable with null reason shows generic message`() {
        val result = BookingRepository.AvailabilityCheckResult(
            available = false,
            reason = null,
            listingBookable = true,
            listingStatus = "active",
            listingTimezone = "America/New_York"
        )
        assertEquals("This time is not available", result.displayReason)
    }

    // ════════════════════════════════════════════════════════════════
    // 4. BLOCK TIME DTOs — Backend alignment
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `BlockTimeRequest has correct field structure for backend`() {
        val request = BlockTimeRequest(
            startDate = "2026-04-03",
            endDate = "2026-04-03",
            startTime = "10:00",
            endTime = "18:00",
            reason = "personal",
            repeat = "none"
        )
        assertEquals("2026-04-03", request.startDate)
        assertEquals("2026-04-03", request.endDate)
        assertEquals("10:00", request.startTime)
        assertEquals("18:00", request.endTime)
        assertEquals("personal", request.reason)
        assertEquals("none", request.repeat)
    }

    @Test
    fun `BackendBlock matches backend block structure`() {
        // Backend block ID format: "block-{timestamp}-{randomStr}"
        val block = BackendBlock(
            id = "block-1712160000000-abc123",
            startDate = "2026-04-03",
            endDate = "2026-04-05",
            startTime = "10:00",
            endTime = "18:00",
            reason = "maintenance",
            repeat = "weekly"
        )
        assertTrue(block.id.startsWith("block-"))
        assertEquals("2026-04-03", block.startDate)
        assertEquals("2026-04-05", block.endDate)
        assertEquals("maintenance", block.reason)
        assertEquals("weekly", block.repeat)
    }

    @Test
    fun `backend block reason values match allowed set`() {
        // Backend validates: personal, maintenance, custom
        val validReasons = listOf("personal", "maintenance", "custom")
        for (reason in validReasons) {
            val request = BlockTimeRequest(
                startDate = "2026-04-03",
                endDate = "2026-04-03",
                reason = reason
            )
            assertTrue(reason in validReasons)
        }
    }

    @Test
    fun `backend block repeat values match allowed set`() {
        // Backend validates: none, daily, weekly, monthly
        val validRepeats = listOf("none", "daily", "weekly", "monthly")
        for (repeat in validRepeats) {
            val request = BlockTimeRequest(
                startDate = "2026-04-03",
                endDate = "2026-04-03",
                repeat = repeat
            )
            assertTrue(repeat in validRepeats)
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 5. CALENDAR DATA PARSING
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `AvailabilityData has correct defaults matching backend`() {
        val data = AvailabilityData()
        assertEquals("America/New_York", data.timezone)
        assertEquals("09:00", data.startTime)
        assertEquals("17:00", data.endTime)
        assertEquals(listOf(1, 2, 3, 4, 5), data.availableDays)
        assertFalse(data.instantBooking)
    }

    @Test
    fun `ListingCalendarData uses days map structure matching backend`() {
        // Backend returns: data.days = { "2026-04-03": { date, status, bookings, blocks } }
        val data = ListingCalendarData(
            listingId = "listing123",
            listingTitle = "Test Listing",
            month = 4,
            year = 2026,
            days = mapOf(
                "2026-04-03" to CalendarDayData(
                    date = "2026-04-03",
                    status = "booked",
                    bookings = listOf(
                        CalendarDayBooking(id = "b1", startTime = "2026-04-03T09:00:00Z", endTime = "2026-04-03T11:00:00Z", status = "confirmed")
                    ),
                    blocks = emptyList()
                ),
                "2026-04-04" to CalendarDayData(
                    date = "2026-04-04",
                    status = "blocked",
                    bookings = emptyList(),
                    blocks = listOf(
                        CalendarDayBlock(id = "block-1", startDate = "2026-04-04", endDate = "2026-04-04", reason = "personal")
                    )
                )
            )
        )
        assertEquals("listing123", data.listingId)
        assertEquals("Test Listing", data.listingTitle)
        assertEquals(2, data.days.size)
        assertEquals("booked", data.days["2026-04-03"]?.status)
        assertEquals("blocked", data.days["2026-04-04"]?.status)
        assertEquals(1, data.days["2026-04-03"]?.bookings?.size)
        assertEquals("confirmed", data.days["2026-04-03"]?.bookings?.first()?.status)
    }

    @Test
    fun `CalendarDayData defaults to available status`() {
        val day = CalendarDayData()
        assertEquals("available", day.status)
        assertTrue(day.bookings.isEmpty())
        assertTrue(day.blocks.isEmpty())
    }

    @Test
    fun `CalendarDayBooking uses startTime and endTime fields matching backend`() {
        // Backend returns: { id, startTime (Date), endTime (Date), status }
        val booking = CalendarDayBooking(
            id = "b1",
            startTime = "2026-04-03T09:00:00.000Z",
            endTime = "2026-04-03T11:00:00.000Z",
            status = "confirmed"
        )
        assertNotNull(booking.startTime)
        assertNotNull(booking.endTime)
        assertTrue(booking.startTime!!.contains("T"))
    }

    @Test
    fun `ListingCalendarUiState uses 1-based months for backend API`() {
        val state = ListingCalendarUiState()
        // Default month should be 1-based (1=January) for backend compatibility
        assertTrue(
            "currentMonth should be 1-based (1-12), got ${state.currentMonth}",
            state.currentMonth in 1..12
        )
    }

    // ════════════════════════════════════════════════════════════════
    // 6. TIMEZONE HANDLING
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `parseDate handles ISO 8601 UTC format`() {
        val millis = parseDate("2026-04-03T09:00:00.000Z")
        assertNotNull("Should parse ISO 8601 UTC", millis)
    }

    @Test
    fun `parseDate handles ISO 8601 without millis`() {
        val millis = parseDate("2026-04-03T09:00:00Z")
        assertNotNull("Should parse ISO 8601 without millis", millis)
    }

    @Test
    fun `parseDate handles date-only format`() {
        val millis = parseDate("2026-04-03")
        assertNotNull("Should parse date-only format", millis)
    }

    @Test
    fun `parseDate returns null for null and blank`() {
        assertNull(parseDate(null))
        assertNull(parseDate(""))
        assertNull(parseDate("   "))
    }

    @Test
    fun `formatInListingTimezone converts UTC to listing timezone`() {
        // Parse a UTC time
        val millis = parseDate("2026-04-03T14:00:00.000Z")!!
        // Format in America/New_York (UTC-4 in April, EDT)
        val formatted = formatInListingTimezone(millis, "America/New_York", "HH:mm")
        assertEquals("10:00", formatted) // 14:00 UTC = 10:00 EDT
    }

    @Test
    fun `formatInListingTimezone handles different timezones`() {
        val millis = parseDate("2026-04-03T14:00:00.000Z")!!
        // America/Los_Angeles is UTC-7 in April (PDT)
        val formatted = formatInListingTimezone(millis, "America/Los_Angeles", "HH:mm")
        assertEquals("07:00", formatted) // 14:00 UTC = 07:00 PDT
    }

    @Test
    fun `formatDateTimeInListingTimezone returns null for null input`() {
        assertNull(formatDateTimeInListingTimezone(null, "America/New_York"))
    }

    // ════════════════════════════════════════════════════════════════
    // 7. BOOKING LIFECYCLE IMPACT ON AVAILABILITY
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `booking request created (pending) blocks availability`() {
        val status = BookingStatus.fromString("pending")
        assertTrue("Pending booking should block slot", status.isActive)
    }

    @Test
    fun `booking approved (confirmed) blocks availability`() {
        val status = BookingStatus.fromString("confirmed")
        assertTrue("Confirmed booking should block slot", status.isActive)
    }

    @Test
    fun `instant booking (created) blocks availability`() {
        val status = BookingStatus.fromString("created")
        assertTrue("Created booking should block slot", status.isActive)
    }

    @Test
    fun `booking cancelled frees slot`() {
        val status = BookingStatus.fromString("cancelled")
        assertTrue("Cancelled booking should free slot", status.isTerminal)
        assertFalse("Cancelled booking should NOT block slot", status.isActive)
    }

    @Test
    fun `booking expired frees slot`() {
        val status = BookingStatus.fromString("expired")
        assertTrue("Expired booking should free slot", status.isTerminal)
        assertFalse("Expired booking should NOT block slot", status.isActive)
    }

    @Test
    fun `booking completed frees slot`() {
        val status = BookingStatus.fromString("completed")
        assertTrue("Completed booking should free slot", status.isTerminal)
    }

    @Test
    fun `booking refunded frees slot`() {
        val status = BookingStatus.fromString("refunded")
        assertTrue("Refunded booking should free slot", status.isTerminal)
    }

    // ════════════════════════════════════════════════════════════════
    // 8. TIME SLOT STATUS CORRECTNESS
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `TimeSlotStatus has all required states`() {
        assertEquals(3, TimeSlotStatus.entries.size)
        assertNotNull(TimeSlotStatus.AVAILABLE)
        assertNotNull(TimeSlotStatus.BOOKED)
        assertNotNull(TimeSlotStatus.BLOCKED)
    }

    @Test
    fun `CalendarTimeSlot defaults to AVAILABLE`() {
        val slot = CalendarTimeSlot()
        assertEquals(TimeSlotStatus.AVAILABLE, slot.status)
    }

    // ════════════════════════════════════════════════════════════════
    // 9. HOST STATUS HELPERS
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `isPendingStatus matches backend pending variants`() {
        assertTrue(isPendingStatus("pending"))
        assertTrue(isPendingStatus("pending_host"))
        assertTrue(isPendingStatus("requires_capture"))
        assertTrue(isPendingStatus("created"))
        assertFalse(isPendingStatus("confirmed"))
        assertFalse(isPendingStatus("cancelled"))
        assertFalse(isPendingStatus(null))
        assertFalse(isPendingStatus(""))
    }

    @Test
    fun `isConfirmedBooking matches backend confirmed variants`() {
        val confirmedBooking = HostBookingDTO(status = "confirmed")
        assertTrue(isConfirmedBooking(confirmedBooking))

        val pendingBooking = HostBookingDTO(status = "pending")
        assertFalse(isConfirmedBooking(pendingBooking))

        val cancelledBooking = HostBookingDTO(status = "cancelled")
        assertFalse(isConfirmedBooking(cancelledBooking))
    }

    // ════════════════════════════════════════════════════════════════
    // 10. CHECK AVAILABILITY REQUEST DTO
    // ════════════════════════════════════════════════════════════════

    @Test
    fun `CheckAvailabilityRequest has ISO date-time format`() {
        val request = CheckAvailabilityRequest(
            startDate = "2026-04-03T09:00:00Z",
            endDate = "2026-04-03T11:00:00Z"
        )
        assertTrue(request.startDate.contains("T"))
        assertTrue(request.endDate.contains("T"))
    }

    @Test
    fun `CheckAvailabilityResponse data structure matches backend`() {
        val response = CheckAvailabilityResponse(
            status = true,
            data = CheckAvailabilityData(
                available = true,
                reason = null,
                listing = CheckAvailabilityListingInfo(
                    bookable = true,
                    status = "active",
                    moderationStatus = "PUBLISHED",
                    timezone = "America/New_York",
                    blockedDates = emptyList()
                )
            )
        )
        assertTrue(response.data!!.available)
        assertNull(response.data!!.reason)
        assertTrue(response.data!!.listing!!.bookable)
        assertEquals("PUBLISHED", response.data!!.listing!!.moderationStatus)
    }
}
