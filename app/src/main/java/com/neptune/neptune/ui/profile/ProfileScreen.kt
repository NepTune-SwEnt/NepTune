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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.R
import com.neptune.neptune.data.rememberImagePickerLauncher
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.feed.sampleFeedItems
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.DownloadChoiceDialog
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.offline.OfflineBanner
import com.neptune.neptune.ui.profile.ProfileScreenTestTags.DOWNLOAD_PROGRESS_BAR
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.NetworkConnectivityObserver

private val ScreenPadding = 16.dp
private val SettingsButtonSize = 30.dp
private val AvatarVerticalSpacing = 15.dp
private val SectionVerticalSpacing = 40.dp
private val BottomButtonBottomPadding = 24.dp
private val ButtonIconSpacing = 8.dp
private val TagsSpacing = 8.dp
private val StatBlockLabelSpacing = 8.dp
private val SamplesSpacing = 20.dp
private const val UNKOWN_CLASS = "Unknown ViewModel class"

/**
 * Centralized constants defining all `testTag` identifiers used in [ProfileScreen] UI tests.
 *
 * These tags are applied to key composable elements (e.g., buttons, fields, avatar) to make them
 * accessible and distinguishable within instrumented Compose UI tests.
 *
 * Naming follows the pattern: `"profile/<element>"`, ensuring uniqueness and consistency.
 *
 * @see testTag
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
  const val FOLLOW_BUTTON = "profile/btn/follow"
  const val FIELD_NAME = "profile/field/name"
  const val FIELD_USERNAME = "profile/field/username"
  const val FIELD_BIO = "profile/field/bio"
  const val DOWNLOAD_PROGRESS_BAR = "profile/downloadProgressBar"
  const val FIELD_ADD_TAG = "profile/field/add_tag"
}

/**
 * Displays the main Profile screen, switching between view (self or other user) and edit modes.
 *
 * @param uiState The current [SelfProfileUiState] containing user data and screen mode.
 * @param callbacks The [ProfileScreenCallbacks] for handling user interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: SelfProfileUiState,
    localAvatarUri: Uri? = null,
    callbacks: ProfileScreenCallbacks = ProfileScreenCallbacks.Empty,
    onAvatarEditClick: () -> Unit = {},
    viewConfig: ProfileViewConfig,
    profileSamplesViewModel: ProfileSamplesViewModel,
    isOnline: Boolean = true,
    isUserLoggedIn: Boolean = true
) {
  Column(modifier = Modifier.padding(ScreenPadding).testTag(ProfileScreenTestTags.ROOT)) {
    when (uiState.mode) {
      // Create profile screen view content
      ProfileMode.VIEW -> {
        ProfileViewContent(
            state = uiState,
            localAvatarUri = localAvatarUri,
            viewConfig = viewConfig,
            goBack = callbacks.goBackClick,
            profileSamplesViewModel = profileSamplesViewModel,
            isOnline = isOnline,
            isUserLoggedIn = isUserLoggedIn)
      }
      // Create profile screen edit content
      ProfileMode.EDIT -> {
        if (!isOnline) OfflineBanner()
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
            onAvatarEditClick = onAvatarEditClick,
            isOnline = isOnline)
      }
    }
  }
}

@Composable
private fun SettingsButton(settings: () -> Unit) {
  IconButton(
      modifier = Modifier.size(SettingsButtonSize).testTag(ProfileScreenTestTags.SETTINGS_BUTTON),
      onClick = settings) {
        Icon(
            modifier = Modifier.size(SettingsButtonSize),
            imageVector = Icons.Default.Settings,
            contentDescription = "Logout",
            tint = NepTuneTheme.colors.onBackground)
      }
}

sealed interface ProfileViewConfig {
  val topBarContent: (@Composable () -> Unit)?
  val belowStatsButton: (@Composable () -> Unit)?
  val bottomScreenButton: (@Composable (modifier: Modifier) -> Unit)?
  val onFollowingClick: (() -> Unit)?
  val onFollowersClick: (() -> Unit)?

  data class SelfProfileConfig(
      private val onEdit: () -> Unit,
      private val settings: () -> Unit,
      val canEditProfile: Boolean = true,
      override val onFollowingClick: (() -> Unit)? = null,
      override val onFollowersClick: (() -> Unit)? = null,
      val isOnline: Boolean = true
  ) : ProfileViewConfig {
    override val topBarContent = @Composable { SettingsButton(settings) }
    override val belowStatsButton = null
    override val bottomScreenButton =
        @Composable { modifier: Modifier ->
          if (canEditProfile) {
            Button(
                onClick = onEdit,
                enabled = isOnline,
                modifier =
                    modifier
                        .padding(bottom = BottomButtonBottomPadding)
                        .testTag(ProfileScreenTestTags.EDIT_BUTTON)) {
                  Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                  Spacer(Modifier.width(ButtonIconSpacing))
                  Text("Edit")
                }
          }
        }
  }

  data class OtherProfileConfig(
      val isFollowing: Boolean,
      val isFollowActionInProgress: Boolean = false,
      val canFollowTarget: Boolean = true,
      private val onFollow: () -> Unit,
      private val errorMessage: String?,
      override val onFollowingClick: (() -> Unit)? = null,
      override val onFollowersClick: (() -> Unit)? = null,
      val isOnline: Boolean = true
  ) : ProfileViewConfig {
    override val topBarContent = null
    override val belowStatsButton: @Composable () -> Unit = composable@{
      if (!canFollowTarget) return@composable
      val label = if (isFollowing) "Unfollow" else "Follow"
      val icon = if (isFollowing) Icons.Default.Clear else Icons.Default.Add

      Column(
          modifier = Modifier.padding(bottom = BottomButtonBottomPadding),
          horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onFollow,
                enabled = !isFollowActionInProgress && isOnline,
                modifier = Modifier.testTag(ProfileScreenTestTags.FOLLOW_BUTTON)) {
                  Icon(imageVector = icon, contentDescription = "Follow")
                  Spacer(Modifier.width(ButtonIconSpacing))
                  Text(label)
                }
            if (!errorMessage.isNullOrBlank()) {
              Spacer(Modifier.height(8.dp))
              Text(
                  text = errorMessage,
                  color = Color.Red,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.testTag("profile/follow_error"))
            }
          }
    }
    override val bottomScreenButton = null
  }
}

/**
 * Displays the profile screen in view-only mode.
 *
 * Shows the user's avatar, name, username, bio, and stats (followers/following), along with an Edit
 * button to enter edit mode.
 *
 * @param state The [SelfProfileUiState] containing the displayed user information.
 * @param localAvatarUri Optional local URI for the avatar image (overrides remote URL if present).
 * @param goBack Callback triggered when the Go Back button is clicked.
 * @param viewConfig Configuration for view-specific UI elements.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileViewContent(
    state: SelfProfileUiState,
    localAvatarUri: Uri?,
    goBack: () -> Unit,
    viewConfig: ProfileViewConfig,
    profileSamplesViewModel: ProfileSamplesViewModel,
    isOnline: Boolean = true,
    isUserLoggedIn: Boolean = true
) {
  var downloadPickerSample by remember { mutableStateOf<Sample?>(null) }
  val downloadProgress: Int? by profileSamplesViewModel.downloadProgress.collectAsState()
  val samples by profileSamplesViewModel.samples.collectAsState()
  val likedSamples by profileSamplesViewModel.likedSamples.collectAsState()
  val activeCommentSampleId by profileSamplesViewModel.activeCommentSampleId.collectAsState()
  val comments by profileSamplesViewModel.comments.collectAsState()
  val sampleResources by profileSamplesViewModel.sampleResources.collectAsState()
  val usernames by profileSamplesViewModel.usernames.collectAsState()
  val mediaPlayer = LocalMediaPlayer.current
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val cardWidth = screenWidth - 20.dp
  val cardHeight = cardWidth * (150f / 166f)
  val samplesListTagModifier =
      if (samples.isNotEmpty()) Modifier.testTag("profile/samples/list") else Modifier

  Scaffold(
      modifier = Modifier.testTag(ProfileScreenTestTags.ROOT),
      topBar = {
        Column {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                // Go Back Button
                IconButton(
                    onClick = goBack,
                    modifier = Modifier.testTag(ProfileScreenTestTags.GOBACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.ArrowBackIosNew,
                          contentDescription = "Go Back",
                          tint = NepTuneTheme.colors.onBackground)
                    }
                viewConfig.topBarContent?.invoke()
              }
        }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        Box(
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .testTag(ProfileScreenTestTags.VIEW_CONTENT)) {
              Column(modifier = Modifier.fillMaxSize()) {
                if (!isOnline && isUserLoggedIn) {
                  OfflineBanner()
                }
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize().padding(bottom = 88.dp).then(samplesListTagModifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  item { Spacer(Modifier.height(AvatarVerticalSpacing)) }

                  // Avatar image
                  item {
                    val avatarModel =
                        localAvatarUri ?: state.avatarUrl ?: R.drawable.ic_avatar_placeholder
                    Avatar(
                        model = avatarModel,
                        modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
                        showEditPencil = false)
                  }

                  item { Spacer(Modifier.height(AvatarVerticalSpacing)) }

                  // Name and username
                  item {
                    Text(
                        text = state.name,
                        color = NepTuneTheme.colors.onBackground,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(ProfileScreenTestTags.NAME))
                  }
                  item {
                    Text(
                        text = "@${state.username}",
                        color = NepTuneTheme.colors.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(ProfileScreenTestTags.USERNAME))
                  }
                  item { Spacer(Modifier.height(SectionVerticalSpacing)) }

                  // Stats row
                  item {
                    StatRow(
                        state = state,
                        onFollowersClick = viewConfig.onFollowersClick,
                        onFollowingClick = viewConfig.onFollowingClick)
                  }
                  item { Spacer(Modifier.height(SectionVerticalSpacing)) }

                  // if view mode is for other users profile, show follow button
                  viewConfig.belowStatsButton?.let { button -> item { button() } }

                  // Bio
                  item {
                    Text(
                        text = if (state.bio != "") "“${state.bio}”" else "",
                        color = NepTuneTheme.colors.onBackground,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(ProfileScreenTestTags.BIO))
                  }

                  // Tags
                  if (state.tags.isNotEmpty()) {
                    item { Spacer(Modifier.height(TagsSpacing)) }
                    item {
                      FlowRow(
                          horizontalArrangement = Arrangement.spacedBy(TagsSpacing),
                          verticalArrangement = Arrangement.spacedBy(TagsSpacing),
                          modifier = Modifier.testTag(ProfileScreenTestTags.TAGS_VIEW_SECTION)) {
                            state.tags.forEach { tag ->
                              InputChip(
                                  selected = false,
                                  onClick = {},
                                  enabled = false,
                                  label = { Text(tag) },
                                  colors =
                                      InputChipDefaults.inputChipColors(
                                          disabledContainerColor =
                                              NepTuneTheme.colors.cardBackground,
                                          disabledLabelColor = NepTuneTheme.colors.onBackground),
                                  border =
                                      InputChipDefaults.inputChipBorder(
                                          borderWidth = 0.dp, enabled = false, selected = false))
                            }
                          }
                    }
                  }

                  item { Spacer(Modifier.height(SectionVerticalSpacing)) }

                  // Posted samples section
                  item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      Text(
                          text = "Posted samples",
                          style = MaterialTheme.typography.headlineSmall,
                          color = NepTuneTheme.colors.onBackground)
                      Spacer(Modifier.height(SamplesSpacing))
                    }
                  }

                  if (samples.isEmpty()) {
                    item {
                      Text(
                          text = "No samples posted yet.",
                          style = MaterialTheme.typography.bodyMedium,
                          color = NepTuneTheme.colors.onBackground)
                    }
                  } else {
                    sampleFeedItems(
                        samples = samples,
                        controller = profileSamplesViewModel,
                        mediaPlayer = mediaPlayer,
                        likedSamples = likedSamples,
                        sampleResources = sampleResources,
                        onDownloadRequest = { sample -> downloadPickerSample = sample },
                        navigateToProfile = {},
                        navigateToOtherUserProfile = {},
                        testTagsForSample = {
                          object : BaseSampleTestTags {
                            override val prefix = "sampleList"
                          }
                        },
                        width = cardWidth,
                        height = cardHeight)
                  }
                }

                activeCommentSampleId?.let { activeId ->
                  CommentDialog(
                      sampleId = activeId,
                      comments = comments,
                      usernames = usernames,
                      onDismiss = { profileSamplesViewModel.resetCommentSampleId() },
                      onAddComment = { id, text -> profileSamplesViewModel.onAddComment(id, text) })
                }
              }
              downloadPickerSample?.let { s ->
                DownloadChoiceDialog(
                    sampleName = s.name,
                    processedAvailable = s.storageProcessedSamplePath.isNotBlank(),
                    onDismiss = { downloadPickerSample = null },
                    onDownloadZip = {
                      downloadPickerSample = null
                      profileSamplesViewModel.onDownloadZippedSample(s)
                    },
                    onDownloadProcessed = {
                      downloadPickerSample = null
                      profileSamplesViewModel.onDownloadProcessedSample(s)
                    },
                )
              }

              if (downloadProgress != null && downloadProgress != 0) {
                DownloadProgressBar(
                    downloadProgress = downloadProgress!!, testTag = DOWNLOAD_PROGRESS_BAR)
              }
              // if mode is self profile, show edit button
              viewConfig.bottomScreenButton?.invoke(Modifier.align(Alignment.BottomCenter))
            }
      }
}

/** Colors for all OutlinedTextField components. */
@OptIn(ExperimentalMaterial3Api::class)
val TextFieldColors: @Composable () -> TextFieldColors = {
  OutlinedTextFieldDefaults.colors(
      focusedTextColor = NepTuneTheme.colors.onBackground,
      unfocusedTextColor = NepTuneTheme.colors.onBackground,
      disabledTextColor = NepTuneTheme.colors.onBackground,
      cursorColor = NepTuneTheme.colors.onBackground,
      focusedBorderColor = NepTuneTheme.colors.onBackground,
      unfocusedBorderColor = NepTuneTheme.colors.onBackground,
      disabledBorderColor = NepTuneTheme.colors.onBackground,
      focusedLabelColor = NepTuneTheme.colors.onBackground,
      unfocusedLabelColor = NepTuneTheme.colors.onBackground,
  )
}

