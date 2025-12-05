package com.neptune.neptune.model

import android.net.Uri
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.TAG_WEIGHT_MAX
import com.neptune.neptune.model.recommendation.RecoUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake Profile Repository for testing purposes. Updated to support multiple profiles and user
 * search.
 */
class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {

  // Store multiple profiles keyed by their User ID (uid)
  private val profiles = mutableMapOf<String, Profile>()

  // Track the ID of the currently logged-in user
  private var currentUserId: String? = initial?.uid

  // Additional mapping for testing specific username lookups strictly by ID
  private val usernames = mutableMapOf<String, String>()

  init {
    if (initial != null) {
      profiles[initial.uid] = initial
    }
  }

  // --- Test Helper Methods ---

  /** Adds a profile to the repository state for testing. */
  fun addProfile(profile: Profile) {
    profiles[profile.uid] = profile
  }

  /** Sets a username for a specific ID explicitly (useful for edge cases). */
  fun setUsernameForTest(userId: String, username: String) {
    usernames[userId] = username
  }

  // --- ProfileRepository Implementation ---

  override suspend fun getCurrentProfile(): Profile? {
    return currentUserId?.let { profiles[it] }
  }

  override suspend fun getProfile(uid: String): Profile? {
    return profiles[uid]
  }

  /**
   * Simulates searching for users by username or name. Note: Ensure your ProfileRepository
   * interface has this method defined.
   */
  override suspend fun searchUsers(query: String): List<Profile> {
    if (query.isBlank()) return emptyList()
    return profiles.values
        .filter {
          it.username.contains(query, ignoreCase = true) ||
              (it.name?.contains(query, ignoreCase = true) == true)
        }
        .toList()
  }

  override fun observeCurrentProfile(): Flow<Profile?> {
    return flowOf(currentUserId?.let { profiles[it] })
  }

  override fun observeProfile(uid: String): Flow<Profile?> {
    return flowOf(profiles[uid])
  }

  override suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile {
    // If we already have a current user, return them
    val existing = currentUserId?.let { profiles[it] }
    if (existing != null) return existing

    // Otherwise create a new one
    val newUid = "test-uid"
    val newProfile =
        Profile(
            uid = newUid,
            name = name ?: "Test User",
            username = (suggestedUsernameBase ?: "user") + "123",
            bio = "Test bio",
            avatarUrl = "",
            subscribers = 0,
            subscriptions = 0,
            likes = 0,
            posts = 0,
            tags = arrayListOf())

    profiles[newUid] = newProfile
    currentUserId = newUid
    return newProfile
  }

  override suspend fun updateName(newName: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        profiles[uid] = current.copy(name = newName)
      }
    }
  }

  override suspend fun updateBio(newBio: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        profiles[uid] = current.copy(bio = newBio)
      }
    }
  }

  override suspend fun getUserNameByUserId(userId: String): String? {
    return profiles[userId]?.username ?: usernames[userId]
  }

  override suspend fun getAvatarUrlByUserId(userId: String): String? {
    return profiles[userId]?.avatarUrl
  }

  // --- Stubs for methods not currently needed for Search testing ---

  override suspend fun unfollowUser(uid: String) {
    // Implement state change if you want to test following logic specifically
  }

  override suspend fun followUser(uid: String) {
    // Implement state change if you want to test following logic specifically
  }

  override suspend fun isUsernameAvailable(username: String): Boolean = true

  override suspend fun setUsername(newUsername: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        profiles[uid] = current.copy(username = newUsername)
      }
    }
  }

  override suspend fun generateRandomFreeUsername(base: String): String = "${base}123"

  override suspend fun updateAvatarUrl(newUrl: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        profiles[uid] = current.copy(avatarUrl = newUrl)
      }
    }
  }

  override suspend fun uploadAvatar(localUri: Uri): String = "fake_url"

  override suspend fun addNewTag(tag: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        val newTags = ArrayList(current.tags)
        if (!newTags.contains(tag)) {
          newTags.add(tag)
          profiles[uid] = current.copy(tags = newTags)
        }
      }
    }
  }

  override suspend fun removeTag(tag: String) {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        val newTags = ArrayList(current.tags)
        if (newTags.contains(tag)) {
          newTags.remove(tag)
          profiles[uid] = current.copy(tags = newTags)
        }
      }
    }
  }

  override suspend fun removeAvatar() {
    currentUserId?.let { uid ->
      val current = profiles[uid]
      if (current != null) {
        profiles[uid] = current.copy(avatarUrl = "")
      }
    }
  }

  override suspend fun getCurrentRecoUserProfile(): RecoUserProfile? {
    val p = profile ?: return null

    val tagsWeight = mutableMapOf<String, Double>()

    // 1) Use tagsWeight if present
    val raw = p.tagsWeight
    if (raw.isNotEmpty()) {
      for ((tag, weight) in raw) {
        if (weight >= 0.0) {
          tagsWeight[tag] = weight
        }
      }
    }

    // 2) Fallback: if no weights, give each tag weight 1.0
    if (tagsWeight.isEmpty()) {
      for (tag in p.tags) {
        tagsWeight[tag] = 1.0
      }
    }

    return RecoUserProfile(uid = p.uid, tagsWeight = tagsWeight)
  }

  override suspend fun recordTagInteraction(
      tags: List<String>,
      likeDelta: Int,
      downloadDelta: Int
  ) {
    if (tags.isEmpty()) return

    val delta = likeDelta * 2.0 + downloadDelta * 1.0
    if (delta == 0.0) return

    val p = profile ?: return

    val current = p.tagsWeight.toMutableMap()

    for (tag in tags) {
      val key = tag.trim().lowercase()
      val old = current[key] ?: 0.0
      val updated = (old + delta).coerceIn(0.0, TAG_WEIGHT_MAX)
      current[key] = updated
    }

    profile = p.copy(tagsWeight = current)
  }
}
