package com.neptune.neptune.model.profile

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user profiles stored in Firestore.
 *
 * Provides suspend functions to create, read, and update user profiles, handle username claiming
 * and availability checks, and manage basic profile metadata such as name, bio, and avatar.
 *
 * @author Arianna Baur
 */
interface ProfileRepository {

  /**
   * Returns the current user's profile.
   *
   * @return snapshot of the current user's profile *
   */
  suspend fun getCurrentProfile(): Profile?

  /**
   * Returns the profile corresponding to the given user uid.
   *
   * @param uid the user ID of the profile to retrieve
   * @return snapshot of the current user's profile *
   */
  suspend fun getProfile(uid: String): Profile?

  /**
   * Observes the profile of the currently signed in user. Ensures real-time updates.
   *
   * @return a flow emitting the profile, or null if missing *
   */
  fun observeCurrentProfile(): Flow<Profile?>

  /**
   * Observes the profile of the given uid. Ensures real-time updates.
   *
   * @param uid the user ID of the profile to observe
   * @return a flow emitting the profile, or null if missing *
   */
  fun observeProfile(uid: String): Flow<Profile?>

  /**
   * This method is only related to the current user. Creates profiles/{uid} if missing on first
   * sign-in. Returns the created/loaded profile.
   *
   * @param suggestedUsernameBase a suggestion for the username's prefix
   * @param name the name of the user
   * @return the created/loaded profile
   */
  suspend fun ensureProfile(
      suggestedUsernameBase: String?,
      name: String?,
  ): Profile

  /**
   * This method is only related to the current user. Checks if the given username is available (not
   * taken by another user).
   */
  suspend fun isUsernameAvailable(username: String): Boolean

  /**
   * This method is only related to the current user. Claims a username and set it on
   * profiles/{uid}. Uses a Firestore transaction with usernames/{username} as a lock doc.
   *
   * @param newUsername the new username to claim
   * @Throws UsernameTakenException when the username is already in use.
   */
  @Throws(UsernameTakenException::class) suspend fun setUsername(newUsername: String)

  /**
   * This method is only related to the current user. Generates a random available username based on
   * the given base string.
   *
   * @return username composed as base + suffix
   */
  suspend fun generateRandomFreeUsername(base: String = "user"): String

  /**
   * This method is only related to the current user. Updates just the name of profiles/{uid},
   * without touching other fields.
   *
   * @param newName the new name
   */
  suspend fun updateName(newName: String)

  /**
   * This method is only related to the current user. Updates just the bio of profiles/{uid},
   * without touching other fields.
   *
   * @param newBio the new bio
   */
  suspend fun updateBio(newBio: String)

  /**
   * This method is only related to the current user. Updates just the avatarUrl of profiles/{uid},
   * without touching other fields.
   *
   * @param newUrl the new avatar URL
   */
  suspend fun updateAvatarUrl(newUrl: String)

  /**
   * This method is only related to the current user. Adds a new tag to profiles/{uid}.tags,
   * avoiding duplicates.
   *
   * @param tag the new tag to add
   */
  suspend fun addNewTag(tag: String)

  /**
   * This method is only related to the current user. Removes a tag from profiles/{uid}.tags.
   *
   * @param tag the tag to remove
   */
  suspend fun removeTag(tag: String)

  /**
   * This method is only related to the current user. Uploads image and updates
   * profiles/{uid}.photoUrl.
   *
   * @param localUri local Uri of the image to upload
   * @return the final URL.
   */
  suspend fun uploadAvatar(localUri: Uri): String

  /**
   * This method is only related to the current user. Clears photo (deletes storage object if
   * present, sets avatarUrl="", uses placeholder picture).
   */
  suspend fun removeAvatar()
}