/**
 * Displays the editable profile form.
 *
 * Provides text fields for editing the user's name, username, and bio. Includes validation messages
 * and a Save button.
 *
 * @param uiState The current [SelfProfileUiState].
 * @param onSave Called when the user presses the Save button.
 * @param onNameChange Called on text change in the name field.
 * @param onUsernameChange Called on text change in the username field.
 * @param onBioChange Called on text change in the bio field.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditContent(
    uiState: SelfProfileUiState,
    localAvatarUri: Uri?,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onTagInputFieldChange: (String) -> Unit,
    onTagSubmit: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAvatarEditClick: () -> Unit,
    isOnline: Boolean = true
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .testTag(ProfileScreenTestTags.EDIT_CONTENT),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Avatar image
        val avatarModel = localAvatarUri ?: uiState.avatarUrl ?: R.drawable.ic_avatar_placeholder
        Avatar(
            model = avatarModel,
            modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
            showEditPencil = true,
            onEditClick = onAvatarEditClick)
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Field for name input
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
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Field for username input
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
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Field for bio input
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
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Tag input and addition
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

        // Tags display
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TagsSpacing),
            verticalArrangement = Arrangement.spacedBy(TagsSpacing),
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
        Spacer(modifier = Modifier.height(SectionVerticalSpacing))

        // Save button
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving && uiState.isValid && isOnline,
            modifier = Modifier.testTag(ProfileScreenTestTags.SAVE_BUTTON)) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
              Spacer(Modifier.width(ButtonIconSpacing))
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
    Spacer(Modifier.height(StatBlockLabelSpacing))
    Text(
        text = "$value",
        color = NepTuneTheme.colors.onBackground,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(testTag))
  }
}

@Composable
private fun StatRow(
    state: SelfProfileUiState,
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null
) {
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
        value = state.subscribers,
        modifier =
            Modifier.weight(1f).let { base ->
              if (!state.isAnonymousUser && onFollowersClick != null) {
                base.clickable { onFollowersClick() }
              } else {
                base
              }
            },
        testTag = ProfileScreenTestTags.FOLLOWERS_BLOCK)
    StatBlock(
        label = "Following",
        value = state.subscriptions,
        modifier =
            Modifier.weight(1f).let { base ->
              if (!state.isAnonymousUser && onFollowingClick != null) {
                base.clickable { onFollowingClick() }
              } else {
                base
              }
            },
        testTag = ProfileScreenTestTags.FOLLOWING_BLOCK)
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
    modifier: Modifier = Modifier,
    model: Any,
    sizeDp: Int = 120,
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
 * Connects the [SelfProfileViewModel] to the [ProfileScreen] and handles state collection. This
 * function is typically used as the entry point for navigation to the profile screen.
 *
 * This method was made using AI assistance
 */
