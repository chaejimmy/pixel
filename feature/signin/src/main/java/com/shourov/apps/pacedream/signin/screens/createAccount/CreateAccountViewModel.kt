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

package com.shourov.apps.pacedream.signin.screens.createAccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.signin.model.AccountCreationScreenState
import com.shourov.apps.pacedream.signin.model.CreateAccountComponents
import com.shourov.apps.pacedream.signin.model.CreateAccountComponents.HOBBIES_AND_INTERESTS
import com.shourov.apps.pacedream.signin.model.CreateAccountData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {
    private var createAccountComponentsOrder = listOf<CreateAccountComponents>(
        CreateAccountComponents.START_EMAIL_PHONE,
        CreateAccountComponents.PASSWORD,
        CreateAccountComponents.NAME_DETAILS,
        CreateAccountComponents.DATE_OF_BIRTH,
        CreateAccountComponents.PROFILE_PICTURE_SETUP,
        CreateAccountComponents.HOBBIES_AND_INTERESTS,
    )

    private var componentIndex = 0

    var toastMessage  = mutableStateOf("")

    private var _createAccountData by mutableStateOf(CreateAccountData())
    val createAccountData: CreateAccountData
        get() = _createAccountData

    private var _accountCreationScreenState by mutableStateOf(
        AccountCreationScreenState(
            currentComponent = createAccountComponentsOrder[componentIndex].name,
            prevComponent = "",
            tittle = createAccountComponentsOrder[componentIndex].title
        ),
    )
    val accountCreationScreenState: AccountCreationScreenState
        get() = _accountCreationScreenState

    fun setCreateAccountData(createAccountData: CreateAccountData){
        _createAccountData = createAccountData.copy()
    }

    fun onPreviousClicked() {
        if (componentIndex != 0) {
            componentIndex -= 1
            _accountCreationScreenState = _accountCreationScreenState.copy(
                currentComponent = createAccountComponentsOrder[componentIndex].name,
                prevComponent = createAccountComponentsOrder[componentIndex + 1].name,
                tittle = createAccountComponentsOrder[componentIndex].title,
                showPreviousButton = componentIndex != 0,
                showDoneButton = componentIndex == createAccountComponentsOrder.size-1
            )
        }
    }

    fun onContinueClicked() {
        if (componentIndex < createAccountComponentsOrder.size) {
            //update the data first


            componentIndex += 1
            _accountCreationScreenState = _accountCreationScreenState.copy(
                currentComponent = createAccountComponentsOrder[componentIndex].name,
                prevComponent = createAccountComponentsOrder[componentIndex - 1].name,
                tittle = createAccountComponentsOrder[componentIndex].title,
                showPreviousButton = componentIndex != 0,
                showDoneButton = componentIndex == createAccountComponentsOrder.size-1

            )

            //update the data

        }
    }

    fun onDoneClicked() {
        viewModelScope.launch {
            val data = _createAccountData
            val result = authSession.updateProfile(
                firstName = data.firstName,
                lastName = data.lastName,
                dateOfBirth = null,
                interests = emptySet(),
            )
            result.fold(
                onSuccess = {
                    Timber.d("Account creation profile update succeeded")
                    toastMessage.value = "Account created successfully"
                },
                onFailure = { error ->
                    Timber.e("Account creation profile update failed: ${error.message}")
                    toastMessage.value = error.message ?: "Failed to create account"
                }
            )
        }
    }
}