package com.neptune.neptune.ui.feed

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.main.DownloadProgressBar
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.main.SampleCommentManager
import com.neptune.neptune.ui.main.SampleItem
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.factory
import com.neptune.neptune.ui.main.onClickFunctions
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.ui.util.NeptuneTopBar
import kotlinx.coroutines.delay

object FeedScreenTestTag : BaseSampleTestTags {
  override val prefix: String = "FeedScreen"
  const val DOWNLOAD_PROGRESS = "feedDownloadProgressBar"
}

/**
 * Composable function representing the feed Screen. This has been written with the help of LLMs.
 *
 * @author Grégory Blanc
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    mainViewModel: MainViewModel =
        viewModel(factory = factory(LocalContext.current.applicationContext as Application)),
    initialType: FeedType = FeedType.DISCOVER,
    goBack: () -> Unit = {},
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {}
) {
  var currentType by remember { mutableStateOf(initialType) }
  val discoverListState = rememberLazyListState()
  val followedListState = rememberLazyListState()

  val isDiscover = currentType == FeedType.DISCOVER

  val activeListState = if (isDiscover) discoverListState else followedListState
  val switchButtonText = "See ${currentType.toggle().title}"

  val discoverSamples by mainViewModel.discoverSamples.collectAsState()
  val followedSamples by mainViewModel.followedSamples.collectAsState()

  val samples = if (isDiscover) discoverSamples else followedSamples

  val likedSamples by mainViewModel.likedSamples.collectAsState()
  val sampleResources by mainViewModel.sampleResources.collectAsState()
  val mediaPlayer = LocalMediaPlayer.current
  val isRefreshing by mainViewModel.isRefreshing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val width = screenWidth - 20.dp
  val height = width * (150f / 166f) // the same ratio than in the mainScreen
  val fontWeight = 400
  val downloadProgress: Int? by mainViewModel.downloadProgress.collectAsState()

  PullToRefreshHandler(
      isRefreshing = isRefreshing,
      pullRefreshState = pullRefreshState,
      onRefresh = { mainViewModel.refresh() })

  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          NeptuneTopBar(
              title = currentType.title,
              goBack = goBack,
              actions = {
                TextButton(onClick = { currentType = currentType.toggle() }) {
                  Text(
                      text = switchButtonText,
                      color = NepTuneTheme.colors.onBackground,
                      style =
                          TextStyle(
                              fontSize = 18.sp,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(fontWeight)))
                }
              },
              divider = false)
        },
        containerColor = NepTuneTheme.colors.background) { paddingValues ->
          Box(
              modifier =
                  Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)
                      .background(NepTuneTheme.colors.background)) {
                LazyColumn(
                    state = activeListState,
                    modifier =
                        Modifier.padding(paddingValues)
                            .fillMaxSize()
                            .background(NepTuneTheme.colors.background),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      items(samples, key = { sample -> sample.id }) { sample ->
                        LaunchedEffect(sample.id) { mainViewModel.loadSampleResources(sample) }

                        val resources = sampleResources[sample.id] ?: SampleResourceState()

                        val clickHandlers =
                            onClickFunctions(
                                onDownloadClick = { mainViewModel.onDownloadSample(sample) },
                                onLikeClick = { isLiked ->
                                  mainViewModel.onLikeClicked(sample, isLiked)
                                },
                                onCommentClick = { mainViewModel.openCommentSection(sample) },
                                onProfileClick = {
                                  if (mainViewModel.isCurrentUser(sample.ownerId))
                                      navigateToProfile()
                                  else navigateToOtherUserProfile(sample.ownerId)
                                })

                        SampleItem(
                            sample = sample,
                            width = width,
                            height = height,
                            isLiked = likedSamples[sample.id] == true,
                            clickHandlers = clickHandlers,
                            resourceState = resources,
                            mediaPlayer = mediaPlayer,
                            iconSize = 20.dp)
                      }

                      item { Spacer(modifier = Modifier.height(20.dp)) }
                    }

                PullToRefreshContainer(
                    state = pullRefreshState,
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(top = paddingValues.calculateTopPadding()),
                    containerColor = NepTuneTheme.colors.background,
                    contentColor = NepTuneTheme.colors.onBackground)
              }
        }
    SampleCommentManager(mainViewModel = mainViewModel)
    if (downloadProgress != null && downloadProgress != 0) {
      DownloadProgressBar(
          downloadProgress = downloadProgress!!, testTag = FeedScreenTestTag.DOWNLOAD_PROGRESS)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullToRefreshHandler(
    isRefreshing: Boolean,
    pullRefreshState: PullToRefreshState,
    onRefresh: () -> Unit,
    wait: Long = 300
) {
  if (pullRefreshState.isRefreshing) {
    LaunchedEffect(true) { onRefresh() }
  }

  LaunchedEffect(isRefreshing) {
    if (isRefreshing) {
      pullRefreshState.startRefresh()
    } else {
      if (pullRefreshState.isRefreshing) {
        delay(wait)
        pullRefreshState.endRefresh()
      }
    }
  }
}
