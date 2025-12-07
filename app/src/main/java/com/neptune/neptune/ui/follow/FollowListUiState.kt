package com.neptune.neptune.ui.follow

data class FollowListUiState(
    val activeTab: FollowListTab,
    val followers: List<FollowListUserItem> = emptyList(),
    val following: List<FollowListUserItem> = emptyList(),
    val isLoadingFollowers: Boolean = false,
    val isLoadingFollowing: Boolean = false,
    val errorMessage: String? = null,
    val isCurrentUserAnonymous: Boolean = false,
)
