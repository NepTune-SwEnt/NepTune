package com.neptune.neptune.ui.profile

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.data.rememberImagePickerLauncher
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.util.NetworkConnectivityObserver

private const val UNKOWN_CLASS = "Unknown ViewModel class"

/**
 * Composable route for the current user's profile.
 *
 * Wires view models, connectivity, and image picker callbacks into [ProfileScreen].
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
      onAvatarEditClick = { imagePickerLauncher.launch("image/*") },
      viewConfig = viewConfig,
      profileSamplesViewModel = samplesViewModel,
      callbacks =
          profileScreenCallbacks(
              onEditClick = viewModel::onEditClick,
              onSaveClick = { _, _, _ -> viewModel.onSaveClick() },
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

/**
 * Composable route for viewing another user's profile.
 *
 * Instantiates [OtherProfileViewModel] and passes observed state into [ProfileScreen].
 */
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
      localAvatarUri = null,
      viewConfig = viewConfig,
      profileSamplesViewModel = samplesViewModel,
      callbacks = profileScreenCallbacks(goBackClick = goBack),
      isOnline = isOnline)
}
