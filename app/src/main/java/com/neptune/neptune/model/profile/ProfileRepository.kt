package com.neptune.neptune.model.profile

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

  /**
   * Returns the current user's profile.
   *
   * @return snapshot of the current user's profile *
   */
  suspend fun getProfile(): Profile?

  /**
   * Observes the profile of the given uid. Ensures real-time updates.
   *
   * @return a flow emitting the profile, or null if missing *
   */
  fun observeProfile(): Flow<Profile?>

  /**
   * Creates profiles/{uid} if missing on first sign-in. Returns the created/loaded profile.
   *
   * @param suggestedUsernameBase a suggestion for the username's prefix
   * @param name the name of the user
   * @return the created/loaded profile
   */
  suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile

  /** Checks if the given username is available (not taken by another user). */
  suspend fun isUsernameAvailable(username: String): Boolean

  /**
   * Claims a username and set it on profiles/{uid}. Uses a Firestore transaction with
   * usernames/{username} as a lock doc.
   *
   * @param newUsername the new username to claim
   * @Throws UsernameTakenException when the username is already in use.
   */
  @Throws(UsernameTakenException::class) suspend fun setUsername(newUsername: String)

  /**
   * Generates a random available username based on the given base string.
   *
   * @return username composed as base + suffix
   */
  suspend fun generateRandomFreeUsername(base: String = "user"): String

  /**
   * Updates just the name of profiles/{uid}, without touching other fields.
   *
   * @param newName the new name
   */
  suspend fun updateName(newName: String)

  /**
   * Updates just the bio of profiles/{uid}, without touching other fields.
   *
   * @param newBio the new bio
   */
  suspend fun updateBio(newBio: String)

  /**
   * Uploads image and updates profiles/{uid}.photoUrl.
   *
   * @param localUri local Uri of the image to upload
   * @return the final URL.
   */
  suspend fun uploadAvatar(localUri: Uri): String

  /**
   * Clears photo (deletes storage object if present, sets avatarUrl="", uses placeholder picture).
   */
  suspend fun removeAvatar()
}