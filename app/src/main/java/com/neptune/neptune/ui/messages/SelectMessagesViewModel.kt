package com.neptune.neptune.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.model.messages.MessageRepository
import com.neptune.neptune.model.messages.MessageRepositoryProvider
import com.neptune.neptune.model.messages.UserMessagePreview
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for managing the state and operations related to the messages preview. This has been
 * written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class SelectMessagesViewModel(
    private val currentUid: String,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val messageRepo: MessageRepository = MessageRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    initialUsers: List<UserMessagePreview>? = null /*for testing only*/
) : ViewModel() {

  private val _users = MutableStateFlow(emptyList<UserMessagePreview>())
  val users: StateFlow<List<UserMessagePreview>> = _users.asStateFlow()

  private val _isAnonymous = MutableStateFlow(auth.currentUser?.isAnonymous ?: true)
  val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()

  private val userMap = mutableMapOf<String, UserMessagePreview>()

  init {
    // Test mode -> inject fake data
    if (initialUsers != null) {
      _users.value = initialUsers
    } else {
      observeProfiles()
      observeMessages()
    }
  }

  private fun observeProfiles() {
    profileRepo
        .observeAllProfiles()
        .onEach { profiles ->
          profiles
              .filterNotNull()
              .filter { it.uid != currentUid } // Exclude current user
              .filter { !it.isAnonymous } // Exclude anonymous
              .forEach { profile ->
                val existing = userMap[profile.uid]
                userMap[profile.uid] =
                    UserMessagePreview(
                        profile = profile,
                        lastMessage = existing?.lastMessage,
                        lastTimestamp = existing?.lastTimestamp,
                        isOnline = existing?.isOnline ?: false)
                observeUserOnline(profile.uid)
              }
          updateUsersState()
        }
        .launchIn(viewModelScope)
  }

  private fun observeMessages() {
    messageRepo
        .observeMessagePreviews(currentUid)
        .onEach { previews ->
          previews.forEach { preview ->
            val existing = userMap[preview.profile.uid]
            userMap[preview.profile.uid] =
                UserMessagePreview(
                    profile = existing?.profile ?: preview.profile,
                    lastMessage = preview.lastMessage,
                    lastTimestamp = preview.lastTimestamp,
                    isOnline = existing?.isOnline ?: preview.isOnline)
          }
          updateUsersState()
        }
        .launchIn(viewModelScope)
  }

  private fun observeUserOnline(uid: String) {
    messageRepo
        .observeUserOnlineState(uid)
        .onEach { isOnline ->
          val existing = userMap[uid]
          if (existing != null) {
            userMap[uid] = existing.copy(isOnline = isOnline)
            updateUsersState()
          }
        }
        .launchIn(viewModelScope)
  }

  private fun updateUsersState() {
    _users.value =
        userMap.values
            .filter { !it.profile.isAnonymous } // Exclude anonymous
            .sortedByDescending { it.lastTimestamp?.toDate()?.time ?: 0L }
            .map { it.copy() }
  }
}
