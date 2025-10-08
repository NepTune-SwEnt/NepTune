package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel: ViewModel() {

    // FIXME: should be changed to real repo call
    private var savedProfile: ProfileUiState = ProfileUiState(
        name = "John Doe",
        username = "johndoe",
        bio = "I make sounds and share samples on NepTune.",
        followers = 1234,
        following = 56,
    )

    private val _uiState = MutableStateFlow(savedProfile)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onEditClick() {
        _uiState.value = _uiState.value.copy(
            mode = ProfileMode.EDIT,
            error = null,
            name = savedProfile.name,
            username = savedProfile.username,
            bio = savedProfile.bio
        ).validated()
    }

    fun onNameChange(newName: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(name = newName).validated()
    }

    fun onUsernameChange(newUsername: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(username = newUsername).validated()
    }

    fun onBioChange(newBio: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(bio = newBio).validated()
    }

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
            savedProfile = savedProfile.copy(
                name = current.name.trim(),
                username = current.username.trim(),
                bio = current.bio.trim(),
            )
            // FIXME: should be real repo call

            // Exit edit mode with fresh snapshot
            _uiState.value = savedProfile.copy(
                mode = ProfileMode.VIEW,
                isSaving = false,
                error = null,
                nameError = null,
                usernameError = null,
                bioError = null)
        }
    }

    private fun ProfileUiState.validated(): ProfileUiState {
        val nameErr = validateName(name)
        val userErr = validateUsername(username)
        val bioErr  = validateBio(bio)
        return copy(
            nameError = nameErr,
            usernameError = userErr,
            bioError = bioErr
        )
    }

    private fun validateName(name: String): String? {
        return when {
            name.trim().length !in 2..30 -> "Name must be between 2 and 30 characters."
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        // TODO: check username availability via repo.
        val usernameRegex = Regex("^[a-z][a-z0-9_]{2,14}$")
        return if (!usernameRegex.matches( username.trim())) {
            "Username must be 3–15 chars, start with a letter, and contain only lowercase letters, numbers, or underscores."
        } else null
    }

    private fun validateBio(bio: String): String? {
        return if (bio.length > 160) "Bio is too long (max 160 characters)." else null
    }

}