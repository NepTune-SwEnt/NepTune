package com.neptune.neptune.ui.follow

data class FollowListUserItem(
    val uid: String,
    val username: String,
    val avatarUrl: String? = null,
    val isFollowedByCurrentUser: Boolean = false,
    val isActionInProgress: Boolean = false,
)
