package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel: ViewModel() {

    // FIXME: added a fake "persisted" snapshot to support Cancel and change detection,
    //  should be changed to real repo call
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
        )
    }

    fun onNameChange(newName: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(name = newName)
    }

    fun onUsernameChange(newUsername: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun onBioChange(newBio: String) {
        if (_uiState.value.mode != ProfileMode.EDIT) return
        _uiState.value = _uiState.value.copy(bio = newBio)
    }

    fun onSaveClick() {
        if (_uiState.value.mode != ProfileMode.EDIT || _uiState.value.isSaving) return

        val current = _uiState.value
        val validationError = validate(current)
        if (validationError != null) {
            _uiState.value = current.copy(error = validationError)
            return
        }

        // Simulate a save (repository call)
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, error = null)

            savedProfile = savedProfile.copy(
                name = current.name.trim(),
                username = current.username.trim(),
                bio = current.bio.trim(),
            )
            // FIXME: should be real repo call

            // Exit edit mode with fresh snapshot
            _uiState.value = savedProfile.copy(mode = ProfileMode.VIEW, isSaving = false, error = null)
        }
    }

    private fun validate(state: ProfileUiState): String? {
        // Name: 2..50 non-blank
        val name = state.name.trim()
        if (name.length !in 2..50) return "Name must be between 2 and 50 characters."

        // Username: 3..15, starts with letter, lowercase letters/numbers/underscore
        val username = state.username.trim()

        val usernameRegex = Regex("^[a-z][a-z0-9_]{2,14}$")
        val usernameOk = usernameRegex.matches(username)
        if (!usernameOk) return "Username must be 3–15 chars, start with a letter, and contain only lowercase letters, numbers, or underscores."

        // Bio: <= 160 chars (adjust as needed)
        if (state.bio.length > 160) return "Bio is too long (max 160 characters)."

        // TODO:
        //  * check username availability via repo.
        //  * handle error messages in red in the UI (with supportingText parameter of OutlinedTextField)

        return null
    }

}