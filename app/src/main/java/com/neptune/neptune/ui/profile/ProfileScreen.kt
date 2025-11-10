package com.neptune.neptune.ui.profile

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neptune.neptune.R
import com.neptune.neptune.data.rememberImagePickerLauncher
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.theme.NepTuneTheme

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
  const val POSTS_BLOCK = "profile/stat/posts"
  const val LIKES_BLOCK = "profile/stat/likes"

  const val TAGS_VIEW_SECTION = "profile/view/tags"
  const val TAGS_EDIT_SECTION = "profile/edit/tags"

  const val EDIT_BUTTON = "profile/btn/edit"
  const val SAVE_BUTTON = "profile/btn/save"
  const val ADD_TAG_BUTTON = "profile/btn/add_tag"
  const val SETTINGS_BUTTON = "profile/btn/settings"
  const val GOBACK_BUTTON = "profile/btn/goback"

  const val FIELD_NAME = "profile/field/name"
  const val FIELD_USERNAME = "profile/field/username"
  const val FIELD_BIO = "profile/field/bio"
  const val FIELD_ADD_TAG = "profile/field/add_tag"
}

/**
 * Displays the main Profile screen, switching between view and edit modes.
 *
 * @param uiState The current [ProfileUiState] containing user data and screen mode.
 * @param callbacks The [ProfileScreenCallbacks] for handling user interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    localAvatarUri: Uri? = null,
    callbacks: ProfileScreenCallbacks = ProfileScreenCallbacks.Empty,
    onAvatarEditClick: () -> Unit = {}
) {
  Column(modifier = Modifier.padding(16.dp).testTag(ProfileScreenTestTags.ROOT)) {
    when (uiState.mode) {
      ProfileMode.VIEW -> {
        ProfileViewContent(
            state = uiState,
            onEdit = callbacks.onEditClick,
            localAvatarUri = localAvatarUri,
            settings = callbacks.onSettingsClick,
            goBack = callbacks.goBackClick)
      }
      ProfileMode.EDIT -> {
        ProfileEditContent(
            uiState = uiState,
            localAvatarUri = localAvatarUri,
            onSave = { callbacks.onSaveClick(uiState.name, uiState.username, uiState.bio) },
            onNameChange = callbacks.onNameChange,
            onUsernameChange = callbacks.onUsernameChange,
            onBioChange = callbacks.onBioChange,
            onTagInputFieldChange = callbacks.onTagInputFieldChange,
            onTagSubmit = callbacks.onTagSubmit,
            onRemoveTag = callbacks.onRemoveTag,
            onAvatarEditClick = onAvatarEditClick)
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
 * @param goBack Callback triggered when the Go Back button is clicked.
 * @param settings Callback triggered when the Settings button is clicked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileViewContent(
    state: ProfileUiState,
    localAvatarUri: Uri?,
    onEdit: () -> Unit,
    goBack: () -> Unit,
    settings: () -> Unit,
) {
  Scaffold(
      modifier = Modifier.testTag(ProfileScreenTestTags.ROOT),
      topBar = {
        Column {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = goBack,
                    modifier = Modifier.testTag(ProfileScreenTestTags.GOBACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.ArrowBackIosNew,
                          contentDescription = "Go Back",
                          tint = NepTuneTheme.colors.onBackground)
                    }
                IconButton(
                    modifier = Modifier.size(30.dp).testTag(ProfileScreenTestTags.SETTINGS_BUTTON),
                    onClick = settings) {
                      Icon(
                          modifier = Modifier.size(30.dp),
                          imageVector = Icons.Default.Settings,
                          contentDescription = "Logout",
                          tint = NepTuneTheme.colors.onBackground)
                    }
              }
        }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        // This box was made using AI assistance
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .verticalScroll(rememberScrollState())
                      .padding(bottom = 88.dp)
                      .testTag(ProfileScreenTestTags.VIEW_CONTENT),
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Spacer(Modifier.height(15.dp))

            val avatarModel = localAvatarUri ?: state.avatarUrl ?: R.drawable.ic_avatar_placeholder
            Avatar(
                avatarModel,
                modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
                showEditPencil = false)

            Spacer(Modifier.height(15.dp))

            Text(
                text = state.name,
                color = NepTuneTheme.colors.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ProfileScreenTestTags.NAME))

            Text(
                text = "@${state.username}",
                color = NepTuneTheme.colors.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(ProfileScreenTestTags.USERNAME))

            Spacer(Modifier.height(40.dp))

            Row(Modifier.fillMaxWidth()) {
              StatBlock(
                  label = "Posts",
                  value = state.posts,
                  modifier = Modifier.weight(1f),
                  testTag = ProfileScreenTestTags.POSTS_BLOCK)
              StatBlock(
                  label = "Likes",
                  value = state.likes,
                  modifier = Modifier.weight(1f),
                  testTag = ProfileScreenTestTags.LIKES_BLOCK)
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

            Spacer(Modifier.height(100.dp))

            Text(
                text = if (state.bio != "") "“${state.bio}”" else "",
                color = NepTuneTheme.colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(ProfileScreenTestTags.BIO))
            Spacer(Modifier.height(100.dp))

            if (state.tags.isNotEmpty()) {
              Spacer(Modifier.height(16.dp))
              FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.testTag(ProfileScreenTestTags.TAGS_VIEW_SECTION)) {
                    state.tags.forEach { tag ->
                      InputChip(
                          selected = false,
                          onClick = {},
                          enabled = false,
                          label = { Text(tag) },
                          colors =
                              InputChipDefaults.inputChipColors(
                                  disabledContainerColor = NepTuneTheme.colors.cardBackground,
                                  disabledLabelColor = NepTuneTheme.colors.onBackground),
                          border =
                              InputChipDefaults.inputChipBorder(
                                  borderWidth = 0.dp, enabled = false, selected = false))
                    }
                  }
            }

            Spacer(Modifier.height(50.dp))
          }

          Button(
              onClick = onEdit,
              enabled = true,
              modifier =
                  Modifier.align(Alignment.BottomCenter)
                      .padding(bottom = 24.dp)
                      .testTag(ProfileScreenTestTags.EDIT_BUTTON)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                Spacer(Modifier.width(8.dp))
                Text("Edit")
              }
        }
      }
}

/** Colors for all OutlinedTextField components. */
@OptIn(ExperimentalMaterial3Api::class)
val TextFieldColors: @Composable () -> TextFieldColors = {
  TextFieldDefaults.outlinedTextFieldColors(
      unfocusedBorderColor = NepTuneTheme.colors.onBackground,
      focusedBorderColor = NepTuneTheme.colors.onBackground,
      disabledBorderColor = NepTuneTheme.colors.onBackground,
      cursorColor = NepTuneTheme.colors.onBackground,
      focusedLabelColor = NepTuneTheme.colors.onBackground,
      unfocusedLabelColor = NepTuneTheme.colors.onBackground,
      focusedTextColor = NepTuneTheme.colors.onBackground,
      unfocusedTextColor = NepTuneTheme.colors.onBackground,
      disabledTextColor = NepTuneTheme.colors.onBackground,
  )
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditContent(
    uiState: ProfileUiState,
    localAvatarUri: Uri?,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onTagInputFieldChange: (String) -> Unit,
    onTagSubmit: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAvatarEditClick: () -> Unit
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .testTag(ProfileScreenTestTags.EDIT_CONTENT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Spacer(modifier = Modifier.height(40.dp))

        val avatarModel = localAvatarUri ?: uiState.avatarUrl ?: R.drawable.ic_avatar_placeholder

        Avatar(
            model = avatarModel,
            modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
            showEditPencil = true,
            onEditClick = onAvatarEditClick)
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            colors = TextFieldColors(),
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
                    color = NepTuneTheme.colors.onBackground,
                    style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            colors = TextFieldColors(),
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
                    color = NepTuneTheme.colors.onBackground,
                    text = "${uiState.username.trim().length}/15",
                    style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = uiState.bio,
            onValueChange = onBioChange,
            label = { Text("Bio") },
            colors = TextFieldColors(),
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
                Text(
                    text = "${uiState.bio.length}/160",
                    color = NepTuneTheme.colors.onBackground,
                    style = MaterialTheme.typography.bodySmall)
              }
            })

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
              value = uiState.inputTag,
              onValueChange = onTagInputFieldChange,
              label = { Text("My music genre") },
              colors = TextFieldColors(),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag(ProfileScreenTestTags.FIELD_ADD_TAG),
              supportingText = {
                Text(
                    text =
                        buildString {
                          append("${uiState.inputTag.trim().length}/20")
                          if (uiState.tagError != null) append(" • ${uiState.tagError}")
                        },
                    color =
                        if (uiState.tagError != null) MaterialTheme.colorScheme.error
                        else NepTuneTheme.colors.onBackground,
                    style = MaterialTheme.typography.bodySmall)
              })
          Spacer(Modifier.width(12.dp))
          Button(
              onClick = onTagSubmit,
              modifier = Modifier.fillMaxHeight().testTag(ProfileScreenTestTags.ADD_TAG_BUTTON)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
              }
        }

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag(ProfileScreenTestTags.TAGS_EDIT_SECTION)) {
              uiState.tags.forEach { tag ->
                key(tag) { // <— ensures slot stability and proper disposal on removal
                  EditableTagChip(
                      tagText = tag,
                      onRemove = onRemoveTag,
                      modifier = Modifier.testTag("profile/tag/chip/${tag.replace(' ', '_')}"))
                }
              }
            }

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
fun StatBlock(label: String, value: Int, modifier: Modifier = Modifier, testTag: String) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = label,
        color = NepTuneTheme.colors.onBackground,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        text = "$value",
        color = NepTuneTheme.colors.onBackground,
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
 * @param model The image model for Coil to load (URL, Uri, resource ID, etc.).
 * @param modifier Layout modifier for sizing and positioning.
 * @param sizeDp The diameter of the avatar circle, in dp.
 * @param showEditPencil Whether to show the edit pencil button.
 * @param onEditClick Callback triggered when the pencil button is clicked.
 */
