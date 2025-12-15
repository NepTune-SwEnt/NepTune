package com.neptune.neptune.ui.profile

import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class ProfileDimensions(
    val screenPadding: Dp,
    val settingsButtonSize: Dp,
    val avatarVerticalSpacing: Dp,
    val textFieldSpacing: Dp,
    val sectionVerticalSpacing: Dp,
    val topScreenPadding: Dp,
    val bioVerticalSpacing: Dp,
    val preSamplesSectionSpacing: Dp,
    val statsSpacing: Dp,
    val bottomButtonBottomPadding: Dp,
    val buttonIconSpacing: Dp,
    val topBarActionsSpacing: Dp,
    val tagsSpacing: Dp,
    val statBlockLabelSpacing: Dp,
    val inlineSpacing: Dp,
    val listBottomPadding: Dp,
)

private val LocalProfileDimensions = staticCompositionLocalOf {
  defaultProfileDimensions(scale = 1f)
}

private fun defaultProfileDimensions(scale: Float) =
    ProfileDimensions(
        screenPadding = (16 * scale).dp,
        settingsButtonSize = (30 * scale).dp,
        avatarVerticalSpacing = (15 * scale).dp,
        textFieldSpacing = (15 * scale).dp,
        sectionVerticalSpacing = (50 * scale).dp,
        topScreenPadding = (40 * scale).dp,
        bioVerticalSpacing = (30 * scale).dp,
        preSamplesSectionSpacing = (60 * scale).dp,
        statsSpacing = (20 * scale).dp,
        bottomButtonBottomPadding = (24 * scale).dp,
        buttonIconSpacing = (8 * scale).dp,
        topBarActionsSpacing = (12 * scale).dp,
        tagsSpacing = (8 * scale).dp,
        statBlockLabelSpacing = (4 * scale).dp,
        inlineSpacing = (12 * scale).dp,
        listBottomPadding = (88 * scale).dp,
    )

@Composable
private fun rememberProfileDimensions(): ProfileDimensions {
  val configuration = LocalConfiguration.current
  val baseWidth = 375f
  val screenWidth = configuration.screenWidthDp.takeIf { it > 0 }?.toFloat() ?: baseWidth
  val scale = (screenWidth / baseWidth).coerceIn(0.85f, 1.25f)
  return defaultProfileDimensions(scale)
}

private const val UNKOWN_CLASS = "Unknown ViewModel class"

private fun appTextStyle(fontSize: TextUnit = 18.sp) =
    TextStyle(
        fontSize = fontSize,
        fontWeight = FontWeight(400),
        fontFamily = FontFamily(Font(R.font.markazi_text)))

