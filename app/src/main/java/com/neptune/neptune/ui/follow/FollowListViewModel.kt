/* This class was implemented with AI assistance */

package com.neptune.neptune.ui.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowListViewModel(
    @Suppress("unused") private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val initialTab: FollowListTab
) : ViewModel() {

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _uiState =
      MutableStateFlow(
          FollowListUiState(
              activeTab = initialTab,
              isCurrentUserAnonymous = auth.currentUser?.isAnonymous ?: true))
  val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun selectTab(tab: FollowListTab) {
    if (tab == _uiState.value.activeTab) return
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun refresh() {
    loadFollowers()
    loadFollowing()
  }

  private fun currentList(isFollowers: Boolean): List<FollowListUserItem> =
      if (isFollowers) _uiState.value.followers else _uiState.value.following

  private fun saveList(isFollowers: Boolean, list: List<FollowListUserItem>) {
    _uiState.update { state ->
      if (isFollowers) state.copy(followers = list) else state.copy(following = list)
    }
  }

  private fun markInProgress(uid: String, isFollowers: Boolean) {
    val updated =
        currentList(isFollowers).map {
          if (it.uid == uid) it.copy(isActionInProgress = true) else it
        }
    saveList(isFollowers, updated)
  }

  private fun toggleFollowFlag(uid: String, isFollowers: Boolean) {
    val updated =
        currentList(isFollowers).map {
          if (it.uid == uid)
              it.copy(
                  isFollowedByCurrentUser = !it.isFollowedByCurrentUser, isActionInProgress = false)
          else it
        }
    saveList(isFollowers, updated)
  }

  fun toggleFollow(uid: String, isFromFollowersList: Boolean) {
    markInProgress(uid, isFromFollowersList)
    viewModelScope.launch {
      delay(250)
      // TODO: replace with repo.followUser/unfollowUser calls and rollback on failure
      toggleFollowFlag(uid, isFromFollowersList)
    }
  }

  private fun loadFollowers() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingFollowers = true, errorMessage = null) }
      delay(400) // simulate backend fetch
      // TODO: replace fakeFollowers() with repo call + map to FollowListUserItem
      _uiState.update {
        it.copy(followers = fakeFollowers(), isLoadingFollowers = false, errorMessage = null)
      }
    }
  }

  private fun loadFollowing() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingFollowing = true, errorMessage = null) }
      delay(400)
      // TODO: replace fakeFollowing() with repo call + map to FollowListUserItem
      _uiState.update {
        it.copy(following = fakeFollowing(), isLoadingFollowing = false, errorMessage = null)
      }
    }
  }

  // ====================== MOCK DATA FOR TESTING ======================
  private fun fakeFollowers(): List<FollowListUserItem> =
      listOf(
          FollowListUserItem(
              uid = "u1",
              username = "luca",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
          FollowListUserItem(uid = "u2", username = "longestusername", avatarUrl = SAMPLE_AVATAR),
          FollowListUserItem(
              uid = "u3",
              username = "chiara",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
          FollowListUserItem(uid = "u4", username = "cate", avatarUrl = SAMPLE_AVATAR),
          FollowListUserItem(
              uid = "u5",
              username = "alice",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
      )

  private fun fakeFollowing(): List<FollowListUserItem> =
      listOf(
          FollowListUserItem(
              uid = "u3",
              username = "chiara",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
          FollowListUserItem(
              uid = "u6",
              username = "eleonora",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
          FollowListUserItem(uid = "u7", username = "mattia", avatarUrl = SAMPLE_AVATAR),
          FollowListUserItem(
              uid = "u8",
              username = "anto",
              avatarUrl = SAMPLE_AVATAR,
              isFollowedByCurrentUser = true),
      )

  private fun fakeEmptyFollowing(): List<FollowListUserItem> = emptyList()

  private fun fakeEmptyFollowers(): List<FollowListUserItem> = emptyList()

  companion object {
    private const val SAMPLE_AVATAR =
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=80"
  }
}
