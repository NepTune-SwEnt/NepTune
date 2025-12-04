package com.neptune.neptune.model

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.recommendation.RecoUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Fake Profile Repository for testing purposes. This has been written with the help of LLMs. */
class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {

  private var profile: Profile? = initial

  // Map userId -> username for testing
  private val usernames = mutableMapOf<String, String>()

  fun setUsernameForTest(userId: String, username: String) {
    usernames[userId] = username
  }

  override suspend fun getCurrentProfile(): Profile? = profile

  override suspend fun getProfile(uid: String): Profile? = profile

  override fun observeCurrentProfile(): Flow<Profile?> = flowOf(profile)

  override fun observeProfile(uid: String): Flow<Profile?> {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun unfollowUser(uid: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun followUser(uid: String) {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile {
    val profile =
        this@FakeProfileRepository.profile
            ?: Profile(
                uid = "test-uid",
                name = name ?: "Test User",
                username = (suggestedUsernameBase ?: "user") + "123",
                bio = "Test bio",
                avatarUrl = "",
                subscribers = 0,
                subscriptions = 0,
                likes = 0,
                posts = 0,
                tags = arrayListOf())
    this@FakeProfileRepository.profile = profile
    return profile
  }

  override suspend fun isUsernameAvailable(username: String): Boolean = true

  override suspend fun setUsername(newUsername: String) {}

  override suspend fun generateRandomFreeUsername(base: String): String = "${base}123"

  override suspend fun updateName(newName: String) {
    profile = profile?.copy(name = newName)
  }

  override suspend fun updateBio(newBio: String) {
    profile = profile?.copy(bio = newBio)
  }

  override suspend fun updateAvatarUrl(newUrl: String) {}

  override suspend fun uploadAvatar(localUri: Uri): String = ""

  override suspend fun addNewTag(tag: String) {
    val currentTags = profile?.tags ?: arrayListOf()
    if (!currentTags.contains(tag)) {
      val updatedTags = ArrayList(currentTags)
      updatedTags.add(tag)
      profile = profile?.copy(tags = updatedTags)
    }
  }

  override suspend fun removeTag(tag: String) {
    val currentTags = profile?.tags ?: arrayListOf()
    if (currentTags.contains(tag)) {
      val updatedTags = ArrayList(currentTags)
      updatedTags.remove(tag)
      profile = profile?.copy(tags = updatedTags)
    }
  }

  override suspend fun removeAvatar() {}

  override suspend fun getAvatarUrlByUserId(userId: String): String? {
    return userId
  }

  override suspend fun getUserNameByUserId(userId: String): String? {
    // Return stored profile username if it matches
    profile?.let { if (it.uid == userId) return it.username }

    // Otherwise check test usernames map
    return usernames[userId]
  }

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
