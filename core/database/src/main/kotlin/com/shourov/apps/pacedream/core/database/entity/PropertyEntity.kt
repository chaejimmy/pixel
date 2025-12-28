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
import com.shourov.apps.pacedream.model.response.home.rooms.Result

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey
    val id: String,
    val name: String?,
    val description: String?,
    val propertyType: String?,
    val locationJson: String?, // JSON string for location data
    val imagesJson: String?, // JSON string for image URLs
    val amenitiesJson: String?, // JSON string for amenities
    val rating: Int?,
    val hostId: String?,
    val roomType: String?,
    val status: Boolean?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * Convert PropertyEntity to Result model.
 * Note: Complex nested objects need JSON parsing for full conversion.
 */
fun PropertyEntity.asExternalModel(): Result {
    return Result(
        __v = null,
        _id = id,
        additional_details = null,
        amenities = null, // Would need JSON parsing
        createdAt = createdAt,
        description = description,
        dynamic_price = null,
        facilities = null,
        faq = null,
        guest_details = null,
        host_id = hostId,
        ideal_renters = null,
        images = null, // Would need JSON parsing
        isDeleted = null,
        location = null, // Would need JSON parsing
        name = name,
        property_type = propertyType,
        rating = rating,
        room_details = null,
        room_type = roomType,
        rules = null,
        status = status,
        updatedAt = updatedAt
    )
}

/**
 * Convert Result model to PropertyEntity.
 */
fun Result.asEntity(): PropertyEntity? {
    val entityId = _id ?: return null
    return PropertyEntity(
        id = entityId,
        name = name,
        description = description,
        propertyType = property_type,
        locationJson = null, // Would need JSON serialization
        imagesJson = null, // Would need JSON serialization
        amenitiesJson = null, // Would need JSON serialization
        rating = rating,
        hostId = host_id,
        roomType = room_type,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