@Composable
fun SelfProfileRoute(
    settings: () -> Unit = {},
    goBack: () -> Unit = {},
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
  val auth = FirebaseAuth.getInstance()
  val ownerId = auth.currentUser?.uid.orEmpty()

  val factory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(SelfProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelfProfileViewModel(repo = ProfileRepositoryProvider.repository) as T
          }
          throw IllegalArgumentException(UNKOWN_CLASS)
        }
      }

  val samplesFactory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(ProfileSamplesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileSamplesViewModel(
                ownerId = ownerId,
                auth = auth,
            )
                as T
          }
          throw IllegalArgumentException(UNKOWN_CLASS)
        }
      }

  val viewModel: SelfProfileViewModel = viewModel(factory = factory)
  val samplesViewModel: ProfileSamplesViewModel = viewModel(factory = samplesFactory)
  val state = viewModel.uiState.collectAsState().value
  val localAvatarUri by viewModel.localAvatarUri.collectAsState()
  val tempAvatarUri by viewModel.tempAvatarUri.collectAsState()
  val isOnline by remember { NetworkConnectivityObserver().isOnline }.collectAsState(initial = true)
  val isUserLoggedIn = viewModel.isUserLoggedIn

  LaunchedEffect(Unit) { viewModel.loadOrEnsure() }

  // Prepare the image picker launcher. The callback will notify the ViewModel.
  val imagePickerLauncher =
      rememberImagePickerLauncher(
          onImageCropped = { croppedUri: Uri? ->
            if (croppedUri != null) {
              viewModel.onAvatarCropped(croppedUri)
            }
          })

  val viewConfig =
      ProfileViewConfig.SelfProfileConfig(
          onEdit = viewModel::onEditClick,
          settings = settings,
          canEditProfile = !state.isAnonymousUser,
          onFollowingClick = {
            if (!state.isAnonymousUser) {
              onFollowingClick()
            }
          },
          onFollowersClick = {
            if (!state.isAnonymousUser) {
              onFollowersClick()
            }
          },
          isOnline = isOnline)

  ProfileScreen(
      uiState = state,
      localAvatarUri =
          if (tempAvatarUri != null) {
            tempAvatarUri
          } else {
            localAvatarUri
          },
      onAvatarEditClick = { imagePickerLauncher.launch("image/*") }, // Launch the picker
      viewConfig = viewConfig,
      profileSamplesViewModel = samplesViewModel,
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
              goBackClick = goBack),
      isOnline = isOnline,
      isUserLoggedIn = isUserLoggedIn)
}

