package com.neptune.neptune.ui.profile

/**
 * Represents the complete UI state of another user's profile screen.
 *
 * @property profile The profile information of the other user.
 * @property isCurrentUserFollowing True if the current user is following this profile.
 */
data class OtherProfileUiState(
    val profile: SelfProfileUiState = SelfProfileUiState(),
    val isCurrentUserFollowing: Boolean = false,
)
