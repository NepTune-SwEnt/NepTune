package com.neptune.neptune.ui.search

import OfflineScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.feed.sampleFeedItems
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.offline.OfflineBanner
import com.neptune.neptune.ui.projectlist.SearchBar
import com.neptune.neptune.ui.theme.NepTuneTheme
import kotlinx.coroutines.delay

/*
Search Screen Composable
Includes: The search bar and the list of samples matching the search query
Uses: SearchViewModel to get the list of samples
Clicking on the profile picture of a sample navigates to the profile screen of the poster
Clicking on like puts it in red
written with assistance from ChatGPT
 */
object SearchScreenTestTags {
  const val SEARCH_SCREEN = "searchScreen"
  const val SEARCH_BAR = "searchBar"
  const val SAMPLE_LIST = "sampleList"
  const val USER_LIST = "userList"
  const val DOWNLOAD_BAR = "downloadBar"
}

class SearchScreenTestTagsPerSampleCard(private val idInColumn: String = "0") : BaseSampleTestTags {
  override val prefix = "SearchScreen"

  override fun tag(name: String) = "${prefix}/${name}_$idInColumn"

  // All tags use computed getters (evaluated to a String)
  override val SAMPLE_CARD
    get() = tag("sampleCard")

  override val SAMPLE_PROFILE_ICON
    get() = tag("sampleProfileIcon")

  override val SAMPLE_USERNAME
    get() = tag("sampleUsername")

  override val SAMPLE_NAME
    get() = tag("sampleName")

  override val SAMPLE_DURATION
    get() = tag("sampleDuration")

  override val SAMPLE_TAGS
    get() = tag("sampleTags")

  override val SAMPLE_LIKES
    get() = tag("sampleLikes")

  override val SAMPLE_COMMENTS
    get() = tag("sampleComments")

  override val SAMPLE_DOWNLOADS
    get() = tag("sampleDownloads")
}

class SearchScreenTestTagsPerUserCard(uid: String) {
  val CARD = "userCard_$uid"
  val USERNAME = "userUsername_$uid"
  val FOLLOW_BUTTON = "userFollowButton_$uid"
}

