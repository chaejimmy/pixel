package com.shourov.apps.pacedream.feature.payment

import com.shourov.apps.pacedream.navigation.BookingDestination
import com.shourov.apps.pacedream.navigation.DashboardDestination
import com.shourov.apps.pacedream.navigation.TabRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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
 */
object PostPaymentNavigator {

    private val _navigationEvents = MutableSharedFlow<PostPaymentEvent>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<PostPaymentEvent> = _navigationEvents.asSharedFlow()

    /**
     * Trigger post-payment navigation to booking detail.
     * This matches the iOS behavior of posting PD_DidCompletePayment notification.
     */
    fun navigateToBookingDetail(bookingId: String?) {
        Timber.d("PostPaymentNavigator: navigating to booking detail bookingId=$bookingId")

        CoroutineScope(Dispatchers.Main).launch {
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
}

sealed class PostPaymentEvent {
    data class NavigateToBookingDetail(val bookingId: String) : PostPaymentEvent()
}
