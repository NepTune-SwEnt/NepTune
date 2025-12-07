package com.neptune.neptune.ui.follow

data class FollowListUserItem(
    val uid: String,
    val username: String,
    val avatarUrl: String?,
    val isFollowedByCurrentUser: Boolean,
    val isActionInProgress: Boolean = false,
)