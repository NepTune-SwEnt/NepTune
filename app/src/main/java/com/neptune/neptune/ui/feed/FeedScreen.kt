package com.neptune.neptune.ui.feed

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
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
import com.neptune.neptune.ui.main.onClickFunctions
import com.neptune.neptune.ui.offline.OfflineBanner
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
    mainViewModel: MainViewModel = viewModel(),
    initialType: FeedType = FeedType.DISCOVER,
    goBack: () -> Unit = {},
    navigateToProfile: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {}
) {
  var currentType by remember { mutableStateOf(initialType) }

  val isRefreshing by mainViewModel.isRefreshing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()
  val downloadProgress: Int? by mainViewModel.downloadProgress.collectAsState()
  val roundShape = 50
  val isOnline by mainViewModel.isOnline.collectAsState()

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
                OutlinedButton(
                    onClick = { currentType = currentType.toggle() },
                    border = BorderStroke(1.dp, NepTuneTheme.colors.onBackground),
                    shape = RoundedCornerShape(roundShape),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = NepTuneTheme.colors.onBackground),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)) {
                      Text(
                          text = "See ${currentType.toggle().title}",
                          style =
                              TextStyle(
                                  fontSize = 16.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight.Bold))
                    }
              },
              divider = false)
        },
        containerColor = NepTuneTheme.colors.background) { paddingValues ->
          Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!isOnline) {
              OfflineBanner()
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              FeedContent(
                  modifier = Modifier.nestedScroll(pullRefreshState.nestedScrollConnection),
                  mainViewModel = mainViewModel,
                  currentType = currentType,
                  navigateToProfile = navigateToProfile,
                  navigateToOtherUserProfile = navigateToOtherUserProfile)

              if (isOnline) {
                PullToRefreshContainer(
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = NepTuneTheme.colors.background,
                    contentColor = NepTuneTheme.colors.onBackground)
              }
            }
          }
        }

    SampleCommentManager(
        mainViewModel = mainViewModel,
        onProfileClicked = { userId ->
          if (mainViewModel.isCurrentUser(userId)) {
            navigateToProfile()
          } else {
            navigateToOtherUserProfile(userId)
          }
        })
    if (downloadProgress != null && downloadProgress != 0) {
      DownloadProgressBar(
          downloadProgress = downloadProgress!!, testTag = FeedScreenTestTag.DOWNLOAD_PROGRESS)
    }
  }
}

@Composable
private fun FeedContent(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    currentType: FeedType,
    navigateToProfile: () -> Unit,
    navigateToOtherUserProfile: (String) -> Unit
) {
  val discoverListState = rememberLazyListState()
  val followedListState = rememberLazyListState()

  val discoverSamples by mainViewModel.discoverSamples.collectAsState()
  val followedSamples by mainViewModel.followedSamples.collectAsState()

  val likedSamples by mainViewModel.likedSamples.collectAsState()
  val sampleResources by mainViewModel.sampleResources.collectAsState()

  val mediaPlayer = LocalMediaPlayer.current
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val width = screenWidth - 20.dp
  val height = width * (150f / 166f)
  val effectDuration = 400

  Crossfade(
      targetState = currentType,
      label = "ListTransition",
      animationSpec = tween(durationMillis = effectDuration),
      modifier = modifier.background(NepTuneTheme.colors.background)) { type ->
        val (currentSamples, currentState) =
            when (type) {
              FeedType.DISCOVER -> discoverSamples to discoverListState
              FeedType.FOLLOWED -> followedSamples to followedListState
            }
        LazyColumn(
            state = currentState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              items(currentSamples, key = { sample -> sample.id }) { sample ->
                LaunchedEffect(sample.id) { mainViewModel.loadSampleResources(sample) }

                val resources = sampleResources[sample.id] ?: SampleResourceState()

                val clickHandlers =
                    onClickFunctions(
                        onDownloadClick = { mainViewModel.onDownloadSample(sample) },
                        onLikeClick = { isLiked -> mainViewModel.onLikeClick(sample, isLiked) },
                        onCommentClick = { mainViewModel.openCommentSection(sample) },
                        onProfileClick = {
                          if (mainViewModel.isCurrentUser(sample.ownerId)) navigateToProfile()
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
