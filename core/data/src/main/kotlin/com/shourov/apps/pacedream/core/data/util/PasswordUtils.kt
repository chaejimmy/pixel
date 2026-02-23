package com.shourov.apps.pacedream.core.data.util

const val PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@\$!%*?&]{8,}\$"
const val PASSWORD_MIN_LENGTH = 8

fun passwordAndConfirmationValid(password: String, confirmedPassword: String): Boolean {
    return isPasswordValid(password) && password == confirmedPassword
}

fun isPasswordValid(password: String): Boolean {
    return PASSWORD_PATTERN.toRegex().matches(password)
}

fun passwordValidationError(password: String): String {
    if (!password.matches(Regex(PASSWORD_PATTERN))) {
        val errorMessage = StringBuilder()

        if (!password.matches(Regex(".*[A-Za-z].*"))) {
            errorMessage.append("- Include at least one letter\n")
        }
        if (!password.matches(Regex(".*\\d.*"))) {
            errorMessage.append("- Include at least one digit\n")
        }
        if (password.contains("[^A-Za-z0-9@\$!%*?&]".toRegex())) {
            errorMessage.append("- Only letters, digits, and @\$!%*?& are allowed\n")
        }
        if (password.length < PASSWORD_MIN_LENGTH) {
            errorMessage.append("- Be at least $PASSWORD_MIN_LENGTH characters long\n")
        }
//        if (password.length > 10) {
//            errorMessage.append("- Should not be more than 10 characters \n")
//        }

        return errorMessage.toString()
    }
    return ""
}

fun passwordConfirmationError(): String {
    return "Passwords don't match"
}