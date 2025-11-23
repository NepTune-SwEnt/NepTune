package com.neptune.neptune.ui.profile

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for displaying another user's profile.
 *
 * For now it uses mocked data and simple in-memory follow toggling. Later you can plug in a real
 * repository fetching by [userId].
 */
class OtherProfileViewModel(
    private val userId: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow(OtherProfileUiState())
  val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

  init {
    // TODO: replace with real repository call in the future.
    // For now: mocked profile based on userId.
    _uiState.value =
        OtherProfileUiState(
            profile =
                SelfProfileUiState(
                    name = "Demo User",
                    username = "demo_$userId",
                    bio = "This is a mocked profile for $userId.",
                    subscribers = 42,
                    subscriptions = 17,
                    likes = 123,
                    posts = 5,
                    tags = listOf("rock", "edm", "synthwave"),
                    mode = ProfileMode.VIEW,
                ),
            isCurrentUserFollowing = false,
        )
  }

  /** Simple toggle for follow/unfollow with local follower count update. */
  fun onFollow() {
    val current = _uiState.value
    val newIsFollowing = !current.isCurrentUserFollowing
    val delta = if (newIsFollowing) 1 else -1

    // TODO: real repo call
    _uiState.value =
        current.copy(
            isCurrentUserFollowing = newIsFollowing,
            profile =
                current.profile.copy(
                    subscribers =
                        (current.profile.subscribers + delta).coerceAtLeast(
                            0), // don’t go below zero
                ),
        )
  }
}
