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

package com.shourov.apps.pacedream.feature.chat.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.MessageRepository
import com.shourov.apps.pacedream.model.MessageAttachment
import com.shourov.apps.pacedream.model.MessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authSession: com.shourov.apps.pacedream.core.network.auth.AuthSession,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var loadMessagesJob: Job? = null

    fun loadMessages(chatId: String) {
        loadMessagesJob?.cancel()
        loadMessagesJob = viewModelScope.launch {
            val resolvedUserId = authSession.currentUserId ?: "unknown"
            _uiState.value = _uiState.value.copy(isLoading = true, chatId = chatId, currentUserId = resolvedUserId)

            // Check attachment status
            launch {
                when (val result = messageRepository.getAttachmentStatus(chatId)) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(attachmentsEnabled = result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(attachmentsEnabled = false)
                    }
                }
            }

            // Mark messages in this chat as read
            launch {
                messageRepository.markChatAsReadOnServer(chatId)
            }

            messageRepository.getChatMessages(chatId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        // Derive otherUserId from message history if not yet set
                        val currentOther = _uiState.value.otherUserId
                        val derivedOther = if (currentOther.isBlank()) {
                            result.data.firstOrNull { it.senderId != resolvedUserId }?.senderId
                                ?: result.data.firstOrNull()?.receiverId
                                ?: ""
                        } else currentOther

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = result.data,
                            otherUserId = derivedOther,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                }
            }
        }
    }

    fun onMessageChange(message: String) {
        _uiState.value = _uiState.value.copy(newMessage = message)
    }

    fun addPendingPhotos(uris: List<Uri>) {
        val current = _uiState.value.pendingPhotos
        val remaining = MAX_PHOTOS_PER_MESSAGE - current.size
        if (remaining <= 0) {
            _uiState.value = _uiState.value.copy(uploadError = "Maximum $MAX_PHOTOS_PER_MESSAGE photos per message.")
            return
        }
        val newPhotos = uris.take(remaining).map { PendingPhoto(uri = it) }
        _uiState.value = _uiState.value.copy(
            pendingPhotos = current + newPhotos,
            uploadError = null
        )
    }

    fun removePendingPhoto(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            pendingPhotos = _uiState.value.pendingPhotos.filter { it.uri != uri }
        )
    }

    fun sendMessage() {
        val snapshot = _uiState.value
        val message = snapshot.newMessage.trim()
        val photos = snapshot.pendingPhotos

        if (message.isEmpty() && photos.isEmpty()) return
        // Prevent duplicate sends while a request is in flight
        if (snapshot.isSending || snapshot.isUploading) return
        // Prevent rapid duplicate text sends (same content within 2 seconds)
        if (photos.isEmpty() && message == lastSentText && System.currentTimeMillis() - lastSentTimestamp < DUPLICATE_SEND_GUARD_MS) return

        viewModelScope.launch {
            if (photos.isNotEmpty()) {
                sendMediaMessage(message, photos)
            } else {
                sendTextMessage(message)
            }
        }
    }

    private var lastSentText: String = ""
    private var lastSentTimestamp: Long = 0L

    private suspend fun sendTextMessage(text: String) {
        _uiState.value = _uiState.value.copy(isSending = true, uploadError = null)

        val snapshot = _uiState.value
        val messageModel = MessageModel(
            id = UUID.randomUUID().toString(),
            chatId = snapshot.chatId,
            senderId = snapshot.currentUserId,
            receiverId = snapshot.otherUserId,
            content = text,
            messageType = "TEXT",
            isRead = false,
            timestamp = nowIso(),
            createdAt = nowIso(),
            status = "sending"
        )

        // Track for duplicate guard
        lastSentText = text
        lastSentTimestamp = System.currentTimeMillis()

        // Optimistic insert
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + messageModel,
            newMessage = "",
            isSending = true
        )

        when (val result = messageRepository.sendMessage(snapshot.chatId, messageModel)) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(isSending = false)
            }
            is Result.Error -> {
                val errorMsg = mapMessageError(result.exception)
                // Mark message as failed
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    messages = _uiState.value.messages.map {
                        if (it.id == messageModel.id) it.copy(status = "failed") else it
                    },
                    error = errorMsg
                )
            }
        }
    }

    /**
     * Map message send errors, surfacing rate-limit, spam blocks, and
     * network/SSL failures (e.g. caused by device-level SPU/KeyMint issues).
     */
    private fun mapMessageError(exception: Throwable): String {
        val msg = exception.message ?: "Failed to send message"
        val lower = msg.lowercase()
        return when {
            lower.contains("secure connection failed") || lower.contains("ssl") ->
                "Secure connection failed. Please restart your device and try again."
            lower.contains("timed out") || lower.contains("timeout") ->
                "Connection timed out. Please check your internet and try again."
            lower.contains("no internet") || lower.contains("unable to resolve") || lower.contains("unknownhost") ->
                "No internet connection. Please check your network and try again."
            lower.contains("unable to reach") || lower.contains("connect") && lower.contains("fail") ->
                "Unable to reach the server. Please check your connection and try again."
            lower.contains("rate") || lower.contains("too many") || lower.contains("429") ->
                "You're sending messages too quickly. Please wait a moment."
            lower.contains("spam") || lower.contains("blocked") || lower.contains("fraud") ->
                "This message was blocked. Please contact support if you believe this is an error."
            lower.contains("restricted") || lower.contains("suspended") ->
                "Your account is currently restricted from sending messages."
            else -> msg
        }
    }

    private suspend fun sendMediaMessage(text: String, photos: List<PendingPhoto>) {
        _uiState.value = _uiState.value.copy(
            isUploading = true,
            uploadProgress = 0f,
            uploadError = null,
            pendingPhotos = emptyList(),
            newMessage = ""
        )

        // Optimistic placeholder message
        val tempId = UUID.randomUUID().toString()
        val placeholderMessage = MessageModel(
            id = tempId,
            chatId = _uiState.value.chatId,
            senderId = _uiState.value.currentUserId,
            content = text.ifBlank { "Sending ${photos.size} photo${if (photos.size > 1) "s" else ""}..." },
            messageType = "IMAGE",
            status = "sending",
            timestamp = nowIso(),
            createdAt = nowIso()
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + placeholderMessage
        )

        when (val result = messageRepository.uploadChatMedia(
            context = appContext,
            threadId = _uiState.value.chatId,
            imageUris = photos.map { it.uri },
            text = text.ifBlank { null }
        )) {
            is Result.Success -> {
                // Replace placeholder with real message
                val realMessage = result.data
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadProgress = 1f,
                    messages = _uiState.value.messages.map {
                        if (it.id == tempId) realMessage else it
                    }
                )
            }
            is Result.Error -> {
                // Mark placeholder as failed and restore photos for retry
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadProgress = 0f,
                    uploadError = result.exception.message,
                    pendingPhotos = photos,
                    messages = _uiState.value.messages.map {
                        if (it.id == tempId) it.copy(status = "failed") else it
                    }
                )
            }
        }
    }

    fun retryFailedMessage(messageId: String) {
        val failedMessage = _uiState.value.messages.find { it.id == messageId && it.status == "failed" } ?: return
        // Remove the failed message and re-send
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.filter { it.id != messageId }
        )

        viewModelScope.launch {
            if (failedMessage.hasImageAttachments || _uiState.value.pendingPhotos.isNotEmpty()) {
                sendMediaMessage(failedMessage.content, _uiState.value.pendingPhotos)
            } else {
                _uiState.value = _uiState.value.copy(newMessage = failedMessage.content)
                sendTextMessage(failedMessage.content)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, uploadError = null)
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    companion object {
        const val MAX_PHOTOS_PER_MESSAGE = 10
        /** Minimum interval in ms before the same text can be sent again. */
        private const val DUPLICATE_SEND_GUARD_MS = 2000L
    }
}

data class PendingPhoto(
    val uri: Uri,
    val id: String = UUID.randomUUID().toString()
)

data class ChatUiState(
    val isLoading: Boolean = false,
    val chatId: String = "",
    val currentUserId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "Other User",
    val otherUserAvatar: String? = null,
    val messages: List<MessageModel> = emptyList(),
    val newMessage: String = "",
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadError: String? = null,
    val attachmentsEnabled: Boolean = false,
    val pendingPhotos: List<PendingPhoto> = emptyList(),
    val error: String? = null
) {
    val canSend: Boolean
        get() = (newMessage.isNotBlank() || pendingPhotos.isNotEmpty()) && !isSending && !isUploading
}
