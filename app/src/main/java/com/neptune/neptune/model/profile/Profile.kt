package com.neptune.neptune.model.profile

/**
 * Immutable representation of a user profile stored in Firestore.
 *
 * Each profile corresponds to a document under `profiles/{uid}` and contains public user
 * information such as username, display name, bio, and followers/following count.
 *
 * @property uid Unique identifier of the user (Firebase Auth UID).
 * @property username Chosen unique username, also mirrored under `usernames/{username}`.
 * @property name Display name shown in the app UI (defaults to username if not set).
 * @property bio Short user description or status message.
 * @property subscriptions Number of other users this profile follows.
 * @property subscribers Number of users following this profile.
 * @property avatarUrl URL of the profile picture (empty string if not set).
 * @author Arianna Baur
 */
data class Profile(
    val uid: String,
    val username: String = "",
    val name: String? = null,
    val bio: String? = null,
    val subscriptions: Long = 0,
    val subscribers: Long = 0,
    val avatarUrl: String = ""
)

/**
 * Exception thrown when attempting to claim a username that is already taken by another user in the
 * `usernames` collection.
 *
 * @param username The username that caused the conflict.
 */
class UsernameTakenException(username: String) : Exception("Username taken: $username")
