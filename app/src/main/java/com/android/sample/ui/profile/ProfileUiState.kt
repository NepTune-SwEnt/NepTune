package com.android.sample.ui.profile

enum class ProfileMode {
    VIEW,
    EDIT
}

data class ProfileUiState(
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
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