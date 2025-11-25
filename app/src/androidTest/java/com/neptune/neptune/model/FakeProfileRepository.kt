package com.neptune.neptune.model

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {

  private var profile: Profile? = initial

  override suspend fun getCurrentProfile(): Profile? = profile

  override suspend fun getProfile(uid: String): Profile? {
    throw UnsupportedOperationException("Not needed in this test")
  }

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
    return userId
  }
}
