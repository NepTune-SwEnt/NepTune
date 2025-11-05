package com.neptune.neptune.model

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {

  private var profile: Profile? = initial

  override suspend fun getProfile(): Profile? = profile

  override fun observeProfile(): Flow<Profile?> = flowOf(profile)

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

  override suspend fun uploadAvatar(localUri: Uri): String = ""

  override suspend fun addNewTag(tag: String) {}

  override suspend fun removeTag(tag: String) {}

  override suspend fun removeAvatar() {}
}
