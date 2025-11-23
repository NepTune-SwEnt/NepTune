package com.neptune.neptune.ui.messages

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.neptune.neptune.model.messages.UserMessagePreview
import com.neptune.neptune.model.profile.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing the state and operations related to the messages preview. This has been
 * written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class SelectMessagesViewModel : ViewModel() {

  private val _users = MutableStateFlow(emptyList<UserMessagePreview>())
  val users: StateFlow<List<UserMessagePreview>> = _users.asStateFlow()

  init {
    loadFakeData()
  }

  // Fake preview data with profiles
  private fun loadFakeData() {
    val fakeUsers =
        listOf(
            UserMessagePreview(
                profile =
                    Profile(
                        uid = "u1",
                        username = "test1",
                        name = "Test1",
                        bio = "Bio1",
                        avatarUrl = ""),
                lastMessage = "Hey, how are you?",
                lastTimestamp = Timestamp.now(),
                isOnline = true),
            UserMessagePreview(
                profile =
                    Profile(
                        uid = "u2",
                        username = "test2",
                        name = "Test2",
                        bio = "Bio2",
                        avatarUrl = ""),
                lastMessage = "Let’s try the new feature",
                lastTimestamp = Timestamp.now(),
                isOnline = false),
            UserMessagePreview(
                profile =
                    Profile(
                        uid = "u3",
                        username = "test3",
                        name = "Test3",
                        bio = "Bio3",
                        avatarUrl = ""),
                lastMessage = null,
                lastTimestamp = null,
                isOnline = true))
    // Sort latest to oldest
    _users.update { fakeUsers.sortedByDescending { it.lastTimestamp?.toDate()?.time ?: 0L } }
  }
}
