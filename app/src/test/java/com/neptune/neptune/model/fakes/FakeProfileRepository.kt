package com.neptune.neptune.model.fakes

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.UsernameTakenException
import com.neptune.neptune.model.recommendation.RecoUserProfile
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
  private val profiles = mutableMapOf<String, Profile>()

  init {
    initial?.let { profiles[it.uid] = it }
  }

  override suspend fun getCurrentProfile(): Profile? {
    return state.value
  }

  override suspend fun getProfile(uid: String): Profile? = profiles[uid] ?: state.value

  override fun observeCurrentProfile(): Flow<Profile?> = state.asStateFlow()

  override fun observeProfile(uid: String): Flow<Profile?> {
    throw UnsupportedOperationException("Not needed in this test")
  }

  override fun observeAllProfiles(): Flow<List<Profile?>> {
    throw UnsupportedOperationException("Not needed in this test")
  }

  private val followingIds = MutableStateFlow<List<String>>(emptyList())
  private val followersIds = MutableStateFlow<List<String>>(emptyList())

  override suspend fun updatePostCount(delta: Int) {}

  override suspend fun updateLikeCount(targetUserId: String, delta: Int) {}

  override suspend fun unfollowUser(uid: String) {
    followingIds.value = followingIds.value.filterNot { it == uid }
  }

  override suspend fun followUser(uid: String) {
    val updated = followingIds.value.toMutableList()
    if (!updated.contains(uid)) {
      updated.add(uid)
      followingIds.value = updated
    }
  }

  override suspend fun getFollowingIds(uid: String): List<String> = followingIds.value

  override suspend fun getFollowersIds(uid: String): List<String> = followersIds.value

  override fun observeFollowingIds(uid: String): Flow<List<String>> = followingIds.asStateFlow()

  override fun observeFollowersIds(uid: String): Flow<List<String>> = followersIds.asStateFlow()

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
    profiles[new.uid] = new
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
    val updated = cur.copy(name = newName)
    state.value = updated
    profiles[updated.uid] = updated
  }

  override suspend fun updateBio(newBio: String) {
    val cur = state.value ?: return
    val updated = cur.copy(bio = newBio)
    state.value = updated
    profiles[updated.uid] = updated
  }

  override suspend fun updateAvatarUrl(newUrl: String) {}

  override suspend fun uploadAvatar(localUri: Uri): String {
    val cur = state.value ?: return ""
    val url = "https://example.invalid/avatars/${cur.uid}.jpg"
    val updated = cur.copy(avatarUrl = url)
    state.value = updated
    profiles[updated.uid] = updated
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
    val updated = cur.copy(avatarUrl = "")
    state.value = updated
    profiles[updated.uid] = updated
  }

  override suspend fun getAvatarUrlByUserId(userId: String): String? {
    return profiles[userId]?.avatarUrl ?: userId
  }

  override suspend fun getUserNameByUserId(userId: String): String? {
    val current = state.value
    return profiles[userId]?.username ?: current?.takeIf { it.uid == userId }?.username
  }

  override suspend fun searchUsers(query: String): List<Profile> = emptyList()

  override suspend fun getCurrentRecoUserProfile(): RecoUserProfile? {
    val profile = state.value ?: return null

    val tagProfile = mutableMapOf<String, Double>()

    // Use stored tag weights if present
    if (profile.tagsWeight.isNotEmpty()) {
      for ((tag, weight) in profile.tagsWeight) {
        if (weight > 0.0) {
          tagProfile[tag] = weight
        }
      }
    }

    // Fallback: if no weights, use plain tags with weight = 1f
    if (tagProfile.isEmpty()) {
      for (tag in profile.tags) {
        tagProfile[tag] = 1.0
      }
    }

    return RecoUserProfile(uid = profile.uid, tagsWeight = tagProfile.toMap())
  }

  override suspend fun recordTagInteraction(
      tags: List<String>,
      likeDelta: Int,
      downloadDelta: Int
  ) {
    val cur = state.value ?: return
    if (tags.isEmpty()) return

    // simple scoring: likes count full, downloads half
    val delta = likeDelta.toDouble() + downloadDelta.toDouble() * 0.5
    if (delta == 0.0) return

    val newWeights = cur.tagsWeight.toMutableMap()
    for (tag in tags) {
      val prev = newWeights[tag] ?: 0.0
      val updated = (prev + delta).coerceAtLeast(0.0)
      newWeights[tag] = updated
    }

    // Keep tag list in sync too
    val newTags = (cur.tags + tags).distinct()

    val updated = cur.copy(tagsWeight = newWeights, tags = newTags)
    state.value = updated
    profiles[updated.uid] = updated
  }

  fun setFollowersIds(ids: List<String>) {
    followersIds.value = ids
  }

  fun setFollowingIds(ids: List<String>) {
    followingIds.value = ids
  }

  fun addProfiles(newProfiles: List<Profile>) {
    newProfiles.forEach { profiles[it.uid] = it }
  }
}