/**
 * Composable function representing the Search Screen.
 *
 * @param searchViewModel The ViewModel managing the search logic and state.
 * @param mediaPlayer The media player instance for sample playback.
 * @param navigateToProfile Lambda function to navigate to the current user's profile.
 * @param navigateToOtherUserProfile Lambda function to navigate to another user's profile by ID.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
) {
  val samples by searchViewModel.samples.collectAsState()
  var searchText by remember { mutableStateOf("") }
  val downloadProgress: Int? by searchViewModel.downloadProgress.collectAsState()
  val sampleResources by searchViewModel.sampleResources.collectAsState()

  // Collect the search type and user results
  val searchType by searchViewModel.searchType.collectAsState()
  val userResults by searchViewModel.userResults.collectAsState()

  // Observe the synchronized following IDs
  val followingIds by searchViewModel.followingIds.collectAsState()
  val currentUserProfile by searchViewModel.currentUserProfile.collectAsState()

  LaunchedEffect(searchText) {
    delay(400L) // debounce time
    searchViewModel.search(searchText)
  }
  val whatToSearchFor = searchType.title
  val likedSamples by searchViewModel.likedSamples.collectAsState()
  val activeCommentSampleId by searchViewModel.activeCommentSampleId.collectAsState()
  val comments by searchViewModel.comments.collectAsState()
  val isOnline by searchViewModel.isOnline.collectAsState()
  val isUserLoggedIn = remember { searchViewModel.isUserLoggedIn }
  Box(modifier = Modifier.fillMaxSize()) {
    if (!isUserLoggedIn) {
      OfflineScreen()
    } else {
      Scaffold(
          containerColor = NepTuneTheme.colors.background,
          modifier = Modifier.testTag(SearchScreenTestTags.SEARCH_SCREEN),
          topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                  // SearchBar (Takes all remaining space)
                  Box(modifier = Modifier.weight(1f)) {
                    SearchBar(
                        searchText,
                        { searchText = it },
                        SearchScreenTestTags.SEARCH_BAR,
                        whatToSearchFor)
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  val roundShapePercentage = 50
                  OutlinedButton(
                      onClick = { searchViewModel.toggleSearchType() },
                      border = BorderStroke(1.dp, NepTuneTheme.colors.onBackground),
                      shape = RoundedCornerShape(roundShapePercentage),
                      colors =
                          ButtonDefaults.outlinedButtonColors(
                              contentColor = NepTuneTheme.colors.onBackground),
                      modifier = Modifier.height(36.dp).width(100.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(
                            text = "See ${searchType.toggle().title}",
                            style =
                                TextStyle(
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            maxLines = 1)
                      }
                }
          },
          content = { pd ->
            Column(modifier = Modifier.fillMaxSize().padding(pd)) {
              if (!isOnline) {
                OfflineBanner()
              }
              if (searchType == SearchType.SAMPLES) {
                ScrollableColumnOfSamples(
                    samples = samples,
                    searchViewModel = searchViewModel,
                    mediaPlayer = mediaPlayer,
                    likedSamples = likedSamples,
                    activeCommentSampleId = activeCommentSampleId,
                    comments = comments,
                    navigateToProfile = navigateToProfile,
                    navigateToOtherUserProfile = navigateToOtherUserProfile,
                    sampleResources = sampleResources)
              } else {
                ScrollableColumnOfUsers(
                    users = userResults,
                    followingIds = followingIds,
                    currentUserId = currentUserProfile?.uid ?: "",
                    onFollowToggle = { uid, isFollowing ->
                      searchViewModel.toggleFollow(uid, isFollowing)
                    },
                    navigateToOtherUserProfile = { uid ->
                      if (searchViewModel.isCurrentUser(uid)) {
                        navigateToProfile()
                      } else {
                        navigateToOtherUserProfile(uid)
                      }
                    },
                    modifier = Modifier.padding(pd))
              }
            }
          })
      if (downloadProgress != null && downloadProgress != 0) {
        DownloadProgressBar(
            downloadProgress = downloadProgress!!, SearchScreenTestTags.DOWNLOAD_BAR)
      }
    }
  }
}

@Composable
fun ScrollableColumnOfUsers(
    users: List<Profile>,
    followingIds: Set<String>,
    currentUserId: String,
    onFollowToggle: (String, Boolean) -> Unit,
    navigateToOtherUserProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyColumn(
      modifier =
          modifier
              .testTag(SearchScreenTestTags.USER_LIST)
              .fillMaxSize()
              .background(NepTuneTheme.colors.background),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(16.dp)) {
        items(users) { profile ->
          val isFollowing = followingIds.contains(profile.uid)
          val isMe = profile.uid == currentUserId
          val testTags = SearchScreenTestTagsPerUserCard(profile.uid)

          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .testTag(testTags.CARD)
                      .clickable { navigateToOtherUserProfile(profile.uid) }
                      .background(NepTuneTheme.colors.cardBackground, RoundedCornerShape(8.dp))
                      .border(1.dp, NepTuneTheme.colors.onBackground, RoundedCornerShape(8.dp))
                      .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically) {
                      if (profile.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_avatar_placeholder),
                            error = painterResource(id = R.drawable.ic_avatar_placeholder))
                      } else {
                        // Use Image instead of Icon to preserve the original colors of the
                        // placeholder
                        Image(
                            painter = painterResource(id = R.drawable.ic_avatar_placeholder),
                            contentDescription = "User Avatar",
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop)
                      }

                      Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = profile.username.ifBlank { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            color = NepTuneTheme.colors.onBackground,
                            modifier = Modifier.testTag(testTags.USERNAME))

                        val namePart = if (!profile.name.isNullOrBlank()) profile.name else ""
                        val separator = if (namePart.isNotBlank()) " • " else ""

                        Text(
                            text = "$namePart$separator${profile.subscribers} followers",
                            style = MaterialTheme.typography.bodySmall,
                            color = NepTuneTheme.colors.onBackground)
                      }
                    }

                if (!isMe) {
                  Button(
                      onClick = { onFollowToggle(profile.uid, isFollowing) },
                      colors =
                          ButtonDefaults.buttonColors(
                              containerColor =
                                  if (isFollowing) Color.Gray
                                  else MaterialTheme.colorScheme.primary),
                      modifier = Modifier.padding(start = 8.dp).testTag(testTags.FOLLOW_BUTTON)) {
                        Text(text = if (isFollowing) "Unfollow" else "Follow")
                      }
                }
              }
        }
      }
}

@Composable
fun ScrollableColumnOfSamples(
    samples: List<Sample>,
    searchViewModel: SearchViewModel,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    likedSamples: Map<String, Boolean> = emptyMap(),
    activeCommentSampleId: String? = null,
    comments: List<Comment> = emptyList(),
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
    sampleResources: Map<String, SampleResourceState> = emptyMap(),
    testTagsForSample: (Sample) -> BaseSampleTestTags = {
      SearchScreenTestTagsPerSampleCard(idInColumn = it.id)
    },
) {
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val width = screenWidth - 20.dp
  val height = width * (150f / 166f) // the same ratio than in the feedScreen
  // Ensure the possibility to like in local
  LazyColumn(
      modifier =
          Modifier.testTag(SearchScreenTestTags.SAMPLE_LIST)
              .fillMaxSize()
              .background(NepTuneTheme.colors.background),
      horizontalAlignment = Alignment.CenterHorizontally) {
        sampleFeedItems(
            samples = samples,
            controller = searchViewModel,
            mediaPlayer = mediaPlayer,
            likedSamples = likedSamples,
            sampleResources = sampleResources,
            navigateToProfile = navigateToProfile,
            navigateToOtherUserProfile = navigateToOtherUserProfile,
            testTagsForSample = testTagsForSample,
            width = width,
            height = height)
      } // Comment Overlay
  if (activeCommentSampleId != null) {
    val usernames by searchViewModel.usernames.collectAsState()
    CommentDialog(
        sampleId = activeCommentSampleId,
        comments = comments,
        usernames = usernames,
        onDismiss = { searchViewModel.resetCommentSampleId() },
        onAddComment = { id, text -> searchViewModel.onAddComment(id, text) },
    )
  }
}