@Composable
fun OtherUserProfileRoute(
    userId: String,
    goBack: () -> Unit = {},
) {
  val auth = FirebaseAuth.getInstance()

  val factory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(OtherProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtherProfileViewModel(
                repo = ProfileRepositoryProvider.repository, userId = userId)
                as T
          }
          throw IllegalArgumentException(UNKOWN_CLASS)
        }
      }

  val samplesFactory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(ProfileSamplesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileSamplesViewModel(ownerId = userId, auth = auth) as T
          }
          throw IllegalArgumentException("Unknown ViewModel class")
        }
      }

  val otherProfileViewModel: OtherProfileViewModel = viewModel(factory = factory)
  val samplesViewModel: ProfileSamplesViewModel = viewModel(factory = samplesFactory)
  val state by otherProfileViewModel.uiState.collectAsState()
  val isOnline by remember { NetworkConnectivityObserver().isOnline }.collectAsState(initial = true)

  val viewConfig =
      ProfileViewConfig.OtherProfileConfig(
          isFollowing = state.isCurrentUserFollowing,
          isFollowActionInProgress = state.isFollowActionInProgress,
          canFollowTarget = !state.profile.isAnonymousUser && !state.isCurrentUserAnonymous,
          onFollow = otherProfileViewModel::onFollow,
          errorMessage = state.errorMessage,
          isOnline = isOnline)

  ProfileScreen(
      uiState = state.profile,
      localAvatarUri = null, // No local avatar for others (only remote URL)
      viewConfig = viewConfig,
      profileSamplesViewModel = samplesViewModel,
      callbacks = profileScreenCallbacks(goBackClick = goBack),
      isOnline = isOnline)
}
