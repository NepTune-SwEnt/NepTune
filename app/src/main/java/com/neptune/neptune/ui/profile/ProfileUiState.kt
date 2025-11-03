package com.neptune.neptune.ui.profile

/** Represents the current interaction mode of the profile screen. */
enum class ProfileMode {
  VIEW,
  EDIT
}

/**
 * Represents the complete UI state of the Profile screen.
 *
 * @property name The user's display name.
 * @property username The user's unique username.
 * @property bio The user's biography text.
 * @property avatarUrl Optional URL to the user's avatar image.
 * @property followers Number of followers.
 * @property following Number of followed accounts.
 * @property mode The current display mode ([ProfileMode.VIEW] or [ProfileMode.EDIT]).
 * @property isSaving True if a save operation is currently in progress.
 * @property error General error message, if any.
 * @property nameError Validation error message for the name field.
 * @property usernameError Validation error message for the username field.
 * @property bioError Validation error message for the bio field.
 * @property isValid True if all field validations pass (no errors).
 */
data class ProfileUiState(
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val likes: Int = 0,
    val posts: Int = 0,
    val mode: ProfileMode = ProfileMode.VIEW,
    val isSaving: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val usernameError: String? = null,
    val bioError: String? = null
) {
  val isValid: Boolean
    get() = nameError == null && usernameError == null && bioError == null
}
