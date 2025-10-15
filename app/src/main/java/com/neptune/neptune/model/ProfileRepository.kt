package com.neptune.neptune.model

interface ProfileRepository {

    /** Returns the current user's profile.
     * @param uid Firebase uid associated to user
     * @return snapshot of the current user's profile**/
    suspend fun getProfile(uid: String): Profile?

    /** Creates profiles/{uid} if missing on first sign-in. Returns the created/loaded profile.
     * @param uid Firebase uid associated to user
     * @param suggestedUsernameBase a suggestion for the username's prefix
     * @param name the name of the user
     * @return the created/loaded profile*/
    suspend fun ensureProfile(
        uid: String,
        suggestedUsernameBase: String?,
        name: String?,
    ): Profile

    /** Checks if the given username is available (not taken by another user). */
    suspend fun isUsernameAvailable(username: String): Boolean

    /** Claims a username and set it on profiles/{uid}.
     * Uses a Firestore transaction with usernames/{username} as a lock doc.
     * @Throws UsernameTakenException when the username is already in use. */
    @Throws(UsernameTakenException::class)
    suspend fun setUsername(uid: String, newUsername: String)

    /** Generates a random available username based on the given base string.
     * @return username composed as base + suffix */
    suspend fun generateRandomFreeUsername(base: String = "user"): String

    /** Updates just the name of profiles/{uid}, without touching other fields.
     * @param uid Firebase uid associated to user
     * @param newName the new name */
    suspend fun updateName(uid: String, newName: String)

    /** Updates just the bio of profiles/{uid}, without touching other fields.
     * @param uid Firebase uid associated to user
     * @param newBio the new bio */
    suspend fun updateBio(uid: String, newBio: String)

    /** Uploads image and updates profiles/{uid}.photoUrl.
     * @param uid Firebase uid associated to user
     * @param localUri local Uri of the image to upload
     * @return the final URL. */
    suspend fun uploadAvatar(uid: String, localUri: android.net.Uri): String

    /** Clears photo (deletes storage object if present, sets avatarUrl="", uses placeholder picture).
     * @param uid Firebase uid associated to user */
    suspend fun removeAvatar(uid: String)
}