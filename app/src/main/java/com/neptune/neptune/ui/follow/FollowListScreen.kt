package com.neptune.neptune.ui.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neptune.neptune.R
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.theme.NepTuneTheme

object FollowListScreenTestTags {
  const val ROOT = "follow_list/root"
  const val TOP_BAR = "follow_list/top_bar"
  const val TITLE = "follow_list/title"
  const val TAB_ROW = "follow_list/tab_row"
  const val TAB_FOLLOWERS = "follow_list/tab/followers"
  const val TAB_FOLLOWING = "follow_list/tab/following"
  const val LIST = "follow_list/list"
  const val LIST_LOADING = "follow_list/list/loading"
  const val LIST_LOADING_INDICATOR = "follow_list/list/loading/indicator"
  const val LIST_EMPTY = "follow_list/list/empty"
  const val LIST_EMPTY_REFRESH = "follow_list/list/empty/refresh"
  const val ITEM = "follow_list/item"
  const val AVATAR = "follow_list/item/avatar"
  const val FOLLOW_BUTTON = "follow_list/item/follow_button"
  const val BACK = "follow_list/back"
}

private val TOP_BAR_PADDING = 16.dp
private val LIST_ITEMS_PADDING = 16.dp
private val LIST_ITEMS_VERTICAL_SPACING = 8.dp
private val LIST_ITEMS_HORIZONTAL_SPACING = 14.dp
private val AVATAR_SIZE = 44.dp
private val CIRCULAR_PROGRESS_SIZE = 16.dp

private fun appTextStyle(fontSize: TextUnit = 18.sp) =
    TextStyle(
        fontSize = fontSize,
        fontWeight = FontWeight(400),
        fontFamily = FontFamily(Font(R.font.markazi_text)))

@Composable
fun FollowListRoute(
    initialTab: FollowListTab,
    goBack: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
) {
  val factory =
      object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(FollowListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FollowListViewModel(
                repo = ProfileRepositoryProvider.repository, initialTab = initialTab)
                as T
          }
          throw IllegalArgumentException("Unknown ViewModel class")
        }
      }

  val viewModel: FollowListViewModel = viewModel(factory = factory)
  val uiState by viewModel.uiState.collectAsState()

  FollowListScreen(
      state = uiState,
      onBack = goBack,
      onTabSelected = viewModel::selectTab,
      onRefresh = viewModel::refresh,
      onToggleFollow = viewModel::toggleFollow,
      navigateToOtherUserProfile = navigateToOtherUserProfile)
}

@Composable
fun FollowListScreen(
    state: FollowListUiState,
    onBack: () -> Unit,
    onTabSelected: (FollowListTab) -> Unit,
    onRefresh: () -> Unit,
    onToggleFollow: (uid: String, isFromFollowersList: Boolean) -> Unit,
    navigateToOtherUserProfile: (String) -> Unit,
) {
  val list =
      when (state.activeTab) {
        FollowListTab.FOLLOWERS -> state.followers
        FollowListTab.FOLLOWING -> state.following
      }

  val isLoading =
      when (state.activeTab) {
        FollowListTab.FOLLOWERS -> state.isLoadingFollowers
        FollowListTab.FOLLOWING -> state.isLoadingFollowing
      }

  Scaffold(
      topBar = {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(TOP_BAR_PADDING)
                    .testTag(FollowListScreenTestTags.TOP_BAR),
            verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                  onClick = onBack, modifier = Modifier.testTag(FollowListScreenTestTags.BACK)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = NepTuneTheme.colors.onBackground)
                  }
              Text(
                  text =
                      when (state.activeTab) {
                        FollowListTab.FOLLOWERS -> "Followers"
                        FollowListTab.FOLLOWING -> "Following"
                      },
                  modifier = Modifier.testTag(FollowListScreenTestTags.TITLE),
                  style = appTextStyle(40.sp),
                  color = NepTuneTheme.colors.onBackground,
              )
            }
      },
      containerColor = NepTuneTheme.colors.background,
      modifier = Modifier.testTag(FollowListScreenTestTags.ROOT)) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          TabRow(
              modifier = Modifier.testTag(FollowListScreenTestTags.TAB_ROW),
              selectedTabIndex = if (state.activeTab == FollowListTab.FOLLOWERS) 0 else 1,
              containerColor = Color.Transparent,
              contentColor = NepTuneTheme.colors.onBackground) {
                Tab(
                    selected = state.activeTab == FollowListTab.FOLLOWERS,
                    onClick = { onTabSelected(FollowListTab.FOLLOWERS) },
                    modifier = Modifier.testTag(FollowListScreenTestTags.TAB_FOLLOWERS),
                    text = { Text("Followers", style = appTextStyle()) })
                Tab(
                    selected = state.activeTab == FollowListTab.FOLLOWING,
                    onClick = { onTabSelected(FollowListTab.FOLLOWING) },
                    modifier = Modifier.testTag(FollowListScreenTestTags.TAB_FOLLOWING),
                    text = { Text("Following", style = appTextStyle()) })
              }

          if (isLoading) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .testTag(FollowListScreenTestTags.LIST_LOADING),
                horizontalArrangement = Arrangement.Center) {
                  CircularProgressIndicator(
                      modifier = Modifier.testTag(FollowListScreenTestTags.LIST_LOADING_INDICATOR))
                }
          } else if (list.isEmpty()) {
            EmptyState(onRefresh = onRefresh)
          } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(FollowListScreenTestTags.LIST),
                verticalArrangement = Arrangement.spacedBy(LIST_ITEMS_VERTICAL_SPACING),
                contentPadding = PaddingValues(LIST_ITEMS_PADDING)) {
                  items(list, key = { it.uid }) { user ->
                    FollowListRow(
                        user = user,
                        isFollowersTab = state.activeTab == FollowListTab.FOLLOWERS,
                        isAnonymous = state.isCurrentUserAnonymous,
                        onToggleFollow = {
                          onToggleFollow(user.uid, state.activeTab == FollowListTab.FOLLOWERS)
                        },
                        onUserClick = { navigateToOtherUserProfile(user.uid) })
                  }
                }
          }
        }
      }
}

