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

package com.shourov.apps.pacedream.core.database.dao

import androidx.room.*
import com.shourov.apps.pacedream.core.database.entity.BookingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE id = :bookingId")
    fun getBookingById(bookingId: String): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE userName = :userName ORDER BY checkInTime DESC")
    fun getBookingsByUserName(userName: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE bookingStatus = :status ORDER BY checkInTime DESC")
    fun getBookingsByStatus(status: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings ORDER BY checkInTime DESC")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteBookingById(bookingId: String)

    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
}
