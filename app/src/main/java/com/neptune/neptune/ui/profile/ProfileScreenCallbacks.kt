package com.neptune.neptune.ui.profile

import androidx.compose.runtime.Immutable

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
        )
  }
}

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
        goBackClick)
