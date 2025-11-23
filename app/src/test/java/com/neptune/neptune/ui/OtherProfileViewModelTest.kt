package com.neptune.neptune.ui

import com.neptune.neptune.ui.profile.OtherProfileViewModel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class OtherProfileViewModelTest {

  @Test
  fun followToggleUpdatesFollowerCount() {
    val viewModel = OtherProfileViewModel(userId = "artist-42")
    val initialFollowers = viewModel.uiState.value.profile.subscribers

    viewModel.onFollow()
    val followingState = viewModel.uiState.value
    assertTrue(followingState.isCurrentUserFollowing)
    assertEquals(initialFollowers + 1, followingState.profile.subscribers)

    viewModel.onFollow()
    val unfollowingState = viewModel.uiState.value
    assertFalse(unfollowingState.isCurrentUserFollowing)
    assertEquals(initialFollowers, unfollowingState.profile.subscribers)
  }
}
