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

package com.shourov.apps.pacedream.signin.model

import android.net.Uri
import com.shourov.apps.pacedream.core.data.AccountSetupData
import com.shourov.apps.pacedream.core.data.UserSetupGender

data class CreateAccountData(
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val profilePicture: Uri = Uri.EMPTY,
    val dateOfBirthMillis: Long? = null,
    val gender: UserSetupGender = UserSetupGender.PREFERS_NOT_TO_SAY,
    val hobbiesNInterest: Set<String> = emptySet()
)

fun CreateAccountData.userProfileDetailsValid(): Boolean {
    return firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty()
}

fun CreateAccountData.dateOfBirthValid(): Boolean {
    if (dateOfBirthMillis == null) return false
    if (dateOfBirthMillis >= System.currentTimeMillis()) return false
    // Enforce minimum age of 13 (COPPA compliance)
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.YEAR, -13)
    return dateOfBirthMillis <= calendar.timeInMillis
}