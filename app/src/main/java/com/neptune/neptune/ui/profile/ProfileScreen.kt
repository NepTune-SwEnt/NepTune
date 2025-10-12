package com.neptune.neptune.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.ui.theme.SampleAppTheme

/**
 * Centralized constants defining all `testTag` identifiers used in [ProfileScreen] UI tests.
 *
 * These tags are applied to key composable elements (e.g., buttons, fields, avatar) to make them
 * accessible and distinguishable within instrumented Compose UI tests.
 *
 * Naming follows the pattern: `"profile/<element>"`, ensuring uniqueness and consistency.
 *
 * Example usage in tests:
 * ```
 * composeTestRule.onNodeWithTag(ProfileScreenTestTags.EDIT_BUTTON).assertIsDisplayed()
 * ```
 *
 * @see testTag
 * @see ProfileScreenTest
 */
object ProfileScreenTestTags {
  const val ROOT = "profile/root"

  const val VIEW_CONTENT = "profile/view"
  const val EDIT_CONTENT = "profile/edit"

  const val AVATAR = "profile/avatar"
  const val NAME = "profile/name"
  const val USERNAME = "profile/username"
  const val BIO = "profile/bio"

  const val FOLLOWERS_BLOCK = "profile/stat/followers"
  const val FOLLOWING_BLOCK = "profile/stat/following"

  const val EDIT_BUTTON = "profile/btn/edit"
  const val SAVE_BUTTON = "profile/btn/save"

  const val FIELD_NAME = "profile/field/name"
  const val FIELD_USERNAME = "profile/field/username"
  const val FIELD_BIO = "profile/field/bio"

  fun statBlockTag(label: String) = "profile/stat/$label"
}

/**
 * Displays the main Profile screen, switching between view and edit modes.
 *
 * @param uiState The current [ProfileUiState] containing user data and screen mode.
 * @param onEditClick Callback invoked when the Edit button is pressed.
 * @param onSaveClick Callback invoked when the Save button is pressed.
 * @param onNameChange Called whenever the user edits their name field.
 * @param onUsernameChange Called whenever the user edits their username field.
 * @param onBioChange Called whenever the user edits their bio field.
 */
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditClick: () -> Unit = {},
    onSaveClick: (name: String, username: String, bio: String) -> Unit = { _, _, _ -> },
    onNameChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
) {
  // TODO: add profile picture, follower/following count and back button
  Column(modifier = Modifier.padding(16.dp).testTag(ProfileScreenTestTags.ROOT)) {
    when (uiState.mode) {
      ProfileMode.VIEW -> {
        ProfileViewContent(
            state = uiState,
            onEdit = onEditClick,
        )
      }
      ProfileMode.EDIT -> {
        ProfileEditContent(
            uiState = uiState,
            onSave = { onSaveClick(uiState.name, uiState.username, uiState.bio) },
            onNameChange = onNameChange,
            onUsernameChange = onUsernameChange,
            onBioChange = onBioChange)
      }
    }
  }
}

/**
 * Displays the profile screen in view-only mode.
 *
 * Shows the user's avatar, name, username, bio, and stats (followers/following), along with an Edit
 * button to enter edit mode.
 *
 * @param state The [ProfileUiState] containing the displayed user information.
 * @param onEdit Callback triggered when the Edit button is clicked.
 */
@Composable
private fun ProfileViewContent(
    state: ProfileUiState,
    onEdit: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().testTag(ProfileScreenTestTags.VIEW_CONTENT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(20.dp))
        Avatar(modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR), showEditPencil = false)
        Spacer(Modifier.height(40.dp))

        Text(
            text = state.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ProfileScreenTestTags.NAME))

        Text(
            text = "@${state.username}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(ProfileScreenTestTags.USERNAME))

        Spacer(Modifier.height(100.dp))

        Text(
            text = if (state.bio != "") "“ ${state.bio} ”" else "",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(ProfileScreenTestTags.BIO))
        Spacer(Modifier.height(200.dp))
        Row(Modifier.fillMaxWidth()) {
          StatBlock(
              label = "Followers",
              value = state.followers,
              modifier = Modifier.weight(1f),
              testTag = ProfileScreenTestTags.FOLLOWERS_BLOCK)
          StatBlock(
              label = "Following",
              value = state.following,
              modifier = Modifier.weight(1f),
              testTag = ProfileScreenTestTags.FOLLOWING_BLOCK)
        }
        Spacer(Modifier.height(80.dp))

        Button(onClick = onEdit, modifier = Modifier.testTag(ProfileScreenTestTags.EDIT_BUTTON)) {
          Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
          Spacer(Modifier.width(8.dp))
          Text("Edit")
        }
      }
}

