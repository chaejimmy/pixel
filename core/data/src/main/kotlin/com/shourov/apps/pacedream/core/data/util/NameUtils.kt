package com.shourov.apps.pacedream.core.data.util

// name pattern: letters (including Unicode), spaces, hyphens, apostrophes, min 2
const val NAME_PATTERN = "^[\\p{L}][\\p{L} '\\-]{1,}\$"
fun isValidName(name: String): Boolean {
    return NAME_PATTERN.toRegex().matches(name.trim())
}

// error message for name
fun nameValidationError(name: String): String {
    val trimmed = name.trim()
    if (!trimmed.matches(Regex(NAME_PATTERN))) {
        val errorMessage = StringBuilder()

        if (trimmed.contains("[0-9]".toRegex())) {
            errorMessage.append("- Should not include any numbers\n")
        }
        if (trimmed.length < 2) {
            errorMessage.append("- Be at least 2 characters long\n")
        }

        return errorMessage.toString()
    }
    return ""
}