@Composable
private fun FollowListRow(
    user: FollowListUserItem,
    isFollowersTab: Boolean,
    isAnonymous: Boolean,
    onToggleFollow: () -> Unit,
    onUserClick: () -> Unit
) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .background(NepTuneTheme.colors.background)
              .padding(LIST_ITEMS_VERTICAL_SPACING)
              .testTag("${FollowListScreenTestTags.ITEM}/${user.uid}"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(LIST_ITEMS_HORIZONTAL_SPACING)) {
        Avatar(
            avatarUrl = user.avatarUrl,
            username = user.username,
            onClick = onUserClick,
            modifier = Modifier.testTag("${FollowListScreenTestTags.AVATAR}/${user.uid}"))
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = user.username,
              style = appTextStyle(),
              color = NepTuneTheme.colors.onBackground,
              modifier = Modifier.clickable(onClick = onUserClick))
        }
        val buttonLabel =
            if (user.isFollowedByCurrentUser) {
              "Unfollow"
            } else if (isFollowersTab) {
              "Follow back"
            } else {
              "Follow"
            }
        Button(
            onClick = onToggleFollow,
            enabled = !isAnonymous && !user.isActionInProgress,
            modifier = Modifier.testTag(FollowListScreenTestTags.FOLLOW_BUTTON)) {
              if (user.isActionInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(CIRCULAR_PROGRESS_SIZE),
                    color = NepTuneTheme.colors.onBackground)
              } else {
                Text(buttonLabel, style = appTextStyle())
              }
            }
      }
}

@Composable
private fun Avatar(
    avatarUrl: String?,
    username: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val placeholderPainter = rememberVectorPainter(Icons.Default.Person)
  val avatarModifier =
      modifier
          .size(AVATAR_SIZE)
          .clip(CircleShape)
          .background(NepTuneTheme.colors.onBackground)
          .clickable(onClick = onClick)

  val model = avatarUrl?.takeUnless { it.isEmpty() } ?: R.drawable.profile
  AsyncImage(
      model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).build(),
      contentDescription = "Avatar for $username",
      modifier = avatarModifier,
      placeholder = placeholderPainter,
      error = placeholderPainter)
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().testTag(FollowListScreenTestTags.LIST_EMPTY),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing here yet", color = NepTuneTheme.colors.onBackground, style = appTextStyle())
        Spacer(modifier = Modifier.height(LIST_ITEMS_VERTICAL_SPACING))
        Button(
            onClick = onRefresh,
            modifier = Modifier.testTag(FollowListScreenTestTags.LIST_EMPTY_REFRESH)) {
              Text("Refresh", style = appTextStyle())
            }
      }
}
