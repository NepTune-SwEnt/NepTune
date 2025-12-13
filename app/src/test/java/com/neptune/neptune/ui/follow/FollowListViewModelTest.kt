/* This test class was partially implemented with AI assistance */
package com.neptune.neptune.ui.follow

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
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

  private fun mockAuth(isAnonymous: Boolean = false, uid: String = "testUid"): FirebaseAuth {
    mockkStatic(FirebaseAuth::class)
    val user =
        mockk<FirebaseUser> {
          every { this@mockk.isAnonymous } returns isAnonymous
          every { this@mockk.uid } returns uid
        }
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
  fun refreshLoadsActiveTabOnlyThenLoadsOtherWhenRequested() =
      runTest(dispatcher) {
        mockAuth()
        val repo = FakeProfileRepository()
        repo.addProfiles(
            listOf(
                Profile(uid = "u1", username = "luca"),
                Profile(uid = "u2", username = "longestusername"),
                Profile(uid = "u3", username = "chiara"),
                Profile(uid = "u4", username = "cate"),
                Profile(uid = "u5", username = "alice"),
                Profile(uid = "u6", username = "eleonora"),
                Profile(uid = "u7", username = "mattia"),
                Profile(uid = "u8", username = "anto")))
        repo.setFollowersIds(listOf("u1", "u2", "u3", "u4", "u5"))
        repo.setFollowingIds(listOf("u3", "u6", "u7", "u8"))

        val viewModel = FollowListViewModel(repo = repo, initialTab = FollowListTab.FOLLOWERS)

        advanceUntilIdle()

        // Initial load (followers tab active) populates only followers
        val afterFollowersLoad = viewModel.uiState.value
        assertEquals(5, afterFollowersLoad.followers.size)
        assertEquals(0, afterFollowersLoad.following.size)
        assertTrue(afterFollowersLoad.followers.first { it.uid == "u3" }.isFollowedByCurrentUser)
        assertFalse(afterFollowersLoad.isLoadingFollowers)
        assertFalse(afterFollowersLoad.isLoadingFollowing)

        // Switch to following tab and refresh to load that list
        viewModel.selectTab(FollowListTab.FOLLOWING)
        viewModel.refresh()
        advanceUntilIdle()

        val afterFollowingLoad = viewModel.uiState.value
        assertEquals(4, afterFollowingLoad.following.size)
        assertEquals(5, afterFollowingLoad.followers.size)
        assertFalse(afterFollowingLoad.isLoadingFollowers)
        assertFalse(afterFollowingLoad.isLoadingFollowing)
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
        val repo = FakeProfileRepository()
        repo.addProfiles(
            listOf(
                Profile(uid = "u1", username = "luca"),
                Profile(uid = "u2", username = "longestusername")))
        repo.setFollowersIds(listOf("u1", "u2"))
        val viewModel = FollowListViewModel(repo = repo, initialTab = FollowListTab.FOLLOWERS)
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

  @Test
  fun unfollowFromFollowersRemovesFromFollowingAndFollowBackReadds() =
      runTest(dispatcher) {
        mockAuth()
        val repo = FakeProfileRepository()
        repo.addProfiles(
            listOf(
                Profile(uid = "u1", username = "luca"),
                Profile(uid = "u2", username = "longestusername"),
                Profile(uid = "u3", username = "chiara"),
                Profile(uid = "u6", username = "eleonora")))
        repo.setFollowersIds(listOf("u1", "u2", "u3"))
        repo.setFollowingIds(listOf("u3", "u6"))

        val viewModel = FollowListViewModel(repo = repo, initialTab = FollowListTab.FOLLOWERS)
        advanceUntilIdle() // load followers

        viewModel.selectTab(FollowListTab.FOLLOWING)
        viewModel.refresh()
        advanceUntilIdle() // load following

        viewModel.selectTab(FollowListTab.FOLLOWERS)
        viewModel.toggleFollow("u3", isFromFollowersList = true) // unfollow chiara
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.following.none { it.uid == "u3" })

        viewModel.toggleFollow("u3", isFromFollowersList = true) // follow back
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.following.any { it.uid == "u3" })
      }

  @Test
  fun unfollowFromFollowingKeepsEntryVisible() =
      runTest(dispatcher) {
        mockAuth()
        val repo = FakeProfileRepository()
        repo.addProfiles(
            listOf(
                Profile(uid = "u1", username = "luca"),
                Profile(uid = "u2", username = "longestusername")))
        repo.setFollowingIds(listOf("u1", "u2"))

        val viewModel = FollowListViewModel(repo = repo, initialTab = FollowListTab.FOLLOWING)
        advanceUntilIdle() // load following

        viewModel.toggleFollow("u1", isFromFollowersList = false) // unfollow from following tab
        advanceUntilIdle()

        val followingList = viewModel.uiState.value.following
        assertTrue(followingList.any { it.uid == "u1" })
        assertFalse(followingList.first { it.uid == "u1" }.isFollowedByCurrentUser)
      }

  @Test
  fun toggleFollowFailureResetsProgressAndRestoresFollowState() =
      runTest(dispatcher) {
        mockAuth()
        val baseRepo = FakeProfileRepository()
        baseRepo.addProfiles(listOf(Profile(uid = "target", username = "target")))
        baseRepo.setFollowersIds(listOf("target"))
        baseRepo.setFollowingIds(listOf("target"))
        val repo = ThrowingFollowRepository(baseRepo)

        val viewModel = FollowListViewModel(repo = repo, initialTab = FollowListTab.FOLLOWERS)
        advanceUntilIdle() // load followers

        viewModel.selectTab(FollowListTab.FOLLOWING)
        viewModel.refresh()
        advanceUntilIdle() // load following
        viewModel.selectTab(FollowListTab.FOLLOWERS)

        viewModel.toggleFollow("target", isFromFollowersList = true) // attempt to unfollow

        assertTrue(
            viewModel.uiState.value.followers.first { it.uid == "target" }.isActionInProgress)

        advanceUntilIdle() // allow coroutine to fail and hit resetProgress

        val state = viewModel.uiState.value
        val followerItem = state.followers.first { it.uid == "target" }
        val followingItem = state.following.first { it.uid == "target" }

        assertTrue(followerItem.isFollowedByCurrentUser)
        assertFalse(followerItem.isActionInProgress)
        assertTrue(followingItem.isFollowedByCurrentUser)
        assertFalse(followingItem.isActionInProgress)
        assertEquals("follow failed", state.errorMessage)
      }
}

private class ThrowingFollowRepository(
    private val delegate: FakeProfileRepository,
) : ProfileRepository by delegate {

  override suspend fun followUser(uid: String) {
    throw RuntimeException("follow failed")
  }

  override suspend fun unfollowUser(uid: String) {
    throw RuntimeException("follow failed")
  }
}
