package com.shourov.apps.pacedream.signin

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.data.AccountCreationProcess
import com.shourov.apps.pacedream.core.data.AccountSetupData
import com.shourov.apps.pacedream.core.data.AccountSetupScreenData
import com.shourov.apps.pacedream.core.data.dateOfBirthValid
import com.shourov.apps.pacedream.core.data.userProfileDetailsValid
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val HOBBIES_MAX_COUNT = 5

@HiltViewModel
class AccountSetupViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _setupError = MutableStateFlow<String?>(null)
    val setupError: StateFlow<String?> = _setupError.asStateFlow()
    private val accountSetupOrder: List<AccountCreationProcess> = listOf(
        AccountCreationProcess.PASSWORD_SETUP,
        AccountCreationProcess.USER_PROFILE_SETUP,
        AccountCreationProcess.PERSONAL_DETAILS_SETUP,
        AccountCreationProcess.PROFILE_PICTURE_SETUP,
        AccountCreationProcess.HOBBIES_N_INTERESTS_SETUP,
    )

    private var _accountSetupData by mutableStateOf(AccountSetupData())
    val accountSetupData: AccountSetupData
        get() = _accountSetupData

    private var _hobbiesNInterests by mutableStateOf(setOf<String>())
    val hobbiesNInterests: Set<String>
        get() = _hobbiesNInterests
    private var accountSetupIndex = 0

    private var _accountSetupScreenData by mutableStateOf(createAccountSetupScreenData())
    val accountSetupScreenData: AccountSetupScreenData
        get() = _accountSetupScreenData

    private var _isContinueEnabled by mutableStateOf(false)
    val isContinueEnabled
        get() = _isContinueEnabled

    fun onContinueClicked() {
        if (accountSetupIndex < accountSetupOrder.size - 1) {
            changeAccountSetupScreenData(accountSetupIndex + 1)
        }
    }

    fun onPreviousClicked() {
        if (accountSetupIndex > 0) {
            changeAccountSetupScreenData(accountSetupIndex - 1)
        }
    }

    fun onPasswordResponse(password: String, isValid: Boolean) {
        _accountSetupData = _accountSetupData.copy(password = password)
        _isContinueEnabled = getIsContinueEnabled() && isValid
    }

    fun onUserProfileResponse(firstName: String, lastName: String, isValid: Boolean) {
        _accountSetupData = _accountSetupData.copy(firstName = firstName, lastName = lastName)
        _isContinueEnabled = getIsContinueEnabled() && isValid
    }

    fun onEmailResponse(email: String) {
        _accountSetupData = _accountSetupData.copy(email = email)
    }

    fun onPhoneNumberResponse(phoneNumber: String) {
        _accountSetupData = _accountSetupData.copy(phoneNumber = phoneNumber)
    }

    fun onDateOfBirthResponse(dateOfBirth: Long) {
        _accountSetupData = _accountSetupData.copy(dateOfBirthMillis = dateOfBirth)
        _isContinueEnabled = getIsContinueEnabled()
    }

    fun onProfilePictureResponse(profilePicture: Uri) {
        _accountSetupData = _accountSetupData.copy(profilePicture = profilePicture)
        _isContinueEnabled = getIsContinueEnabled()
    }

    fun onHobbiesNInterestsToggle(hobbiesNInterest: String, isChecked: Boolean) {
        _hobbiesNInterests = if (isChecked) {
            _hobbiesNInterests + hobbiesNInterest
        } else {
            _hobbiesNInterests - hobbiesNInterest
        }
        _accountSetupData = _accountSetupData.copy(hobbiesNInterest = _hobbiesNInterests)
    }

    private fun changeAccountSetupScreenData(newIndex: Int) {
        accountSetupIndex = newIndex
        _isContinueEnabled = getIsContinueEnabled()
        _accountSetupScreenData = createAccountSetupScreenData()
    }

    private fun getIsContinueEnabled(): Boolean {
        return when (accountSetupOrder[accountSetupIndex]) {
            AccountCreationProcess.PASSWORD_SETUP -> _accountSetupData.password.isNotEmpty()

            AccountCreationProcess.USER_PROFILE_SETUP -> _accountSetupData.userProfileDetailsValid()

            AccountCreationProcess.PERSONAL_DETAILS_SETUP -> _accountSetupData.dateOfBirthValid()

            AccountCreationProcess.PROFILE_PICTURE_SETUP -> true

            AccountCreationProcess.HOBBIES_N_INTERESTS_SETUP -> _hobbiesNInterests.size <= HOBBIES_MAX_COUNT
        }
    }

    private fun createAccountSetupScreenData(): AccountSetupScreenData {
        return AccountSetupScreenData(
            processIndex = accountSetupIndex,
            totalProcessCount = accountSetupOrder.size,
            accountCreationProcess = accountSetupOrder[accountSetupIndex],
            shouldShowPreviousButton = accountSetupIndex > 0,
            shouldShowDoneButton = accountSetupIndex == accountSetupOrder.size - 1,
        )
    }

    /**
     * Submit account setup data to backend
     * Called when user clicks "Done" on the last setup step
     */
    fun onDoneClicked(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val data = _accountSetupData

        _isSubmitting.value = true
        _setupError.value = null

        viewModelScope.launch {
            val result = authSession.updateProfile(
                firstName = data.firstName,
                lastName = data.lastName,
                dateOfBirth = data.dateOfBirthMillis,
                interests = data.hobbiesNInterest,
            )
            result.fold(
                onSuccess = {
                    _isSubmitting.value = false
                    _setupComplete.value = true
                    Timber.d("Account setup completed successfully")
                    onSuccess()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "Failed to save profile"
                    _isSubmitting.value = false
                    _setupError.value = errorMessage
                    Timber.e("Account setup failed: $errorMessage")
                    onError(errorMessage)
                }
            )
        }
    }
}