/**
 * Displays the editable profile form.
 *
 * Provides text fields for editing the user's name, username, and bio. Includes validation messages
 * and a Save button.
 *
 * @param uiState The current [ProfileUiState].
 * @param onSave Called when the user presses the Save button.
 * @param onNameChange Called on text change in the name field.
 * @param onUsernameChange Called on text change in the username field.
 * @param onBioChange Called on text change in the bio field.
 */
@Composable
private fun ProfileEditContent(
    uiState: ProfileUiState,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxSize().testTag(ProfileScreenTestTags.EDIT_CONTENT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Spacer(modifier = Modifier.height(40.dp))

        Avatar(
            modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
            showEditPencil = true,
            onEditClick = { /* TODO: will open photo picker later */})
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_NAME),
            isError = uiState.nameError != null,
            supportingText = {
              val err = uiState.nameError
              if (err != null) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              } else {
                Text(
                    text = "${uiState.name.trim().length}/30",
                    style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_USERNAME),
            isError = uiState.usernameError != null,
            supportingText = {
              val err = uiState.usernameError
              if (err != null) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              } else {
                Text(
                    text = "${uiState.username.trim().length}/15",
                    style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_BIO),
            minLines = 3,
            isError = uiState.bioError != null,
            supportingText = {
              val err = uiState.bioError
              if (err != null) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
              } else {
                Text(text = "${uiState.bio.length}/160", style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving && uiState.isValid,
            modifier = Modifier.testTag(ProfileScreenTestTags.SAVE_BUTTON)) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
              Spacer(Modifier.width(8.dp))
              Text("Save")
            }
      }
}

/**
 * Displays a small statistics block for a given label and value (e.g. followers count).
 *
 * @param label The label text, e.g. "Followers".
 * @param value The numeric value to display.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
private fun StatBlock(label: String, value: Int, modifier: Modifier = Modifier, testTag: String) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        text = "$value",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(testTag))
  }
}

/**
 * Displays a circular avatar with an optional edit button overlay.
 *
 * When [showEditPencil] is true, a small floating action button appears in the corner.
 *
 * @param modifier Layout modifier for sizing and positioning.
 * @param sizeDp The diameter of the avatar circle, in dp.
 * @param showEditPencil Whether to show the edit pencil button.
 * @param onEditClick Callback triggered when the pencil button is clicked.
 */
@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    showEditPencil: Boolean,
    onEditClick: () -> Unit = {} // currently NO-OP
) {
  Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.BottomEnd) {
    Image(
        painter = painterResource(id = R.drawable.ic_avatar_placeholder),
        contentDescription = "Profile picture",
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.matchParentSize()
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))

    if (showEditPencil) {
      SmallFloatingActionButton(
          onClick = onEditClick, // no-op for now
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          shape = CircleShape,
          modifier = Modifier.align(Alignment.BottomEnd)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit avatar")
          }
    }
  }
}

/**
 * Previews the [ProfileScreen] in either view or edit mode.
 *
 * @param mode The [ProfileMode] to preview.
 */
@Composable
fun ProfileScreenPreview(mode: ProfileMode) {
  SampleAppTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      ProfileScreen(
          uiState =
              ProfileUiState(
                  name = "John Doe", username = "johndoe", bio = "I'm awesome", mode = mode))
    }
  }
}

/** Preview of the profile screen in view-only mode. */
@Preview
@Composable
fun ProfileScreenViewModePreview() {
  ProfileScreenPreview(ProfileMode.VIEW)
}

/** Preview of the profile screen in editable mode. */
@Preview
@Composable
fun ProfileScreenEditModePreview() {
  ProfileScreenPreview(ProfileMode.EDIT)
}

/**
 * Composable route for the Profile feature.
 *
 * Connects the [ProfileViewModel] to the [ProfileScreen] and handles state collection. This
 * function is typically used as the entry point for navigation to the profile screen.
 */
@Composable
fun ProfileRoute() {
  val viewModel: ProfileViewModel = viewModel()
  val state = viewModel.uiState.collectAsState().value

  ProfileScreen(
      uiState = state,
      onEditClick = viewModel::onEditClick,
      onSaveClick = { _, _, _ -> viewModel.onSaveClick() }, // VM reads from state
      onNameChange = viewModel::onNameChange,
      onUsernameChange = viewModel::onUsernameChange,
      onBioChange = viewModel::onBioChange,
  )
}
