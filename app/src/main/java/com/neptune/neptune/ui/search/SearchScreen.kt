package com.neptune.neptune.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.Sample
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.SampleCard
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
}

class SearchScreenTestTagsPerSampleCard(private val idInColumn: Int = 0) : BaseSampleTestTags {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = viewModel(),
    onProfilePicClick: () -> Unit = {},
    onSampleClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current
) {
  val samples by searchViewModel.samples.collectAsState()
  var searchText by remember { mutableStateOf("") }
  LaunchedEffect(searchText) {
    delay(400L) // debounce time
    searchViewModel.search(searchText)
  }
  val samplesStr = "Samples"
  Scaffold(
      containerColor = NepTuneTheme.colors.background,
      modifier = Modifier.testTag(SearchScreenTestTags.SEARCH_SCREEN),
      topBar = {
        SearchBar(searchText, { searchText = it }, SearchScreenTestTags.SEARCH_BAR, samplesStr)
      },
      content = { pd ->
        ScrollableColumnOfSamples(
            samples = samples,
            onProfilePicClick = onProfilePicClick,
            onDownloadClick = onDownloadClick,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            modifier = Modifier.padding(pd),
            mediaPlayer = mediaPlayer)
      })
}

@Composable
fun ScrollableColumnOfSamples(
    samples: List<Sample>,
    onProfilePicClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current
) {
  LazyColumn(
      modifier =
          modifier
              .testTag(SearchScreenTestTags.SAMPLE_LIST)
              .fillMaxSize()
              .background(NepTuneTheme.colors.listBackground),
      horizontalAlignment = Alignment.CenterHorizontally) {
        val width = 300.dp
        items(samples) { sample ->
          // change height and width if necessary
          val testTags = SearchScreenTestTagsPerSampleCard(idInColumn = sample.id)
          println(testTags.SAMPLE_CARD)
          SampleCard(
              sample = sample,
              width = width,
              onProfileClick = onProfilePicClick,
              onLikeClick = onLikeClick,
              onDownloadClick = onDownloadClick,
              onCommentClick = onCommentClick,
              testTags = testTags,
              mediaPlayer = mediaPlayer)
        }
      }
}
