/* This file was coded with AI assistance */

package com.neptune.neptune.ui

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.recommendation.RecoUserProfile
import com.neptune.neptune.ui.profile.OtherProfileViewModel
import com.neptune.neptune.utils.MainDispatcherRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OtherProfileViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @get:Rule val mainDispatcherRule = MainDispatcherRule(dispatcher)

  @Test
  fun followToggleUpdatesFollowerCount() =
      runTest(dispatcher) {
        val targetUserId = "artist-42"
        val otherProfile =
            Profile(
                uid = targetUserId,
                username = "artist42",
                name = "Artist 42",
                bio = "Composer",
                subscribers = 120,
                subscriptions = 25,
                likes = 10,
                posts = 3)
        val currentProfile = Profile(uid = "viewer-1", username = "listener", name = "Listener")

        val repo = FollowToggleTestRepository(otherProfile, currentProfile)
        val mockAuth: FirebaseAuth = mock()
        val viewModel =
            OtherProfileViewModel(
                repo = repo,
                userId = targetUserId,
                auth = mockAuth,
            )
        advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertEquals(120, initialState.profile.subscribers)
        assertFalse(initialState.isCurrentUserFollowing)

        viewModel.onFollow()
        advanceUntilIdle()
        assertEquals(listOf(targetUserId), repo.followCalls)
        val pendingState = viewModel.uiState.value
        assertTrue(pendingState.isCurrentUserFollowing)

        val followedOther = otherProfile.copy(subscribers = otherProfile.subscribers + 1)
        repo.updateFollowing(listOf(targetUserId))
        repo.updateOther(followedOther)
        advanceUntilIdle()

        val followingState = viewModel.uiState.value
        assertTrue(followingState.isCurrentUserFollowing)
        assertEquals(121, followingState.profile.subscribers)

        viewModel.onFollow()
        advanceUntilIdle()
        assertEquals(listOf(targetUserId), repo.unfollowCalls)

        repo.updateFollowing(emptyList())
        repo.updateOther(otherProfile)
        advanceUntilIdle()

        val unfollowingState = viewModel.uiState.value
        assertFalse(unfollowingState.isCurrentUserFollowing)
        assertEquals(120, unfollowingState.profile.subscribers)
      }

  @Test
  fun anonymousCurrentUserCannotFollow() =
      runTest(dispatcher) {
        val targetUserId = "artist-42"
        val otherProfile = Profile(uid = targetUserId, username = "artist")
        val currentProfile = Profile(uid = "viewer-1", username = "viewer", isAnonymous = true)
        val repo = FollowToggleTestRepository(otherProfile, currentProfile)
        val mockAuth: FirebaseAuth = mock()
        val viewModel =
            OtherProfileViewModel(
                repo = repo,
                userId = targetUserId,
                auth = mockAuth,
            )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCurrentUserAnonymous)

        viewModel.onFollow()
        advanceUntilIdle()

        assertTrue(repo.followCalls.isEmpty())
        assertTrue(repo.unfollowCalls.isEmpty())
      }

  @Test
  fun anonymousAuthUserWithoutProfileCannotFollow() =
      runTest(dispatcher) {
        val targetUserId = "artist-24"
        val otherProfile = Profile(uid = targetUserId, username = "artist24")
        val repo = FollowToggleTestRepository(otherProfile, initialCurrentProfile = null)
        val mockAuth: FirebaseAuth = mock()
        val mockUser: FirebaseUser = mock()
        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.isAnonymous).thenReturn(true)

        val viewModel =
            OtherProfileViewModel(
                repo = repo,
                userId = targetUserId,
                auth = mockAuth,
            )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCurrentUserAnonymous)

        viewModel.onFollow()
        advanceUntilIdle()

        assertTrue(repo.followCalls.isEmpty())
        assertTrue(repo.unfollowCalls.isEmpty())
      }
}

private class FollowToggleTestRepository(
    initialOtherProfile: Profile,
    initialCurrentProfile: Profile?,
    initialFollowing: List<String> = emptyList()
) : ProfileRepository {

  private val otherProfileState = MutableStateFlow<Profile?>(initialOtherProfile)
  private val currentProfileState = MutableStateFlow<Profile?>(initialCurrentProfile)
  private val followingState = MutableStateFlow(initialFollowing)

  val followCalls = mutableListOf<String>()
  val unfollowCalls = mutableListOf<String>()

  fun updateOther(profile: Profile) {
    otherProfileState.value = profile
  }

  fun updateFollowing(newFollowing: List<String>) {
    followingState.value = newFollowing
  }

  override suspend fun getCurrentProfile(): Profile? = currentProfileState.value

  override suspend fun getProfile(uid: String): Profile? {
    val profile = otherProfileState.value
    return if (profile?.uid == uid) profile else null
  }

  override fun observeCurrentProfile(): Flow<Profile?> = currentProfileState

  override fun observeProfile(uid: String): Flow<Profile?> = otherProfileState

  override fun observeAllProfiles(): Flow<List<Profile?>> =
      combine(otherProfileState, currentProfileState) { other, current -> listOf(other, current) }

  override suspend fun updatePostCount(delta: Int) {}

  override suspend fun updateLikeCount(targetUserId: String, delta: Int) {}

  override suspend fun unfollowUser(uid: String) {
    unfollowCalls.add(uid)
  }

  override suspend fun followUser(uid: String) {
    followCalls.add(uid)
  }

  override suspend fun getFollowingIds(uid: String): List<String> = followingState.value

  override suspend fun getFollowersIds(uid: String): List<String> = emptyList()

  override fun observeFollowingIds(uid: String): Flow<List<String>> = followingState

  override fun observeFollowersIds(uid: String): Flow<List<String>> = MutableStateFlow(emptyList())

  override suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun isUsernameAvailable(username: String): Boolean {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun setUsername(newUsername: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun generateRandomFreeUsername(base: String): String {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun updateName(newName: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun updateBio(newBio: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun updateAvatarUrl(newUrl: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun addNewTag(tag: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun removeTag(tag: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun uploadAvatar(localUri: Uri): String {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun removeAvatar() {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun getAvatarUrlByUserId(userId: String): String? {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun getUserNameByUserId(userId: String): String? {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun searchUsers(query: String): List<Profile> = emptyList()

  override suspend fun getCurrentRecoUserProfile(): RecoUserProfile? {
    TODO("Not yet implemented")
  }

  override suspend fun recordTagInteraction(
      tags: List<String>,
      likeDelta: Int,
      downloadDelta: Int
  ) {
    TODO("Not yet implemented")
  }
}