@Composable
fun Avatar(
    model: Any,
    sizeDp: Int = 120,
    modifier: Modifier = Modifier,
    showEditPencil: Boolean,
    onEditClick: () -> Unit = {}
) {
  Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.BottomEnd) {
    AsyncImage(
        model = model,
        contentDescription = "Avatar",
        modifier =
            Modifier.fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, NepTuneTheme.colors.accentPrimary, CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
        error = painterResource(id = R.drawable.ic_avatar_placeholder))
    if (showEditPencil) {
      SmallFloatingActionButton(
          onClick = onEditClick,
          modifier = Modifier.align(Alignment.BottomEnd),
          shape = CircleShape,
          containerColor = NepTuneTheme.colors.accentPrimary,
          contentColor = NepTuneTheme.colors.onBackground) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Avatar")
          }
    }
  }
}

/**
 * Displays an editable tag chip with a remove icon.
 *
 * @param tagText The text to display inside the chip.
 * @param onRemove Callback invoked when the remove icon is clicked.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
fun EditableTagChip(tagText: String, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
  InputChip(
      modifier = modifier,
      selected = false,
      onClick = {},
      label = { Text(text = tagText) },
      trailingIcon = {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove tag",
            tint = NepTuneTheme.colors.onBackground,
            modifier =
                Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                          onRemove(tagText)
                        }
                    .testTag("profile/tag/remove/${tagText.replace(' ', '_')}"))
      },
      colors =
          InputChipDefaults.inputChipColors(
              containerColor = NepTuneTheme.colors.background,
              labelColor = NepTuneTheme.colors.onBackground,
          ))
}

/**
 * Composable route for the Profile feature.
 *
 * Connects the [ProfileViewModel] to the [ProfileScreen] and handles state collection. This
 * function is typically used as the entry point for navigation to the profile screen.
 *
 * This method was made using AI assistance
 */
