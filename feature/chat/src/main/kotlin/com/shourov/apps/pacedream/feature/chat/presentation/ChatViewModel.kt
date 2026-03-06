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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.MessageRepository
import com.shourov.apps.pacedream.model.MessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authSession: com.shourov.apps.pacedream.core.network.auth.AuthSession
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            val resolvedUserId = authSession.currentUserId ?: "unknown"
            _uiState.value = _uiState.value.copy(isLoading = true, chatId = chatId, currentUserId = resolvedUserId)
            
            messageRepository.getChatMessages(chatId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = result.data,
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
    
    fun sendMessage() {
        val message = _uiState.value.newMessage.trim()

        if (message.isEmpty()) return

        viewModelScope.launch {
            val snapshot = _uiState.value
            _uiState.value = snapshot.copy(isSending = true)

            val messageModel = MessageModel(
                id = UUID.randomUUID().toString(),
                chatId = snapshot.chatId,
                senderId = snapshot.currentUserId,
                receiverId = snapshot.otherUserId,
                content = message,
                messageType = "TEXT",
                attachmentUrl = null,
                isRead = false,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            )

            when (val result = messageRepository.sendMessage(snapshot.chatId, messageModel)) {
                is Result.Success -> {
                    // Use latest state to avoid overwriting concurrent mutations
                    _uiState.value = _uiState.value.copy(
                        newMessage = "",
                        isSending = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.exception.message
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val chatId: String = "",
    val currentUserId: String = "", // Resolved from AuthSession when loading messages
    val otherUserId: String = "",
    val otherUserName: String = "Other User", // This should come from user data
    val otherUserAvatar: String? = null, // This should come from user data
    val messages: List<MessageModel> = emptyList(),
    val newMessage: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)
