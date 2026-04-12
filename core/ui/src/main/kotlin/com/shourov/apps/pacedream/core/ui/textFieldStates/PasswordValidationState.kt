package com.shourov.apps.pacedream.core.ui.textFieldStates

import com.shourov.apps.pacedream.core.data.util.isPasswordValid
import com.shourov.apps.pacedream.core.data.util.passwordAndConfirmationValid
import com.shourov.apps.pacedream.core.data.util.passwordConfirmationError
import com.shourov.apps.pacedream.core.data.util.passwordValidationError

class PasswordValidationState : GenericTextFieldState<String>(
    validator = { isPasswordValid(it) },
    errorFor = { passwordValidationError(it) },
    initialText = "",
)

class ConfirmPasswordState(
    private val newPassword: String,
) : GenericTextFieldState<String>(
    validator = { passwordAndConfirmationValid(newPassword, it) },
    errorFor = { passwordConfirmationError() },
    initialText = "",
)

/**
 * A confirm-password state that reads the current password dynamically via a lambda,
 * instead of capturing a stale snapshot at creation time.
 */
class LiveConfirmPasswordState(
    private val newPasswordProvider: () -> String,
) : GenericTextFieldState<String>(
    validator = { true }, // overridden below
    errorFor = { passwordConfirmationError() },
    initialText = "",
) {
    override val isValid: Boolean
        get() = passwordAndConfirmationValid(newPasswordProvider(), text)
}