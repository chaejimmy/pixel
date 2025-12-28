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

package com.shourov.apps.pacedream.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shourov.apps.pacedream.model.BookingModel

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey
    val id: String,
    val userProfilePic: Int?,
    val userName: String?,
    val checkOutTime: String?,
    val checkInTime: String?,
    val bookingStatus: String?,
    val price: String?,
    // Additional fields to match BookingModel
    val propertyImage: String?,
    val propertyName: String?,
    val hostName: String?,
    val currency: String?,
    val totalPrice: Double?,
    val startDate: String?,
    val endDate: String?,
    val status: String?
)

fun BookingEntity.asExternalModel(): BookingModel {
    return BookingModel(
        id = id,
        userProfilePic = userProfilePic,
        userName = userName,
        checkOutTime = checkOutTime,
        checkInTime = checkInTime,
        bookingStatus = bookingStatus,
        price = price,
        propertyImage = propertyImage,
        propertyName = propertyName ?: "",
        hostName = hostName ?: "",
        currency = currency ?: "USD",
        totalPrice = totalPrice ?: 0.0,
        startDate = startDate ?: "",
        endDate = endDate ?: "",
        status = status ?: "PENDING"
    )
}

fun BookingModel.asEntity(): BookingEntity {
    return BookingEntity(
        id = id,
        userProfilePic = userProfilePic,
        userName = userName,
        checkOutTime = checkOutTime,
        checkInTime = checkInTime,
        bookingStatus = bookingStatus,
        price = price,
        propertyImage = propertyImage,
        propertyName = propertyName,
        hostName = hostName,
        currency = currency,
        totalPrice = totalPrice,
        startDate = startDate,
        endDate = endDate,
        status = status
    )
}
