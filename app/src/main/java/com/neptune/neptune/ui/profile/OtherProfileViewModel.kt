package com.neptune.neptune.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for displaying another user's profile.
 *
 * For now it uses mocked data and simple in-memory follow toggling. Later you can plug in a real
 * repository fetching by [userId].
 */
class OtherProfileViewModel(
    private val repo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val userId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val imageRepo: ImageStorageRepository = ImageStorageRepository(),
    private val storageService: StorageService = StorageService(FirebaseStorage.getInstance())
) : ViewModel() {

  private val _uiState = MutableStateFlow(OtherProfileUiState())
  val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(repo.observeProfile(userId), repo.observeCurrentProfile()) { other, current ->
            other to current
          }
          .collectLatest { (otherProfile, currentProfile) ->
            if (otherProfile != null) {
              val isCurrentUserFollowing = currentProfile?.following?.contains(userId) == true
              _uiState.value =
                  OtherProfileUiState(
                      profile =
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
                              error = null),
                      isCurrentUserFollowing = isCurrentUserFollowing)
            }
          }
    }
  }

  /** Simple toggle for follow/unfollow with local follower count update. */
  fun onFollow() {
    val isCurrentUserFollowing = _uiState.value.isCurrentUserFollowing

    viewModelScope.launch {
      try {
        if (isCurrentUserFollowing) {
          repo.unfollowUser(userId)
        } else {
          repo.followUser(userId)
        }
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                profile = _uiState.value.profile.copy(error = "Unable to update follow state"))
      }
    }
  }
}
