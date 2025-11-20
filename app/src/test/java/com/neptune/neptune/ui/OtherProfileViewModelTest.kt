package com.neptune.neptune.ui

import com.neptune.neptune.ui.profile.OtherProfileViewModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtherProfileViewModelTest {

  @Test
  fun followToggleUpdatesFollowerCount() {
    val viewModel = OtherProfileViewModel(userId = "artist-42")
    val initialFollowers = viewModel.uiState.value.profile.followers

    viewModel.onFollow()
    val followingState = viewModel.uiState.value
      assertTrue(followingState.isFollowing)
      assertEquals(initialFollowers + 1, followingState.profile.followers)

    viewModel.onFollow()
    val unfollowingState = viewModel.uiState.value
      assertFalse(unfollowingState.isFollowing)
      assertEquals(initialFollowers, unfollowingState.profile.followers)
  }
}