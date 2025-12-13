/* This class was implemented with AI assistance */

package com.neptune.neptune.ui.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
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
    if (_uiState.value.isCurrentUserAnonymous) return
    when (_uiState.value.activeTab) {
      FollowListTab.FOLLOWERS -> loadFollowers()
      FollowListTab.FOLLOWING -> loadFollowing()
    }
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
      val isCurrentlyFollowed =
          currentList(isFromFollowersList).firstOrNull { it.uid == uid }?.isFollowedByCurrentUser
              ?: false
      try {
        if (isCurrentlyFollowed) {
          repo.unfollowUser(uid)
        } else {
          repo.followUser(uid)
        }
        toggleFollowFlag(uid, isFromFollowersList)
      } catch (e: Exception) {
        resetProgress(uid, isFromFollowersList, isCurrentlyFollowed)
        _uiState.update {
          it.copy(errorMessage = e.message ?: "Failed to update follow state. Please try again.")
        }
      }
    }
  }

  private fun loadFollowers() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingFollowers = true, errorMessage = null) }
      val uid = auth.currentUser?.uid
      if (uid == null) {
        _uiState.update {
          it.copy(
              isLoadingFollowers = false, errorMessage = "User not authenticated. Please sign in.")
        }
        return@launch
      }

      try {
        val followersIds = repo.getFollowersIds(uid)
        val followingIds = repo.getFollowingIds(uid).toSet()
        val followers =
            buildUserItems(
                ids = followersIds,
                isFollowedByCurrentUser = { targetUid -> followingIds.contains(targetUid) })
        _uiState.update {
          it.copy(followers = followers, isLoadingFollowers = false, errorMessage = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoadingFollowers = false,
              errorMessage = e.message ?: "Failed to load followers. Please try again.")
        }
      }
    }
  }

  private fun loadFollowing() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingFollowing = true, errorMessage = null) }
      val uid = auth.currentUser?.uid
      if (uid == null) {
        _uiState.update {
          it.copy(
              isLoadingFollowing = false, errorMessage = "User not authenticated. Please sign in.")
        }
        return@launch
      }

      try {
        val followingIds = repo.getFollowingIds(uid)
        val following = buildUserItems(ids = followingIds, isFollowedByCurrentUser = { true })
        _uiState.update {
          it.copy(following = following, isLoadingFollowing = false, errorMessage = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoadingFollowing = false,
              errorMessage = e.message ?: "Failed to load following list. Please try again.")
        }
      }
    }
  }

  private suspend fun buildUserItems(
      ids: List<String>,
      isFollowedByCurrentUser: (String) -> Boolean
  ): List<FollowListUserItem> {
    return ids.distinct().mapNotNull { targetUid ->
      val profile = runCatching { repo.getProfile(targetUid) }.getOrNull() ?: return@mapNotNull null
      toUserItem(profile, isFollowedByCurrentUser(targetUid))
    }
  }

  private fun toUserItem(profile: Profile, isFollowedByCurrentUser: Boolean) =
      FollowListUserItem(
          uid = profile.uid,
          username = profile.username.ifBlank { profile.name ?: profile.uid },
          avatarUrl = profile.avatarUrl.takeUnless { it.isBlank() },
          isFollowedByCurrentUser = isFollowedByCurrentUser)

  private fun resetProgress(uid: String, isFollowers: Boolean, followState: Boolean) {
    val updated =
        currentList(isFollowers).map {
          if (it.uid == uid)
              it.copy(isActionInProgress = false, isFollowedByCurrentUser = followState)
          else it
        }
    saveList(isFollowers, updated)
  }
}
