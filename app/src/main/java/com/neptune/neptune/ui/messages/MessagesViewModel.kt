package com.neptune.neptune.ui.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.neptune.neptune.model.messages.Message
import com.neptune.neptune.model.messages.MessageRepository
import com.neptune.neptune.model.messages.MessageRepositoryProvider
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and operations related to the messages. This has been written
 * with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class MessagesViewModel(
    private val otherUserId: String,
    private val currentUserId: String,
    private val messageRepo: MessageRepository = MessageRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
) : ViewModel() {

  private val _messages = MutableStateFlow(emptyList<Message>())
  val messages: StateFlow<List<Message>> = _messages.asStateFlow()

  private val _otherUsername = MutableStateFlow("Loading..")
  val otherUsername: StateFlow<String> = _otherUsername.asStateFlow()
  private val _otherAvatar = MutableStateFlow<String?>(null)
  val otherAvatar: StateFlow<String?> = _otherAvatar.asStateFlow()
  private val _isOnline = MutableStateFlow(false)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val conversationId = listOf(currentUserId, otherUserId).sorted().joinToString("_")

  init {
    observeOtherUserProfile()
    observeOtherUserOnline()
    observeConversation()
  }

  fun sendMessage(text: String) {
    val msg =
        Message(
            id = System.currentTimeMillis().toString(),
            authorId = currentUserId,
            text = text,
            timestamp = Timestamp.now())

    _messages.value = _messages.value + msg

    viewModelScope.launch {
      try {
        messageRepo.sendMessage(conversationId, msg)
      } catch (e: Exception) {
        Log.e("MessagesViewModel", "Failed to send message: ${e.message}")
      }
    }
  }

  private fun observeConversation() {
    messageRepo
        .observeMessages(conversationId)
        .onEach { _messages.value = it }
        .launchIn(viewModelScope)
  }

  private fun observeOtherUserProfile() {
    viewModelScope.launch {
      try {
        val profile = profileRepo.getProfile(otherUserId)
        if (profile != null) {
          _otherUsername.value = profile.username
          _otherAvatar.value = profile.avatarUrl
        }
      } catch (e: Exception) {
        Log.e("MessagesViewModel", "Failed to observe other user profile: ${e.message}")
      }
    }
  }

  private fun observeOtherUserOnline() {
    messageRepo
        .observeUserOnlineState(otherUserId)
        .onEach { online -> _isOnline.value = online }
        .launchIn(viewModelScope)
  }
}
