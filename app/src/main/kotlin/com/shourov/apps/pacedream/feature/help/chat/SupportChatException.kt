package com.shourov.apps.pacedream.feature.help.chat

/**
 * Domain-level error. Never surfaces raw backend strings to the UI.
 */
sealed class SupportChatException(message: String) : Exception(message) {
    data object Offline : SupportChatException("You appear to be offline. Please check your connection.")
    data object TooFast : SupportChatException("You're sending messages a bit too quickly. Please wait a moment.")
    data object SessionExpired : SupportChatException("Please sign in again to continue.")
    data object SessionMissing : SupportChatException("We couldn't find this conversation. Please start a new one.")
    data object Restricted : SupportChatException("We can't reach support right now. Please try again later.")
    data object Generic : SupportChatException("Something went wrong. Please try again.")
}
