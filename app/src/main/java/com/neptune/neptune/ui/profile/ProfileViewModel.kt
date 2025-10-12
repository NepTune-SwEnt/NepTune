package com.neptune.neptune.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and validating profile data.
 *
 * Holds the current [ProfileUiState] and exposes update functions for UI-driven changes (name,
 * username, bio). Simulates save operations (to be replaced with repository calls).
 */
class ProfileViewModel : ViewModel() {

  // FIXME: should be changed to real repo call
  private var savedProfile: ProfileUiState =
      ProfileUiState(
          name = "John Doe",
          username = "johndoe",
          bio = "I make sounds and share samples on NepTune.",
          followers = 1234,
          following = 56,
      )

  private val _uiState = MutableStateFlow(savedProfile)
  val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

  /** Enters edit mode and restores current saved profile data for editing. */
  fun onEditClick() {
    _uiState.value =
        _uiState.value
            .copy(
                mode = ProfileMode.EDIT,
                error = null,
                name = savedProfile.name,
                username = savedProfile.username,
                bio = savedProfile.bio)
            .validated()
  }

  /**
   * Updates the name field when in edit mode and revalidates the input.
   *
   * @param newName The new name entered by the user.
   */
  fun onNameChange(newName: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(name = newName).validated()
  }

  /**
   * Updates the username field when in edit mode and revalidates the input.
   *
   * @param newUsername The new username entered by the user.
   */
  fun onUsernameChange(newUsername: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(username = newUsername).validated()
  }

  /**
   * Updates the bio field when in edit mode and revalidates the input.
   *
   * @param newBio The new bio text entered by the user.
   */
  fun onBioChange(newBio: String) {
    if (_uiState.value.mode != ProfileMode.EDIT) return
    _uiState.value = _uiState.value.copy(bio = newBio).validated()
  }

  /**
   * Validates and saves the profile data.
   *
   * If validation succeeds, simulates a repository save and switches back to view mode. If any
   * field is invalid, the corresponding error message is displayed.
   */
  fun onSaveClick() {
    if (_uiState.value.mode != ProfileMode.EDIT || _uiState.value.isSaving) return

    val current = _uiState.value
    val validated = current.validated()
    _uiState.value = validated
    if (!validated.isValid) return

    // Simulate a save (repository call)
    viewModelScope.launch {
      _uiState.value = validated.copy(isSaving = true, error = null)

      // Simulate repo call
      savedProfile =
          savedProfile.copy(
              name = current.name.trim(),
              username = current.username.trim(),
              bio = current.bio.trim(),
          )
      // FIXME: should be real repo call

      // Exit edit mode with fresh snapshot
      _uiState.value =
          savedProfile.copy(
              mode = ProfileMode.VIEW,
              isSaving = false,
              error = null,
              nameError = null,
              usernameError = null,
              bioError = null)
    }
  }

  /**
   * Validates all editable fields in the current [ProfileUiState].
   *
   * @return A copy of the state with error fields populated if validation fails.
   */
  private fun ProfileUiState.validated(): ProfileUiState {
    val nameErr = validateName(name)
    val userErr = validateUsername(username)
    val bioErr = validateBio(bio)
    return copy(nameError = nameErr, usernameError = userErr, bioError = bioErr)
  }

  /**
   * Checks if the provided name meets length constraints.
   *
   * @param name The input name.
   * @return An error message if invalid, or null if valid.
   */
  private fun validateName(name: String): String? {
    return when {
      name.trim().length !in 2..30 -> "Name must be between 2 and 30 characters."
      else -> null
    }
  }

  /**
   * Validates username format and basic constraints.
   *
   * Usernames must:
   * - Start with a lowercase letter
   * - Contain only lowercase letters, numbers, or underscores
   * - Be 3–15 characters long
   *
   * @param username The input username.
   * @return An error message if invalid, or null if valid.
   */
  private fun validateUsername(username: String): String? {
    // TODO: check username availability via repo.
    val usernameRegex = Regex("^[a-z][a-z0-9_]{2,14}$")
    return if (!usernameRegex.matches(username.trim())) {
      "Username must be 3–15 chars, start with a letter, and contain only lowercase letters, numbers, or underscores."
    } else null
  }

  /**
   * Validates that the bio length does not exceed 160 characters.
   *
   * @param bio The input biography text.
   * @return An error message if too long, or null if valid.
   */
  private fun validateBio(bio: String): String? {
    return if (bio.length > 160) "Bio is too long (max 160 characters)." else null
  }
}
