/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shourov.apps.pacedream.core.data.repository

import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.database.dao.BookingDao
import com.shourov.apps.pacedream.core.database.entity.BookingEntity
import com.shourov.apps.pacedream.core.database.entity.asEntity
import com.shourov.apps.pacedream.core.database.entity.asExternalModel
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.model.BookingModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val apiService: PaceDreamApiService,
    private val bookingDao: BookingDao
) {
    
    fun getUserBookings(userName: String): Flow<Result<List<BookingModel>>> {
        return bookingDao.getBookingsByUserName(userName).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getBookingById(bookingId: Int): Flow<Result<BookingModel?>> {
        return bookingDao.getBookingById(bookingId).map { entity ->
            Result.Success(entity?.asExternalModel())
        }
    }

    fun getBookingsByStatus(status: String): Flow<Result<List<BookingModel>>> {
        return bookingDao.getBookingsByStatus(status).map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    fun getAllBookings(): Flow<Result<List<BookingModel>>> {
        return bookingDao.getAllBookings().map { entities ->
            Result.Success(entities.map { it.asExternalModel() })
        }
    }

    suspend fun createBooking(booking: BookingModel): Result<BookingModel> {
        return try {
            val response = apiService.createBooking(booking)
            if (response.isSuccessful) {
                // Save to local database
                booking.asEntity()?.let { bookingDao.insertBooking(it) }
                Result.Success(booking)
            } else {
                Result.Error(Exception("Failed to create booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updateBooking(booking: BookingModel): Result<BookingModel> {
        return try {
            val bookingId = booking.id ?: return Result.Error(Exception("Booking ID is required"))
            val response = apiService.updateBooking(bookingId.toString(), booking)
            if (response.isSuccessful) {
                // Update local database
                booking.asEntity()?.let { bookingDao.updateBooking(it) }
                Result.Success(booking)
            } else {
                Result.Error(Exception("Failed to update booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun cancelBooking(bookingId: Int): Result<Unit> {
        return try {
            val response = apiService.cancelBooking(bookingId.toString())
            if (response.isSuccessful) {
                // Update local database
                bookingDao.deleteBookingById(bookingId)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to cancel booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun confirmBooking(bookingId: Int): Result<Unit> {
        return try {
            val response = apiService.confirmBooking(bookingId.toString())
            if (response.isSuccessful) {
                // Booking confirmed - could update local database status if needed
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to confirm booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun refreshUserBookings(userName: String): Result<Unit> {
        return try {
            val response = apiService.getUserBookings(userName)
            if (response.isSuccessful) {
                // Handle response and save to database
                // This would need to be implemented based on your API response structure
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to refresh bookings: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