private val EDIT_FIELDS_FONT_SIZE = 20.sp
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
  val dimensions = rememberProfileDimensions()
  CompositionLocalProvider(LocalProfileDimensions provides dimensions) {
    Column(
        modifier = Modifier.padding(dimensions.screenPadding).testTag(ProfileScreenTestTags.ROOT)) {
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
}

@Composable
private fun SettingsButton(settings: () -> Unit) {
  val dimensions = LocalProfileDimensions.current
  IconButton(
      modifier =
          Modifier.size(dimensions.settingsButtonSize)
              .testTag(ProfileScreenTestTags.SETTINGS_BUTTON),
      onClick = settings) {
        Icon(
            modifier = Modifier.size(dimensions.settingsButtonSize),
            imageVector = Icons.Default.Settings,
            contentDescription = "Logout",
            tint = NepTuneTheme.colors.onBackground)
      }
}

sealed interface ProfileViewConfig {
  val topBarContent: (@Composable () -> Unit)?
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
    override val topBarContent =
        @Composable {
          val dimensions = LocalProfileDimensions.current
          Row(
              horizontalArrangement = Arrangement.spacedBy(dimensions.topBarActionsSpacing),
              verticalAlignment = Alignment.CenterVertically) {
                if (canEditProfile) {
                  IconButton(
                      onClick = onEdit,
                      enabled = isOnline,
                      modifier =
                          Modifier.size(dimensions.settingsButtonSize)
                              .testTag(ProfileScreenTestTags.EDIT_BUTTON)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = NepTuneTheme.colors.onBackground)
                      }
                }
                SettingsButton(settings)
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
    override val topBarContent =
        @Composable
        Composable@{
          val dimensions = LocalProfileDimensions.current
          if (!canFollowTarget) return@Composable
          val label = if (isFollowing) "Unfollow" else "Follow"
          val icon = if (isFollowing) Icons.Default.Clear else Icons.Default.Add

          Column(horizontalAlignment = Alignment.End) {
            Button(
                onClick = onFollow,
                enabled = !isFollowActionInProgress && isOnline,
                border = BorderStroke(2.dp, color = NepTuneTheme.colors.onBackground),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = NepTuneTheme.colors.background,
                        contentColor = NepTuneTheme.colors.onPrimary),
                modifier = Modifier.testTag(ProfileScreenTestTags.FOLLOW_BUTTON)) {
                  Icon(
                      imageVector = icon,
                      contentDescription = "Follow",
                      tint = NepTuneTheme.colors.onBackground)
                  Spacer(Modifier.width(dimensions.buttonIconSpacing))
                  Text(
                      text = label,
                      style = appTextStyle(),
                      color = NepTuneTheme.colors.onBackground)
                }
            if (!errorMessage.isNullOrBlank()) {
              Text(
                  text = errorMessage,
                  color = Color.Red,
                  style = appTextStyle(),
                  textAlign = TextAlign.End,
                  modifier = Modifier.testTag("profile/follow_error"))
            }
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
  val dimensions = LocalProfileDimensions.current
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val cardWidth = screenWidth - (dimensions.screenPadding * 2f)
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
                    modifier = Modifier.fillMaxSize().then(samplesListTagModifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  item { Spacer(Modifier.height(dimensions.topScreenPadding)) }

                  // Avatar image
                  item {
                    val avatarModel =
                        localAvatarUri ?: state.avatarUrl ?: R.drawable.ic_avatar_placeholder
                    Avatar(
                        model = avatarModel,
                        modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
                        showEditPencil = false)
                  }

                  item { Spacer(Modifier.height(dimensions.avatarVerticalSpacing)) }

                  // Name and username
                  item {
                    Text(
                        text = state.name,
                        color = NepTuneTheme.colors.onBackground,
                        style = appTextStyle(40.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(ProfileScreenTestTags.NAME))
                  }
                  item {
                    Text(
                        text = "@${state.username}",
                        color = NepTuneTheme.colors.onBackground,
                        style = appTextStyle(20.sp),
                        modifier = Modifier.testTag(ProfileScreenTestTags.USERNAME))
                  }
                  item { Spacer(Modifier.height(dimensions.statsSpacing)) }

                  // Stats row
                  item {
                    Box(
                        modifier =
                            Modifier.border(
                                width = 0.5.dp,
                                color = NepTuneTheme.colors.accentPrimary,
                                shape = RoundedCornerShape(16.dp))) {
                          StatRow(
                              state = state,
                              onFollowersClick = viewConfig.onFollowersClick,
                              onFollowingClick = viewConfig.onFollowingClick)
                        }
                  }
                  item { Spacer(Modifier.height(dimensions.bioVerticalSpacing)) }

                  // Bio
                  item {
                    Text(
                        text = if (state.bio != "") "“${state.bio}”" else "",
                        color = NepTuneTheme.colors.onBackground,
                        style = appTextStyle(30.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(ProfileScreenTestTags.BIO))
                  }

                  // Tags
                  if (state.tags.isNotEmpty()) {
                    item {
                      FlowRow(
                          horizontalArrangement = Arrangement.spacedBy(dimensions.tagsSpacing),
                          verticalArrangement = Arrangement.spacedBy(dimensions.tagsSpacing),
                          modifier = Modifier.testTag(ProfileScreenTestTags.TAGS_VIEW_SECTION)) {
                            state.tags.forEach { tag ->
                              InputChip(
                                  selected = false,
                                  onClick = {},
                                  enabled = false,
                                  label = { Text(text = tag, style = appTextStyle()) },
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

                  item { Spacer(Modifier.height(dimensions.bioVerticalSpacing)) }

                  if (samples.isEmpty()) {
                    item {
                      Text(
                          text = "No samples posted yet.",
                          style = appTextStyle(),
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
                        height = cardHeight,
                        showOwnerInfo = false)
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
  val dimensions = LocalProfileDimensions.current
  Scaffold(
      topBar = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically) {
              Button(
                  onClick = onSave,
                  enabled = !uiState.isSaving && uiState.isValid && isOnline,
                  border = BorderStroke(2.dp, color = NepTuneTheme.colors.onBackground),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = NepTuneTheme.colors.background,
                          contentColor = NepTuneTheme.colors.onPrimary),
                  modifier = Modifier.testTag(ProfileScreenTestTags.SAVE_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = NepTuneTheme.colors.onBackground)
                  }
            }
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .testTag(ProfileScreenTestTags.EDIT_CONTENT),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top) {
              Spacer(modifier = Modifier.height(dimensions.topScreenPadding))

              // Avatar image
              val avatarModel =
                  localAvatarUri ?: uiState.avatarUrl ?: R.drawable.ic_avatar_placeholder
              Avatar(
                  model = avatarModel,
                  modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR),
                  showEditPencil = true,
                  onEditClick = onAvatarEditClick)
              Spacer(modifier = Modifier.height(dimensions.sectionVerticalSpacing))

              // Field for name input
              OutlinedTextField(
                  value = uiState.name,
                  textStyle = appTextStyle(EDIT_FIELDS_FONT_SIZE),
                  onValueChange = onNameChange,
                  label = { Text(text = "Name", style = appTextStyle()) },
                  colors = TextFieldColors(),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_NAME),
                  isError = uiState.nameError != null,
                  supportingText = {
                    val err = uiState.nameError
                    if (err != null) {
                      Text(
                          text = err,
                          color = MaterialTheme.colorScheme.error,
                          style = appTextStyle())
                    } else {
                      Text(
                          text = "${uiState.name.trim().length}/30",
                          color = NepTuneTheme.colors.onBackground,
                          style = appTextStyle())
                    }
                  })
              Spacer(modifier = Modifier.height(dimensions.textFieldSpacing))

              // Field for username input
              OutlinedTextField(
                  value = uiState.username,
                  textStyle = appTextStyle(EDIT_FIELDS_FONT_SIZE),
                  onValueChange = onUsernameChange,
                  label = { Text(text = "Username", style = appTextStyle()) },
                  colors = TextFieldColors(),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_USERNAME),
                  isError = uiState.usernameError != null,
                  supportingText = {
                    val err = uiState.usernameError
                    if (err != null) {
                      Text(
                          text = err,
                          color = MaterialTheme.colorScheme.error,
                          style = appTextStyle())
                    } else {
                      Text(
                          color = NepTuneTheme.colors.onBackground,
                          text = "${uiState.username.trim().length}/15",
                          style = appTextStyle())
                    }
                  })
              Spacer(modifier = Modifier.height(dimensions.textFieldSpacing))

              // Field for bio input
              OutlinedTextField(
                  value = uiState.bio,
                  textStyle = appTextStyle(EDIT_FIELDS_FONT_SIZE),
                  onValueChange = onBioChange,
                  label = { Text(text = "Bio", style = appTextStyle()) },
                  colors = TextFieldColors(),
                  modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIELD_BIO),
                  singleLine = true,
                  isError = uiState.bioError != null,
                  supportingText = {
                    val err = uiState.bioError
                    if (err != null) {
                      Text(
                          text = err,
                          color = MaterialTheme.colorScheme.error,
                          style = appTextStyle())
                    } else {
                      Text(
                          text = "${uiState.bio.length}/160",
                          color = NepTuneTheme.colors.onBackground,
                          style = appTextStyle())
                    }
                  })
              Spacer(modifier = Modifier.height(dimensions.textFieldSpacing))

              // Tag input and addition
              Row(
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                    value = uiState.inputTag,
                    textStyle = appTextStyle(EDIT_FIELDS_FONT_SIZE),
                    onValueChange = onTagInputFieldChange,
                    label = {
                      Text(text = "My music genre", style = appTextStyle(EDIT_FIELDS_FONT_SIZE))
                    },
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
                          style = appTextStyle())
                    })
                Spacer(Modifier.width(dimensions.inlineSpacing))
                Button(
                    onClick = onTagSubmit,
                    border = BorderStroke(2.dp, color = NepTuneTheme.colors.onBackground),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = NepTuneTheme.colors.background,
                            contentColor = NepTuneTheme.colors.onPrimary),
                    modifier =
                        Modifier.fillMaxHeight().testTag(ProfileScreenTestTags.ADD_TAG_BUTTON)) {
                      Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = "Add",
                          tint = NepTuneTheme.colors.onBackground)
                    }
              }
              Spacer(Modifier.height(dimensions.inlineSpacing))

              // Tags display
              FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(dimensions.tagsSpacing),
                  verticalArrangement = Arrangement.spacedBy(dimensions.tagsSpacing),
                  modifier = Modifier.testTag(ProfileScreenTestTags.TAGS_EDIT_SECTION)) {
                    uiState.tags.forEach { tag ->
                      key(tag) { // <— ensures slot stability and proper disposal on removal
                        EditableTagChip(
                            tagText = tag,
                            onRemove = onRemoveTag,
                            modifier =
                                Modifier.testTag("profile/tag/chip/${tag.replace(' ', '_')}"))
                      }
                    }
                  }
              Spacer(modifier = Modifier.height(dimensions.sectionVerticalSpacing))
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
  val dimensions = LocalProfileDimensions.current
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = label,
        color = NepTuneTheme.colors.onBackground,
        style = appTextStyle(),
        textAlign = TextAlign.Center)
    Spacer(Modifier.height(dimensions.statBlockLabelSpacing))
    Text(
        text = "$value",
        color = NepTuneTheme.colors.onBackground,
        style = appTextStyle(20.sp),
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
      label = { Text(text = tagText, style = appTextStyle()) },
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
