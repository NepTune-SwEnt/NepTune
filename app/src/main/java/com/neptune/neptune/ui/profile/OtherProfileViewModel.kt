package com.neptune.neptune.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying another user's profile.
 *
 * For now it uses mocked data and simple in-memory follow toggling. Later you can plug in a real
 * repository fetching by [userId].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtherProfileViewModel(
    private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val userId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(OtherProfileUiState())
  val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        val currentProfileFlow = repo.observeCurrentProfile()
        val followingFlow =
            currentProfileFlow.flatMapLatest { currentProfile ->
              val uid = currentProfile?.uid ?: auth.currentUser?.uid
              if (uid != null) repo.observeFollowingIds(uid) else flowOf(emptyList())
            }

        combine(repo.observeProfile(userId), currentProfileFlow, followingFlow) {
                other,
                current,
                following ->
              Triple(other, current, following)
            }
            .collectLatest { (otherProfile, currentProfile, followingIds) ->
              if (otherProfile != null) {
                val isCurrentUserFollowing = followingIds.contains(userId)
                val authAnonymous = auth.currentUser?.isAnonymous == true
                val isCurrentUserAnonymous = authAnonymous || currentProfile?.isAnonymous == true
                val updatedProfile =
                    SelfProfileUiState(
                        name = otherProfile.name.orEmpty(),
                        username = otherProfile.username,
                        bio = otherProfile.bio.orEmpty(),
                        avatarUrl = otherProfile.avatarUrl,
                        subscribers = otherProfile.subscribers.toInt(),
                        subscriptions = otherProfile.subscriptions.toInt(),
                        likes = otherProfile.likes.toInt(),
                        posts = otherProfile.posts.toInt(),
                        tags = otherProfile.tags,
                        isAnonymousUser = otherProfile.isAnonymous,
                        error = null)

                _uiState.value =
                    _uiState.value.copy(
                        profile = updatedProfile,
                        isCurrentUserFollowing = isCurrentUserFollowing,
                        errorMessage = null,
                        isFollowActionInProgress = false,
                        isCurrentUserAnonymous = isCurrentUserAnonymous)
              }
            }
      } catch (e: Exception) {
        Log.e("OtherProfileViewModel", "Failed to init: ${e.message}")
      }
    }
  }

  /** Simple toggle for follow/unfollow with local follower count update. */
  fun onFollow() {
    if (_uiState.value.profile.isAnonymousUser || _uiState.value.isCurrentUserAnonymous) return
    val isCurrentUserFollowing = _uiState.value.isCurrentUserFollowing
    _uiState.value =
        _uiState.value.copy(
            errorMessage = null,
            isFollowActionInProgress = true,
        )

    viewModelScope.launch {
      try {
        if (isCurrentUserFollowing) {
          repo.unfollowUser(userId)
        } else {
          repo.followUser(userId)
        }
        _uiState.update { state ->
          state.copy(
              isCurrentUserFollowing = !isCurrentUserFollowing, isFollowActionInProgress = false)
        }
      } catch (_: Exception) {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = "Unable to update follow state",
                isFollowActionInProgress = false,
                isCurrentUserFollowing = isCurrentUserFollowing,
            )
      }
    }
  }
}
