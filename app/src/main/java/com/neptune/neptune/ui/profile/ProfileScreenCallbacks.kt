package com.neptune.neptune.ui.profile

import androidx.compose.runtime.Immutable

/**
 * Strongly typed container for all user interaction callbacks on the profile screen.
 *
 * Keeps composables lightweight by bundling lambdas instead of passing them individually.
 */
@Immutable
data class ProfileScreenCallbacks(
    val onEditClick: () -> Unit,
    val onSaveClick: (name: String, username: String, bio: String) -> Unit,
    val onNameChange: (String) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onBioChange: (String) -> Unit,
    val onTagInputFieldChange: (String) -> Unit,
    val onTagSubmit: () -> Unit,
    val onRemoveTag: (String) -> Unit,
    val goBackClick: () -> Unit,
    val onAvatarEditClick: () -> Unit,
) {
  companion object {
    val Empty =
        ProfileScreenCallbacks(
            onEditClick = {},
            onSaveClick = { _, _, _ -> },
            onNameChange = {},
            onUsernameChange = {},
            onBioChange = {},
            onTagInputFieldChange = {},
            onTagSubmit = {},
            onRemoveTag = {},
            goBackClick = {},
            onAvatarEditClick = {},
        )
  }
}

/** Convenience builder that mirrors [ProfileScreenCallbacks] defaults for optional lambdas. */
fun profileScreenCallbacks(
    onEditClick: () -> Unit = {},
    onSaveClick: (String, String, String) -> Unit = { _, _, _ -> },
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
    onTagInputFieldChange: (String) -> Unit = {},
    onTagSubmit: () -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    goBackClick: () -> Unit = {},
    onAvatarEditClick: () -> Unit = {},
) =
    ProfileScreenCallbacks(
        onEditClick,
        onSaveClick,
        onNameChange,
        onUsernameChange,
        onBioChange,
        onTagInputFieldChange,
        onTagSubmit,
        onRemoveTag,
        goBackClick,
        onAvatarEditClick)
