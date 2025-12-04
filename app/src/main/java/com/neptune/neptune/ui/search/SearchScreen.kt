package com.neptune.neptune.ui.search

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.feed.sampleFeedItems
import com.neptune.neptune.ui.main.CommentDialog
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.main.SampleResourceState
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

fun searchScreenFactory(application: Application) =
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
        viewModel(
            factory = searchScreenFactory(LocalContext.current.applicationContext as Application)),
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
) {
  val samples by searchViewModel.samples.collectAsState()
  var searchText by remember { mutableStateOf("") }
  val downloadProgress: Int? by searchViewModel.downloadProgress.collectAsState()
  val sampleResources by searchViewModel.sampleResources.collectAsState()

  LaunchedEffect(searchText) {
    delay(400L) // debounce time
    searchViewModel.search(searchText)
  }
  val samplesStr = "Samples"
  val likedSamples by searchViewModel.likedSamples.collectAsState()
  val activeCommentSampleId by searchViewModel.activeCommentSampleId.collectAsState()
  val comments by searchViewModel.comments.collectAsState()
  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = NepTuneTheme.colors.background,
        modifier = Modifier.testTag(SearchScreenTestTags.SEARCH_SCREEN),
        topBar = {
          SearchBar(searchText, { searchText = it }, SearchScreenTestTags.SEARCH_BAR, samplesStr)
        },
        content = { pd ->
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
        })
    if (downloadProgress != null && downloadProgress != 0) {
      DownloadProgressBar(downloadProgress = downloadProgress!!, SearchScreenTestTags.DOWNLOAD_BAR)
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
          modifier
              .testTag(SearchScreenTestTags.SAMPLE_LIST)
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
