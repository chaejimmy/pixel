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
import com.pacedream.common.util.UserFacingErrorMapper
import com.shourov.apps.pacedream.core.common.result.Result
import com.shourov.apps.pacedream.core.data.repository.MessageRepository
import com.shourov.apps.pacedream.model.MessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authSession: com.shourov.apps.pacedream.core.network.auth.AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    fun loadChats() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val userId = authSession.currentUserId ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not signed in"
                )
                return@launch
            }

            // Fetch chat list from the server API
            try {
                val json = messageRepository.fetchChatListRaw()
                if (json == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = UserFacingErrorMapper.forLoadMessages(null)
                    )
                    return@launch
                }
                val chats = parseChatListResponse(json, userId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    chats = chats,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = UserFacingErrorMapper.forLoadMessages(e)
                )
            }
        }
    }

    private fun parseChatListResponse(
        json: com.google.gson.JsonElement?,
        currentUserId: String
    ): List<ChatItem> {
        if (json == null || !json.isJsonObject) return emptyList()
        val dataArray = json.asJsonObject.getAsJsonArray("data") ?: return emptyList()

        return dataArray.mapNotNull { element ->
            try {
                val chat = element.asJsonObject
                val chatId = chat.get("_id")?.asString ?: return@mapNotNull null
                val users = chat.getAsJsonArray("users") ?: return@mapNotNull null
                val latestMessage = chat.getAsJsonObject("latestMessage")

                val otherUser = users.firstOrNull { user ->
                    user.asJsonObject.get("_id")?.asString != currentUserId
                }?.asJsonObject

                val otherUserName = if (otherUser != null) {
                    val first = otherUser.get("first_name")?.asString ?: ""
                    val last = otherUser.get("last_name")?.asString ?: ""
                    "$first $last".trim().ifBlank { "Unknown" }
                } else "Unknown"

                ChatItem(
                    chatId = chatId,
                    otherUserId = otherUser?.get("_id")?.asString ?: "",
                    otherUserName = otherUserName,
                    otherUserAvatar = otherUser?.get("profilePic")?.asString?.takeIf { it.isNotBlank() },
                    lastMessage = latestMessage?.get("message")?.asString ?: "",
                    lastMessageTime = latestMessage?.get("updatedAt")?.asString
                        ?: latestMessage?.get("createdAt")?.asString ?: "",
                    unreadCount = 0
                )
            } catch (_: Exception) { null }
        }.sortedByDescending { it.lastMessageTime }
    }
    
    private fun groupMessagesIntoChats(messages: List<MessageModel>, currentUserId: String): List<ChatItem> {
        val chatMap = mutableMapOf<String, MutableList<MessageModel>>()

        // Group messages by chat ID, skipping messages without a valid chatId
        messages.forEach { message ->
            val chatId = message.chatId
            if (!chatId.isNullOrBlank()) {
                chatMap.getOrPut(chatId) { mutableListOf() }.add(message)
            }
        }
        
        // Convert to ChatItem list
        return chatMap.map { (chatId, messageList) ->
            val sortedMessages = messageList.sortedBy { it.timestamp }
            val lastMessage = sortedMessages.lastOrNull()
            val otherUserId = if (lastMessage?.senderId == currentUserId) {
                lastMessage?.receiverId ?: ""
            } else {
                lastMessage?.senderId ?: ""
            }

            // Website parity: extract real user name from message data when available
            val otherUserName = sortedMessages
                .firstOrNull { it.senderId != currentUserId }
                ?.let { it.userName?.takeIf { n -> n.isNotBlank() } }
                ?: otherUserId.takeIf { it.isNotBlank() }
                ?: "Unknown"
            val otherUserAvatar = sortedMessages
                .firstOrNull { it.senderId != currentUserId }
                ?.profilePic?.takeIf { it.isNotBlank() }

            ChatItem(
                chatId = chatId,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                otherUserAvatar = otherUserAvatar,
                lastMessage = lastMessage?.displayText ?: "",
                lastMessageTime = lastMessage?.timestamp ?: "",
                unreadCount = messageList.count { !it.isRead && it.receiverId == currentUserId }
            )
        }.sortedByDescending { it.lastMessageTime }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatListUiState(
    val isLoading: Boolean = false,
    val chats: List<ChatItem> = emptyList(),
    val error: String? = null
)
