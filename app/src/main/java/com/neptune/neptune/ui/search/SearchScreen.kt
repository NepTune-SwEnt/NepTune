package com.neptune.neptune.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.ClickHandlers
import com.neptune.neptune.ui.main.IconWithText
import com.neptune.neptune.ui.main.IconWithTextPainter
import com.neptune.neptune.ui.main.MainScreenTestTags
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
    clickHandlers: ClickHandlers = onClickFunctions(),
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
            clickHandlers = clickHandlers,
            modifier = Modifier.padding(pd),
            mediaPlayer = mediaPlayer)
      })
}

@Composable
fun ScrollableColumnOfSamples(
    modifier: Modifier = Modifier,
    samples: List<Sample>,
    clickHandlers: ClickHandlers,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current
) {
  LazyColumn(
      modifier =
          modifier
              .testTag(SearchScreenTestTags.SAMPLE_LIST)
              .fillMaxSize()
              .background(NepTuneTheme.colors.background),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        val width = 300.dp
        items(samples) { sample ->
          // change height and width if necessary
          val testTags = SearchScreenTestTagsPerSampleCard(idInColumn = sample.id)
          SampleCardSearchScreen(
              sample = sample,
              width = width,
              clickHandlers = clickHandlers,
              testTags = testTags,
              mediaPlayer = mediaPlayer)
        }
      }
}

@Composable
fun SampleCardSearchScreen(
    sample: Sample,
    width: Dp = 150.dp,
    height: Dp = 166.dp,
    testTags: BaseSampleTestTags = MainScreenTestTags,
    clickHandlers: ClickHandlers,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current
) {

  var isLiked by remember { mutableStateOf(false) }
  val likeDescription = if (isLiked) "liked" else "not liked"
  val heartColor = if (isLiked) Color.Red else NepTuneTheme.colors.background
  val heartIcon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
  Card(
      modifier =
          Modifier.width(width)
              .height(height)
              .clickable(
                  onClick = { mediaPlayer.togglePlay(mediaPlayer.getUriFromSampleId(sample.id)) })
              .testTag(testTags.SAMPLE_CARD),
      colors = CardDefaults.cardColors(containerColor = NepTuneTheme.colors.cardBackground),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, NepTuneTheme.colors.onBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
          // Profile and Name
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
                Icon(
                    painter = painterResource(R.drawable.profile),
                    contentDescription = "Profile",
                    tint = Color.Unspecified,
                    modifier =
                        Modifier.clickable(onClick = clickHandlers.onProfileClick)
                            .size(22.dp)
                            .testTag(testTags.SAMPLE_PROFILE_ICON))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sample.name,
                    color = NepTuneTheme.colors.onBackground,
                    modifier = Modifier.testTag(testTags.SAMPLE_USERNAME),
                    style =
                        TextStyle(
                            fontSize = 19.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
              }

          Spacer(Modifier.height(8.dp))

          // Waveform Image
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.waveform),
                contentDescription = "Waveform",
                modifier = Modifier.fillMaxWidth(0.8f).height(60.dp),
                alignment = Alignment.Center)
          }

          Spacer(Modifier.height(4.dp))

          // Sample name and duration
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    sample.name,
                    color = NepTuneTheme.colors.onBackground,
                    modifier = Modifier.padding(start = 6.dp).testTag(testTags.SAMPLE_NAME),
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
                val minutes = sample.durationSeconds / 60
                val seconds = sample.durationSeconds % 60
                val durationText = "%02d:%02d".format(minutes, seconds)
                Text(
                    durationText,
                    color = NepTuneTheme.colors.onBackground,
                    modifier = Modifier.padding(end = 8.dp).testTag(testTags.SAMPLE_DURATION),
                    style =
                        TextStyle(
                            fontSize = 10.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(400)))
              }
          // Bottom Turquoise bar
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .background(NepTuneTheme.colors.onBackground)
                      .padding(vertical = 4.dp, horizontal = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      Text(
                          sample.tags.joinToString(", "),
                          color = NepTuneTheme.colors.background,
                          modifier = Modifier.testTag(testTags.SAMPLE_TAGS),
                          style =
                              TextStyle(
                                  fontSize = 10.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                      Text(
                          text = "see more…",
                          color = NepTuneTheme.colors.background,
                          style =
                              TextStyle(
                                  fontSize = 10.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                    }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      IconWithText(
                          icon = heartIcon,
                          iconDescription = "Like",
                          text = sample.likes.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_LIKES)
                                  .semantics { stateDescription = likeDescription }
                                  .clickable(
                                      onClick = {
                                        isLiked = !isLiked
                                        clickHandlers.onLikeClick(isLiked)
                                      }),
                          tint = heartColor)

                      IconWithTextPainter(
                          icon = painterResource(R.drawable.comments),
                          iconDescription = "Comments",
                          text = sample.comments.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_COMMENTS)
                                  .clickable(onClick = clickHandlers.onCommentClick))
                      IconWithTextPainter(
                          icon = painterResource(R.drawable.download),
                          iconDescription = "Downloads",
                          text = sample.downloads.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_DOWNLOADS)
                                  .clickable(onClick = clickHandlers.onDownloadClick))
                    }
              }
        }
      }
}
