package com.neptune.neptune.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.neptune.neptune.ui.feed.FeedCallbacks
import com.neptune.neptune.ui.feed.FeedItemStyle
import com.neptune.neptune.ui.feed.sampleFeedItems
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.CommentDialogAction
import com.neptune.neptune.ui.main.DownloadChoiceDialog
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.offline.OfflineBanner
import com.neptune.neptune.ui.profile.ProfileScreenTestTags.DOWNLOAD_PROGRESS_BAR
import com.neptune.neptune.ui.theme.NepTuneTheme

/**
 * Centralized constants defining all `testTag` identifiers used in Profile UI tests.
 *
 * These tags are applied to key composable elements (e.g., buttons, fields, avatar) to make them
 * accessible and distinguishable within instrumented Compose UI tests.
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
 * Shared configuration between profile view modes.
 *
 * Implementations supply the top bar content and stat click handlers relevant to either self or
 * other-user profiles.
 */
sealed interface ProfileViewConfig {
  val topBarContent: (@Composable () -> Unit)?
  val onFollowingClick: (() -> Unit)?
  val onFollowersClick: (() -> Unit)?

  /**
   * Configuration for the current user's own profile.
   *
   * Controls visibility of edit/settings buttons and the ability to navigate to follower lists.
   */
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

  /** Configuration when viewing another user's profile, including follow action state. */
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
