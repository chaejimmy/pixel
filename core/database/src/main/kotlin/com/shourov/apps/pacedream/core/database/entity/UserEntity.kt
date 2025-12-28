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
import com.shourov.apps.pacedream.model.response.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val profilePic: String?,
    val roleId: Int?,
    val userId: String?,
    val emailVerified: Boolean?,
    val allowSharedBooking: Boolean?,
    val createdAt: String?,
    val dob: String?,
    val gender: String?,
    val identityVerified: Boolean?,
    val isBlocked: Boolean?,
    val mobile: String?,
    val mobileVerified: Boolean?,
    val preferredLanguage: String?,
    val rating: Int?,
    val rememberMe: Boolean?,
    val superHost: Boolean?,
    val updatedAt: String?
)

fun UserEntity.asExternalModel(): User {
    return User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        profilePic = profilePic,
        roleId = roleId,
        userId = userId,
        emailVerified = emailVerified,
        allowSharedBooking = allowSharedBooking,
        createdAt = createdAt,
        dob = dob,
        gender = gender,
        identityVerified = identityVerified,
        isBlocked = isBlocked,
        mobile = mobile,
        mobileVerified = mobileVerified,
        preferredLanguage = preferredLanguage,
        rating = rating,
        rememberMe = rememberMe,
        superHost = superHost,
        updatedAt = updatedAt,
        token = null,
        hobbiesInterests = null,
        partnerHosting = null,
        properties = null,
        reviews = null
    )
}

fun User.asEntity(): UserEntity? {
    val entityId = id ?: return null
    return UserEntity(
        id = entityId,
        firstName = firstName,
        lastName = lastName,
        profilePic = profilePic,
        roleId = roleId,
        userId = userId,
        emailVerified = emailVerified,
        allowSharedBooking = allowSharedBooking,
        createdAt = createdAt,
        dob = dob,
        gender = gender,
        identityVerified = identityVerified,
        isBlocked = isBlocked,
        mobile = mobile,
        mobileVerified = mobileVerified,
        preferredLanguage = preferredLanguage,
        rating = rating,
        rememberMe = rememberMe,
        superHost = superHost,
        updatedAt = updatedAt
    )
}
