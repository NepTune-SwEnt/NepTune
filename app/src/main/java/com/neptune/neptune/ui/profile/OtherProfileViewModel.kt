package com.neptune.neptune.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for displaying another user's profile.
 *
 * For now it uses mocked data and simple in-memory follow toggling.
 * Later you can plug in a real repository fetching by [userId].
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
                        followers = 42,
                        following = 17,
                        likes = 123,
                        posts = 5,
                        tags = listOf("rock", "edm", "synthwave"),
                        mode = ProfileMode.VIEW,
                    ),
                isFollowing = false,
            )
    }

    /** Simple toggle for follow/unfollow with local follower count update. */
    fun onFollow() {
        val current = _uiState.value
        val newIsFollowing = !current.isFollowing
        val delta = if (newIsFollowing) 1 else -1

        _uiState.value =
            current.copy(
                isFollowing = newIsFollowing,
                profile =
                    current.profile.copy(
                        followers =
                            (current.profile.followers + delta)
                                .coerceAtLeast(0), // don’t go below zero
                    ),
            )
    }
}
