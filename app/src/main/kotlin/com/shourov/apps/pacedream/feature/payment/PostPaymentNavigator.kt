package com.shourov.apps.pacedream.feature.payment

import com.shourov.apps.pacedream.navigation.DashboardDestination
import com.shourov.apps.pacedream.navigation.TabRouter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * Post-payment navigation matching iOS PostPaymentNavigator.swift.
 *
 * After a successful Stripe payment:
 * 1. Dismiss all payment sheets/views
 * 2. Switch to Bookings tab
 * 3. Navigate to BookingDetailScreen for the new booking
 *
 * Usage:
 *   PostPaymentNavigator.navigateToBookingDetail("booking_123")
 *
 * The DashboardNavigation composable should collect [navigationEvents] to
 * perform the actual navigation.
 *
 * Note: [navigateToBookingDetail] must be called from a coroutine scope
 * managed by the caller (e.g. viewModelScope) — never from a fire-and-forget
 * CoroutineScope that leaks beyond the lifecycle of the hosting component.
 */
object PostPaymentNavigator {

    private val _navigationEvents = MutableSharedFlow<PostPaymentEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<PostPaymentEvent> = _navigationEvents.asSharedFlow()

    /**
     * Trigger post-payment navigation to booking detail.
     * This matches the iOS behavior of posting PD_DidCompletePayment notification.
     *
     * Must be called from a lifecycle-aware coroutine scope (e.g. viewModelScope).
     */
    suspend fun navigateToBookingDetail(bookingId: String?) {
        Timber.d("PostPaymentNavigator: navigating to booking detail bookingId=$bookingId")

        // Switch to bookings tab first
        TabRouter.switchTo(DashboardDestination.BOOKINGS)

        // Then navigate to detail if we have an ID
        if (!bookingId.isNullOrBlank()) {
            _navigationEvents.emit(
                PostPaymentEvent.NavigateToBookingDetail(bookingId)
            )
        }
    }
}

sealed class PostPaymentEvent {
    data class NavigateToBookingDetail(val bookingId: String) : PostPaymentEvent()
}