@Composable
fun ProfileRoute(settings: () -> Unit = {}, goBack: () -> Unit = {}) {

  val factory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repo = ProfileRepositoryProvider.repository) as T
          }
          throw IllegalArgumentException("Unknown ViewModel class")
        }
      }

  val viewModel: ProfileViewModel = viewModel(factory = factory)
  val state = viewModel.uiState.collectAsState().value
  val localAvatarUri by viewModel.localAvatarUri.collectAsState()
  val tempAvatarUri by viewModel.tempAvatarUri.collectAsState()

  LaunchedEffect(Unit) { viewModel.loadOrEnsure() }

  // Prepare the image picker launcher. The callback will notify the ViewModel.
  val imagePickerLauncher =
      rememberImagePickerLauncher(
          onImageCropped = { croppedUri: Uri? ->
            if (croppedUri != null) {
              viewModel.onAvatarCropped(croppedUri)
            }
          })

  ProfileScreen(
      uiState = state,
      localAvatarUri =
          if (tempAvatarUri != null) {
            tempAvatarUri
          } else {
            localAvatarUri
          },
      onAvatarEditClick = { imagePickerLauncher.launch("image/*") }, // Launch the picker
      callbacks =
          profileScreenCallbacks(
              onEditClick = viewModel::onEditClick,
              onSaveClick = { _, _, _ -> viewModel.onSaveClick() }, // VM reads from state
              onNameChange = viewModel::onNameChange,
              onUsernameChange = viewModel::onUsernameChange,
              onBioChange = viewModel::onBioChange,
              onTagInputFieldChange = viewModel::onTagInputFieldChange,
              onTagSubmit = viewModel::onTagAddition,
              onRemoveTag = viewModel::onTagDeletion,
              onSettingsClick = settings,
              goBackClick = goBack))
}
