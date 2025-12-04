package com.neptune.neptune.ui.search

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.main.SampleItem
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.onClickFunctions
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

private fun factory(application: Application) =
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
          @Suppress("UNCHECKED_CAST")
          return SearchViewModel(context = application, auth = FirebaseAuth.getInstance()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
      }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel =
        viewModel(factory = factory(LocalContext.current.applicationContext as Application)),
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

  LaunchedEffect(searchText) {
    delay(400L) // debounce time
    searchViewModel.search(searchText)
  }
  val whatToSearchFor = searchType.title
  val likedSamples by searchViewModel.likedSamples.collectAsState()
  val activeCommentSampleId by searchViewModel.activeCommentSampleId.collectAsState()
  val comments by searchViewModel.comments.collectAsState()
  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = NepTuneTheme.colors.background,
        modifier = Modifier.testTag(SearchScreenTestTags.SEARCH_SCREEN),
        topBar = {
          Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally) {
                SearchBar(
                    searchText,
                    { searchText = it },
                    SearchScreenTestTags.SEARCH_BAR,
                    whatToSearchFor)

                val roundShape = 50
                Box(modifier = Modifier.padding(bottom = 8.dp)) {
                  OutlinedButton(
                      onClick = { searchViewModel.toggleSearchType() },
                      border = BorderStroke(1.dp, NepTuneTheme.colors.onBackground),
                      shape = RoundedCornerShape(roundShape),
                      colors =
                          ButtonDefaults.outlinedButtonColors(
                              contentColor = NepTuneTheme.colors.onBackground),
                      modifier = Modifier.height(36.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(
                            text = "See ${searchType.toggle().title}",
                            style =
                                TextStyle(
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                                    fontWeight = FontWeight.Bold))
                      }
                }
              }
        },
        content = { pd ->
          if (searchType == SearchType.SAMPLES) {
            ScrollableColumnOfSamples(
                samples = samples,
                searchViewModel = searchViewModel,
                modifier = Modifier.padding(pd),
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
                navigateToOtherUserProfile = { uid ->
                  if (searchViewModel.isCurrentUser(uid)) {
                    navigateToProfile()
                  } else {
                    navigateToOtherUserProfile(uid)
                  }
                },
                modifier = Modifier.padding(pd))
          }
        })
    if (downloadProgress != null && downloadProgress != 0) {
      DownloadProgressBar(downloadProgress = downloadProgress!!, SearchScreenTestTags.DOWNLOAD_BAR)
    }
  }
}

@Composable
fun ScrollableColumnOfUsers(
    users: List<Profile>,
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
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clickable { navigateToOtherUserProfile(profile.uid) }
                      .background(NepTuneTheme.colors.cardBackground, RoundedCornerShape(8.dp))
                      .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically) {
                if (profile.avatarUrl.isNotBlank()) {
                  AsyncImage(
                      model = profile.avatarUrl,
                      contentDescription = "User Avatar",
                      modifier = Modifier.size(40.dp).clip(CircleShape),
                      contentScale = ContentScale.Crop,
                      placeholder = painterResource(id = R.drawable.profile),
                      error = painterResource(id = R.drawable.profile))
                } else {
                  Icon(
                      painter = painterResource(id = R.drawable.profile),
                      contentDescription = "User Avatar",
                      modifier = Modifier.size(40.dp),
                      tint = NepTuneTheme.colors.onBackground)
                }

                Column(modifier = Modifier.padding(start = 16.dp)) {
                  Text(
                      text = profile.username.ifBlank { "User" },
                      style = MaterialTheme.typography.titleMedium,
                      color = NepTuneTheme.colors.onBackground)
                  if (!profile.name.isNullOrBlank()) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = NepTuneTheme.colors.onBackground)
                  }
                }
              }
        }
      }
}

@Composable
fun ScrollableColumnOfSamples(
    modifier: Modifier = Modifier,
    samples: List<Sample>,
    searchViewModel: SearchViewModel,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    likedSamples: Map<String, Boolean> = emptyMap(),
    activeCommentSampleId: String? = null,
    comments: List<Comment> = emptyList(),
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
    sampleResources: Map<String, SampleResourceState> = emptyMap(),
) {
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val width = screenWidth - 20.dp
  val height = width * (150f / 166f) // the same ratio than in the feedScreen
  // Ensure the possibility to like in local
  LazyColumn(
      modifier =
          modifier
              .testTag(SearchScreenTestTags.SAMPLE_LIST)
              .fillMaxSize()
              .background(NepTuneTheme.colors.background),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        items(samples) { sample ->
          LaunchedEffect(sample.id, sample.storagePreviewSamplePath) {
            searchViewModel.loadSampleResources(sample)
          }
          val resources = sampleResources[sample.id] ?: SampleResourceState()
          // change height and width if necessary
          val testTags = SearchScreenTestTagsPerSampleCard(idInColumn = sample.id)
          val isLiked = likedSamples[sample.id] == true
          val actions =
              onClickFunctions(
                  onDownloadClick = { searchViewModel.onDownloadSample(sample) },
                  onLikeClick = {
                    val newIsLiked = !isLiked
                    searchViewModel.onLikeClick(sample, newIsLiked)
                  },
                  onCommentClick = { searchViewModel.onCommentClicked(sample) },
                  onProfileClick = {
                    val ownerId = sample.ownerId
                    if (ownerId.isNotBlank()) {
                      if (searchViewModel.isCurrentUser(ownerId)) {
                        navigateToProfile()
                      } else {
                        navigateToOtherUserProfile(ownerId)
                      }
                    }
                  },
              )
          SampleItem(
              sample = sample,
              width = width,
              height = height,
              clickHandlers = actions,
              isLiked = likedSamples[sample.id] == true,
              testTags = testTags,
              mediaPlayer = mediaPlayer,
              resourceState = resources,
              iconSize = 20.dp)
        }
      } // Comment Overlay
  if (activeCommentSampleId != null) {
    val usernames by searchViewModel.usernames.collectAsState()
    CommentDialog(
        sampleId = activeCommentSampleId,
        comments = comments,
        usernames = usernames,
        onDismiss = { searchViewModel.resetCommentSampleId() },
        onAddComment = { id, text -> searchViewModel.onAddComment(id, text) })
  }
}
