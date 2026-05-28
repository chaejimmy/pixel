package com.pacedream.app.feature.checkout

import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.BookingRepository
import com.shourov.apps.pacedream.core.data.repository.BookingRepository.AvailabilityCheckResult
import com.shourov.apps.pacedream.model.BookingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Narrow interface exposing the two BookingRepository operations
 * [CheckoutViewModel] needs:
 *   1. [checkAvailability] — pre-flight availability check before
 *      creating a PaymentIntent so we never charge a card for a slot
 *      that is already booked by another guest.
 *   2. [cacheBooking] — write the confirmed booking back into Room so
 *      the Bookings tab reflects it immediately without a refresh.
 *
 * Extracted so the view-model can be unit-tested without standing up
 * the full [BookingRepository] graph (which transitively pulls in
 * Retrofit / Room / network plumbing that a JVM unit test should not
 * need).
 */
interface CheckoutBookingGate {
    suspend fun checkAvailability(
        listingId: String,
        startDate: String,
        endDate: String,
    ): Result<AvailabilityCheckResult>

    suspend fun cacheBooking(booking: BookingModel)
}

@Singleton
class BookingRepositoryCheckoutGate @Inject constructor(
    private val bookingRepository: BookingRepository,
) : CheckoutBookingGate {
    override suspend fun checkAvailability(
        listingId: String,
        startDate: String,
        endDate: String,
    ): Result<AvailabilityCheckResult> =
        bookingRepository.checkAvailability(listingId, startDate, endDate)

    override suspend fun cacheBooking(booking: BookingModel) {
        bookingRepository.cacheBooking(booking)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckoutBookingGateModule {
    @Binds
    @Singleton
    abstract fun bindCheckoutBookingGate(
        impl: BookingRepositoryCheckoutGate,
    ): CheckoutBookingGate
}
