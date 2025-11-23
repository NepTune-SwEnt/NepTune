package com.neptune.neptune.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.collectLatest
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

  /** Latest snapshot we saw from Firestore, used to detect changes on save. */
  private var snapshot: Profile? = null

  init {
    viewModelScope.launch {
      val currentProfile = repo.getCurrentProfile()
      val isCurrentUserFollowing = currentProfile?.following?.contains(userId) == true

      repo.observeProfile(userId).collectLatest { p ->
        snapshot = p

        if (p != null) {
          _uiState.value =
              _uiState.value.copy(
                  profile =
                      SelfProfileUiState(
                          name = p.name.orEmpty(),
                          username = p.username,
                          bio = p.bio.orEmpty(),
                          avatarUrl = p.avatarUrl,
                          subscribers = p.subscribers.toInt(),
                          subscriptions = p.subscriptions.toInt(),
                          likes = p.likes.toInt(),
                          posts = p.posts.toInt(),
                          tags = p.tags,
                          error = null),
                  isCurrentUserFollowing = isCurrentUserFollowing)
        }
      }
    }
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
