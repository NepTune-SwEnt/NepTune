package com.neptune.neptune.model.fakes

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.UsernameTakenException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeProfileRepository(
    initial: Profile? =
        Profile(
            uid = "testUid",
            username = "johndoe",
            name = "John Doe",
            bio = "Hello! New NepTune user here!",
            avatarUrl = "")
) : ProfileRepository {

  private val state = MutableStateFlow(initial)

  override suspend fun getCurrentProfile(): Profile? {
    TODO("Not yet implemented")
  }

  override suspend fun getProfile(uid: String): Profile? = state.value

  override fun observeCurrentProfile(): Flow<Profile?> = state.asStateFlow()

  override fun observeProfile(uid: String): Flow<Profile?> {
    TODO("Not yet implemented")
  }

  override suspend fun unfollowUser(uid: String) {
    TODO("Not yet implemented")
  }

  override suspend fun followUser(uid: String) {
    TODO("Not yet implemented")
  }

  override suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile {
    val existing = state.value
    if (existing != null) return existing

    val base = (suggestedUsernameBase?.ifBlank { null } ?: "user")
    val username = generateRandomFreeUsername(base)
    val finalName = name?.ifBlank { null } ?: username
    val new =
        Profile(
            uid = "testUid",
            username = username,
            name = finalName,
            bio = "Hello! New NepTune user here!",
            avatarUrl = "")
    state.value = new
    return new
  }

  override suspend fun isUsernameAvailable(username: String): Boolean {
    val current = state.value
    return current?.username?.equals(username, ignoreCase = false) != true
  }

  override suspend fun setUsername(newUsername: String) {
    if (!isUsernameAvailable(newUsername)) throw UsernameTakenException(newUsername)
    val cur = state.value ?: error("No profile yet")
    state.value = cur.copy(username = newUsername, name = cur.name ?: newUsername)
  }

  override suspend fun generateRandomFreeUsername(base: String): String {
    val cleaned = base.ifBlank { "user" }
    if (isUsernameAvailable(cleaned)) return cleaned
    var n = 1
    while (true) {
      val candidate = "${cleaned}_${n}"
      if (isUsernameAvailable(candidate)) return candidate
      n++
    }
  }

  override suspend fun updateName(newName: String) {
    val cur = state.value ?: return
    state.value = cur.copy(name = newName)
  }

  override suspend fun updateBio(newBio: String) {
    val cur = state.value ?: return
    state.value = cur.copy(bio = newBio)
  }

  override suspend fun updateAvatarUrl(newUrl: String) {}

  override suspend fun uploadAvatar(localUri: Uri): String {
    val cur = state.value ?: return ""
    val url = "https://example.invalid/avatars/${cur.uid}.jpg"
    state.value = cur.copy(avatarUrl = url)
    return url
  }

  override suspend fun addNewTag(tag: String) {
    val cur = state.value ?: return
    val currentTags = ArrayList(cur.tags)
    if (!currentTags.contains(tag)) {
      currentTags.add(tag)
      state.value = cur.copy(tags = currentTags)
    }
  }

  override suspend fun removeTag(tag: String) {
    val cur = state.value ?: return
    val currentTags = ArrayList(cur.tags)
    if (currentTags.remove(tag)) {
      state.value = cur.copy(tags = currentTags)
    }
  }

  override suspend fun removeAvatar() {
    val cur = state.value ?: return
    state.value = cur.copy(avatarUrl = "")
  }
}
