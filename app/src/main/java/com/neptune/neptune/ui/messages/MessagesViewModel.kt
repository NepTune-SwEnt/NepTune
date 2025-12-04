package com.neptune.neptune.ui.messages

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.neptune.neptune.model.messages.Messages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing the state and operations related to the messages. This has been written
 * with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class MessagesViewModel(
    private val otherUserId: String,
    private val initialMessages: List<Messages> = emptyList() /*For testing only*/
) : ViewModel() {

  private val _messages = MutableStateFlow(initialMessages)
  val messages: StateFlow<List<Messages>> = _messages.asStateFlow()

  // TODO Replace with repo calls
  private val _otherUsername = MutableStateFlow("Test1")
  val otherUsername: StateFlow<String> = _otherUsername.asStateFlow()
  private val _otherAvatar = MutableStateFlow<String?>(null)
  val otherAvatar: StateFlow<String?> = _otherAvatar.asStateFlow()
  private val _isOnline = MutableStateFlow(false)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  val currentUserId = "ME" // TODO load from FirebaseAuth

  init {
    if (initialMessages.isEmpty()) {
      loadFakeData(otherUserId)
    }
  }

  private fun loadConversation() {
    // TODO repository.loadMessages(otherUserId) -> load conv from repo
  }

  fun sendMessage(text: String) {
    val msg =
        Messages(
            id = System.currentTimeMillis().toString(),
            authorId = currentUserId,
            text = text,
            timestamp = Timestamp.now())

    _messages.value = _messages.value + msg

    // TODO repository.sendMessage(otherUserId, msg)

  }

  private fun loadFakeData(uid: String) {

    val bleh = "BLEH\uD83D\uDE1D"
    when (uid) {
      "u1" -> {
        _otherUsername.value = "test1"
        _isOnline.value = true

        _messages.value =
            listOf(
                Messages("1", uid, "Byebye Sweetie Banana", Timestamp.now()),
                Messages("2", currentUserId, "Mikkaaaa", Timestamp.now()),
            )
      }
      "u2" -> {
        _otherUsername.value = "test2"
        _isOnline.value = false

        _messages.value =
            listOf(
                Messages("10", uid, bleh, Timestamp.now()),
                Messages("11", currentUserId, bleh, Timestamp.now()),
                Messages("12", uid, bleh, Timestamp.now()))
      }
      "u3" -> {
        _otherUsername.value = "test3"
        _isOnline.value = true

        _messages.value =
            listOf(
                Messages("20", currentUserId, "21", Timestamp.now()),
                Messages("21", uid, "Banana", Timestamp.now()))
      }
      else -> {
        _otherUsername.value = "Unknown"
        _messages.value = emptyList()
      }
    }
  }
}
