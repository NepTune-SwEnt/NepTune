/* This test class was partially implemented with AI assistance */
package com.neptune.neptune.ui.follow

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.utils.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FollowListViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @get:Rule val mainDispatcherRule = MainDispatcherRule(dispatcher)

  private fun mockAuth(isAnonymous: Boolean = false): FirebaseAuth {
    mockkStatic(FirebaseAuth::class)
    val user = mockk<FirebaseUser> { every { this@mockk.isAnonymous } returns isAnonymous }
    val auth = mockk<FirebaseAuth> { every { currentUser } returns user }
    every { FirebaseAuth.getInstance() } returns auth
    return auth
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun initialStateUsesProvidedTabAndAuthFlag() =
      runTest(dispatcher) {
        mockAuth(isAnonymous = false)

        val viewModel =
            FollowListViewModel(
                repo = FakeProfileRepository(), initialTab = FollowListTab.FOLLOWING)

        val state = viewModel.uiState.value
        assertEquals(FollowListTab.FOLLOWING, state.activeTab)
        assertFalse(state.isCurrentUserAnonymous)
      }

  @Test
  fun refreshPopulatesFollowersAndFollowing() =
      runTest(dispatcher) {
        mockAuth()
        val viewModel =
            FollowListViewModel(
                repo = FakeProfileRepository(), initialTab = FollowListTab.FOLLOWERS)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.followers.size)
        assertEquals(4, state.following.size)
        assertFalse(state.isLoadingFollowers)
        assertFalse(state.isLoadingFollowing)
      }

  @Test
  fun selectTabSwitchesActiveTab() =
      runTest(dispatcher) {
        mockAuth()
        val viewModel =
            FollowListViewModel(
                repo = FakeProfileRepository(), initialTab = FollowListTab.FOLLOWERS)

        viewModel.selectTab(FollowListTab.FOLLOWING)

        assertEquals(FollowListTab.FOLLOWING, viewModel.uiState.value.activeTab)

        viewModel.selectTab(FollowListTab.FOLLOWING)

        // State should remain unchanged when selecting the same tab again
        assertEquals(FollowListTab.FOLLOWING, viewModel.uiState.value.activeTab)
      }

  @Test
  fun toggleFollowMarksProgressThenTogglesFollowFlag() =
      runTest(dispatcher) {
        mockAuth()
        val viewModel =
            FollowListViewModel(
                repo = FakeProfileRepository(), initialTab = FollowListTab.FOLLOWERS)
        advanceUntilIdle()

        val targetId = "u2"

        viewModel.toggleFollow(targetId, isFromFollowersList = true)

        val duringAction =
            viewModel.uiState.value.followers.first { it.uid == targetId }.isActionInProgress
        assertTrue(duringAction)

        advanceUntilIdle()

        val updatedUser = viewModel.uiState.value.followers.first { it.uid == targetId }
        assertTrue(updatedUser.isFollowedByCurrentUser)
        assertFalse(updatedUser.isActionInProgress)
      }
}
