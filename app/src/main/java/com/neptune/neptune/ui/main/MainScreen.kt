package com.neptune.neptune.ui.main

import OfflineScreen
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.feed.FeedType
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.offline.OfflineBanner
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.formatTime
import java.util.Locale
import kotlinx.coroutines.delay

val TOP_BAR_HEIGHT = 90.dp
val PROFILE_ICON_SIZE = 57.dp
val LOGO_HEIGHT = TOP_BAR_HEIGHT - 20.dp

object MainScreenTestTags : BaseSampleTestTags {
  override val prefix = "MainScreen"

  // General
  const val MAIN_SCREEN = "mainScreen"
  const val POST_BUTTON = "postButton"
  const val DOWNLOAD_PROGRESS = "downloadProgressBar"

  // Top Bar
  const val TOP_BAR = "topBar"
  const val TOP_BAR_LOGO = "topBarLogo"

  // Sample Card
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

  // Lazy column
  const val LAZY_COLUMN_SAMPLE_LIST = "sampleList"

  // Comments
  const val COMMENT_SECTION = "commentSection"
  const val COMMENT_TEXT_FIELD = "commentTextField"
  const val COMMENT_POST_BUTTON = "commentPostButton"
  const val COMMENT_LIST = "commentList"
  const val COMMENT_PICTURE = "commentPicture"
  const val COMMENT_DELETE_BUTTON = "commentDeleteButton"
}

/**
 * Composable function representing the Main Screen. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Implementation of the main screen
fun MainScreen(
    navigateToProfile: () -> Unit = {},
    navigateToProjectList: () -> Unit = {},
    navigateToOtherUserProfile: (String) -> Unit = {},
    navigateToSampleList: (FeedType) -> Unit = {},
    mainViewModel: MainViewModel = viewModel()
) {

  val wait: Long = 300
  // Depends on the size of the screen

  val isRefreshing by mainViewModel.isRefreshing.collectAsState()
  val pullRefreshState = rememberPullToRefreshState()
  val isOnline by mainViewModel.isOnline.collectAsState()
  val nestedScrollModifier =
      if (isOnline) {
        Modifier.nestedScroll(pullRefreshState.nestedScrollConnection)
      } else {
        Modifier
      }

  LaunchEffectMain(pullRefreshState, mainViewModel, isRefreshing, wait)
  fun onCommentClicked(sample: Sample) {
    mainViewModel.openCommentSection(sample)
  }

  fun handleProfileNavigation(ownerId: String) {
    if (ownerId.isBlank()) return
    if (mainViewModel.isCurrentUser(ownerId)) {
      navigateToProfile()
    } else {
      navigateToOtherUserProfile(ownerId)
    }
  }
  Box(
      modifier =
          Modifier.fillMaxSize()
              .then(nestedScrollModifier)
              .testTag(MainScreenTestTags.MAIN_SCREEN)) {
        MainScaffold(
            mainViewModel,
            mainScaffoldNavigation =
                MainScaffoldNavigation(
                    navigateToProjectList,
                    navigateToSampleList,
                    navigateToProfile,
                ),
            { s -> onCommentClicked(s) },
            { o -> handleProfileNavigation(o) },
            isOnline,
            pullRefreshState)
      }
}

private data class MainScaffoldNavigation(
    val navigateToProjectList: () -> Unit,
    val navigateToSampleList: (FeedType) -> Unit,
    val navigateToProfile: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    mainViewModel: MainViewModel,
    mainScaffoldNavigation: MainScaffoldNavigation,
    onCommentClicked: (Sample) -> Unit,
    handleProfileNavigation: (String) -> Unit,
    isOnline: Boolean,
    pullRefreshState: PullToRefreshState
) {
  var downloadPickerSample by remember { mutableStateOf<Sample?>(null) }
  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val currentUser by mainViewModel.currentUser.collectAsState()
  val isUserLoggedIn = currentUser != null
  val maxColumns = if (screenWidth < 360.dp) 1 else 2
  val downloadProgress: Int? by mainViewModel.downloadProgress.collectAsState()
  val showDownloadPicker = downloadPickerSample != null
  val discoverSamples by mainViewModel.discoverSamples.collectAsState()
  val followedSamples by mainViewModel.followedSamples.collectAsState()
  val userAvatar by mainViewModel.userAvatar.collectAsState()
  val isAnonymous by mainViewModel.isAnonymous.collectAsState()
  val recommendedSamples by mainViewModel.recommendedSamples.collectAsState()
  Scaffold(
      topBar = {
        MainTopAppBar(
            userAvatar = userAvatar, navigateToProfile = mainScaffoldNavigation.navigateToProfile)
      },
      floatingActionButton = {
        if (isUserLoggedIn && !isAnonymous) {
          FloatingActionButton(
              onClick = mainScaffoldNavigation.navigateToProjectList,
              containerColor = NepTuneTheme.colors.postButton,
              contentColor = NepTuneTheme.colors.onBackground,
              shape = CircleShape,
              modifier =
                  Modifier.shadow(
                          elevation = 4.dp,
                          spotColor = NepTuneTheme.colors.shadow,
                          ambientColor = NepTuneTheme.colors.shadow,
                          shape = CircleShape)
                      .size(52.dp)
                      .testTag(MainScreenTestTags.POST_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create a Post",
                    modifier = Modifier.size(70.dp))
              }
        }
      },
      content = { paddingValues ->
        MainContent(
            paddingValues = paddingValues,
            mainViewModel = mainViewModel,
            maxColumns = maxColumns,
            pullRefreshState = pullRefreshState,
            mainContentState =
                MainContentState(
                    discoverSamples = recommendedSamples.ifEmpty { discoverSamples },
                    followedSamples = followedSamples,
                    isAnonymous = isAnonymous,
                    isOnline = isOnline,
                    isUserLoggedIn = isUserLoggedIn),
            mainContentActions =
                MainContentActions(
                    onCommentClicked = { onCommentClicked(it) },
                    handleProfileNavigation = { handleProfileNavigation(it) },
                    navigateToSampleList = mainScaffoldNavigation.navigateToSampleList,
                    onDownloadRequest = { sample -> downloadPickerSample = sample }))
      },
      containerColor = NepTuneTheme.colors.background)
  // Comment Overlay (Outside Scaffold content, but inside Box to float over everything)
  SampleCommentManager(
      mainViewModel = mainViewModel, onProfileClicked = { handleProfileNavigation(it) })

  if (downloadProgress != null && downloadProgress != 0) {
    DownloadProgressBar(downloadProgress = downloadProgress!!, MainScreenTestTags.DOWNLOAD_PROGRESS)
  }
  if (showDownloadPicker) {
    val s = downloadPickerSample!!
    DownloadChoiceDialog(
        sampleName = s.name,
        processedAvailable = s.storageProcessedSamplePath.isNotBlank(),
        onDismiss = { downloadPickerSample = null },
        onDownloadZip = {
          downloadPickerSample = null // hide dialog first
          mainViewModel.onDownloadZippedSample(s) // start download
        },
        onDownloadProcessed = {
          downloadPickerSample = null
          mainViewModel.onDownloadProcessedSample(s)
        })
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchEffectMain(
    pullRefreshState: PullToRefreshState,
    mainViewModel: MainViewModel,
    isRefreshing: Boolean,
    wait: Long
) {
  LaunchedEffect(pullRefreshState.isRefreshing) {
    if (pullRefreshState.isRefreshing) {
      mainViewModel.refresh()
    }
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

/** Data class to group the state variables for MainContent. */
data class MainContentState(
    val discoverSamples: List<Sample>,
    val followedSamples: List<Sample>,
    val isAnonymous: Boolean = false,
    val isOnline: Boolean = true,
    val isUserLoggedIn: Boolean = true
)

/** Data class to group the action callbacks for MainContent. */
data class MainContentActions(
    val onCommentClicked: (Sample) -> Unit,
    val handleProfileNavigation: (String) -> Unit,
    val navigateToSampleList: (FeedType) -> Unit,
    val onDownloadRequest: (Sample) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    paddingValues: PaddingValues,
    mainViewModel: MainViewModel,
    maxColumns: Int,
    pullRefreshState: PullToRefreshState,
    mainContentState: MainContentState,
    mainContentActions: MainContentActions
) {
  val horizontalPadding = 30.dp
  Box(modifier = Modifier.fillMaxSize()) {
    if (!mainContentState.isUserLoggedIn) {
      OfflineScreen()
    } else {
      Column(
          modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Top) {
            if (!mainContentState.isOnline) {
              OfflineBanner()
            }

            // online
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        bottom = paddingValues.calculateBottomPadding()), // Apply Scaffold padding
                modifier =
                    Modifier.fillMaxSize().testTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)) {
                  // ----------------Discover Section-----------------
                  item {
                    Row(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                      SectionHeader(
                          title = FeedType.DISCOVER.title,
                          onClick = { mainContentActions.navigateToSampleList(FeedType.DISCOVER) })
                    }
                  }
                  item {
                    SampleSectionLazyRow(
                        mainViewModel = mainViewModel,
                        samples = mainContentState.discoverSamples,
                        rowsPerColumn = 2,
                        onCommentClick = { mainContentActions.onCommentClicked(it) },
                        onProfileClick = { mainContentActions.handleProfileNavigation(it) },
                        onDownloadRequest = { mainContentActions.onDownloadRequest(it) },
                        isAnonymous = mainContentState.isAnonymous)
                  }
                  // ----------------Followed Section-----------------
                  item {
                    Row(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                      SectionHeader(
                          title = FeedType.FOLLOWED.title,
                          onClick = { mainContentActions.navigateToSampleList(FeedType.FOLLOWED) })
                    }
                  }
                  item {
                    SampleSectionLazyRow(
                        mainViewModel = mainViewModel,
                        samples = mainContentState.followedSamples,
                        rowsPerColumn = maxColumns,
                        onCommentClick = { mainContentActions.onCommentClicked(it) },
                        onProfileClick = { mainContentActions.handleProfileNavigation(it) },
                        onDownloadRequest = { mainContentActions.onDownloadRequest(it) },
                    )
                    Spacer(modifier = Modifier.height(50.dp))
                  }
                }
          }
      if (mainContentState.isOnline) {
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .padding(top = paddingValues.calculateTopPadding()),
            containerColor = NepTuneTheme.colors.background,
            contentColor = NepTuneTheme.colors.onBackground)
      }
    }
  }
}

@Composable
private fun SampleSectionLazyRow(
    mainViewModel: MainViewModel,
    samples: List<Sample>,
    rowsPerColumn: Int,
    onCommentClick: (Sample) -> Unit,
    onProfileClick: (String) -> Unit,
    onDownloadRequest: (Sample) -> Unit,
    isAnonymous: Boolean = false
) {
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val horizontalPadding = 30.dp
  val spacing = 25.dp
  val cardWidth = (screenWidth - horizontalPadding * 2 - spacing) / 2

  val sampleResources by mainViewModel.sampleResources.collectAsState()
  val likedSamples by mainViewModel.likedSamples.collectAsState()

  LazyRow(
      horizontalArrangement = Arrangement.spacedBy(spacing),
      contentPadding = PaddingValues(horizontal = horizontalPadding),
      modifier = Modifier.fillMaxWidth()) {
        val validSamples =
            samples.filter {
              it.storageProcessedSamplePath.isNotBlank() || it.storagePreviewSamplePath.isNotBlank()
            }
        val columns = validSamples.chunked(rowsPerColumn)

        items(columns) { samplesColumn ->
          Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            samplesColumn.forEach { sample ->
              LaunchedEffect(sample.id, sample.storagePreviewSamplePath) {
                mainViewModel.loadSampleResources(sample)
              }

              val resources = sampleResources[sample.id] ?: SampleResourceState()

              val clickHandlers =
                  onClickFunctions(
                      onDownloadClick = { onDownloadRequest(sample) },
                      onLikeClick = { isLiked ->
                        if (!isAnonymous) mainViewModel.onLikeClick(sample, isLiked)
                      },
                      onCommentClick = { onCommentClick(sample) },
                      onProfileClick = { onProfileClick(sample.ownerId) },
                  )

              SampleItem(
                  sample = sample,
                  isLiked = likedSamples[sample.id] == true,
                  clickHandlers = clickHandlers,
                  resourceState = resources,
                  sampleItemStyle = SampleItemStyle(width = cardWidth))
            }
          }
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(userAvatar: String?, navigateToProfile: () -> Unit, signedIn: Boolean = true) {
  Column {
    Box(
        modifier =
            Modifier.padding(0.dp)
                .fillMaxWidth()
                .height(TOP_BAR_HEIGHT)
                .testTag(MainScreenTestTags.TOP_BAR)) {
          // Keep the title constrained to the center area so the actions stay to the right
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.neptune_logo),
                contentDescription = "NepTune Logo",
                modifier = Modifier.height(LOGO_HEIGHT).testTag(MainScreenTestTags.TOP_BAR_LOGO),
                contentScale = ContentScale.Fit)

            // Profile Button
            if (signedIn) {
              IconButton(
                  onClick = navigateToProfile,
                  modifier =
                      Modifier.align(Alignment.CenterEnd)
                          .padding(horizontal = (TOP_BAR_HEIGHT - PROFILE_ICON_SIZE) / 2)
                          .size(PROFILE_ICON_SIZE)
                          .testTag(NavigationTestTags.PROFILE_BUTTON)) {
                    AsyncImage(
                        model =
                            ImageRequest.Builder(LocalContext.current)
                                .data(userAvatar ?: R.drawable.profile)
                                .crossfade(true)
                                .build(),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.profile),
                        error = painterResource(R.drawable.profile))
                  }
            }
          }
        }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 0.75.dp,
        color = NepTuneTheme.colors.onBackground)
  }
}

/** Data class to group the state variables for MainContent. */
data class SampleItemStyle(
    val width: Dp,
    val height: Dp = 166.dp,
    val iconSize: Dp = 16.dp,
    val showOwnerInfo: Boolean = true
)

@Composable
fun SampleItem(
    sample: Sample,
    isLiked: Boolean,
    clickHandlers: ClickHandlers,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    testTags: BaseSampleTestTags = MainScreenTestTags,
    resourceState: SampleResourceState = SampleResourceState(),
    sampleItemStyle: SampleItemStyle
) {

  Column(modifier = Modifier.width(sampleItemStyle.width)) {
    // Header (Avatar + Name)
    if (sampleItemStyle.showOwnerInfo) {
      SampleCardHeader(
          avatarUrl = resourceState.ownerAvatarUrl,
          userName = resourceState.ownerName,
          onProfileClick = clickHandlers.onProfileClick,
          testTags = testTags)
    }

    // Card (Image + Waveform + Title)
    SampleCard(
        sample = sample,
        isLiked = isLiked,
        clickHandlers = clickHandlers,
        testTags = testTags,
        mediaPlayer = mediaPlayer,
        resourceState = resourceState,
        sampleCardStyle =
            SampleCardStyle(
                width = sampleItemStyle.width,
                height = sampleItemStyle.height,
                iconSize = sampleItemStyle.iconSize))
  }
}

@Composable
fun SampleCardHeader(
    avatarUrl: String?,
    userName: String,
    onProfileClick: () -> Unit,
    testTags: BaseSampleTestTags
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        AsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl ?: R.drawable.profile)
                    .crossfade(true)
                    .build(),
            contentDescription = "Profile",
            modifier =
                Modifier.size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, NepTuneTheme.colors.onBackground, CircleShape)
                    .testTag(testTags.SAMPLE_PROFILE_ICON)
                    .clickable(onClick = onProfileClick),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.profile),
            error = painterResource(R.drawable.profile))
        Spacer(Modifier.width(8.dp))
        Text(
            text = userName,
            color = NepTuneTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                TextStyle(
                    fontSize = 19.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight(400)),
            modifier =
                Modifier.testTag(testTags.SAMPLE_USERNAME).clickable(onClick = onProfileClick))
      }
}

// ----------------Section Header-----------------
@Composable
fun SectionHeader(title: String, onClick: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp).clickable { onClick() },
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = NepTuneTheme.colors.onBackground,
            style =
                TextStyle(
                    fontSize = 37.sp,
                    fontFamily = FontFamily(Font(R.font.markazi_text)),
                    fontWeight = FontWeight(400)))
        IconButton(onClick = onClick) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "See More",
              modifier = Modifier.size(40.dp),
              tint = NepTuneTheme.colors.onBackground)
        }
      }
}

// ----------------Click Handlers for Sample Card-----------------
// Placeholder for click handlers
data class ClickHandlers(
    val onProfileClick: () -> Unit,
    val onCommentClick: () -> Unit,
    val onDownloadClick: () -> Unit,
    val onLikeClick: (Boolean) -> Unit
)

// Function to create click handlers with default empty implementations
fun onClickFunctions(
    onProfileClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onLikeClick: (Boolean) -> Unit = {}
): ClickHandlers {
  return ClickHandlers(
      onProfileClick = onProfileClick,
      onCommentClick = onCommentClick,
      onDownloadClick = onDownloadClick,
      onLikeClick = onLikeClick)
}

data class SampleCardStyle(
    val width: Dp = 150.dp,
    val height: Dp = 166.dp,
    val iconSize: Dp = 16.dp
)

// ----------------Sample Card-----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleCard(
    sample: Sample,
    isLiked: Boolean,
    testTags: BaseSampleTestTags = MainScreenTestTags,
    clickHandlers: ClickHandlers,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
    resourceState: SampleResourceState = SampleResourceState(),
    sampleCardStyle: SampleCardStyle
) {
  val likeDescription = if (isLiked) "liked" else "not liked"
  val heartColor = if (isLiked) Color.Red else NepTuneTheme.colors.background
  val heartIcon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
  var isExpanded by remember { mutableStateOf(false) }
  val bottomPartHeight = sampleCardStyle.height * 0.36f
  val imagePartHeight = sampleCardStyle.height - bottomPartHeight
  val density = LocalDensity.current
  val smallFontSize = with(density) { (sampleCardStyle.height * 0.06f).toSp() }
  val regularFontSize = with(density) { (sampleCardStyle.height * 0.072f).toSp() }

  Card(
      modifier =
          Modifier.width(sampleCardStyle.width)
              .animateContentSize()
              .clickable(
                  onClick = {
                    if (resourceState.audioUrl != null) {
                      mediaPlayer.togglePlay(resourceState.audioUrl.toUri())
                    } else {
                      mediaPlayer.togglePlay(mediaPlayer.getUriFromSampleId(sample.id))
                    }
                  })
              .testTag(testTags.SAMPLE_CARD),
      colors = CardDefaults.cardColors(containerColor = NepTuneTheme.colors.cardBackground),
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, NepTuneTheme.colors.onBackground)) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Box(modifier = Modifier.fillMaxWidth().height(imagePartHeight)) {
            SampleCardBoxContent(resourceState, imagePartHeight, sample, testTags, regularFontSize)
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
                          text = sample.tags.joinToString(" ") { "#${it}" },
                          color = NepTuneTheme.colors.background,
                          modifier = Modifier.testTag(testTags.SAMPLE_TAGS).weight(1f),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          style =
                              TextStyle(
                                  fontSize = smallFontSize,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                      Text(
                          text = if (isExpanded) "see less…" else "see more…",
                          color = NepTuneTheme.colors.background,
                          modifier = Modifier.clickable { isExpanded = !isExpanded },
                          style =
                              TextStyle(
                                  fontSize = smallFontSize,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(400)))
                    }
                if (isExpanded) {
                  Spacer(Modifier.height(4.dp))
                  Text(
                      text = sample.description,
                      color = NepTuneTheme.colors.background,
                      maxLines = 5,
                      overflow = TextOverflow.Ellipsis,
                      style =
                          TextStyle(
                              fontSize = regularFontSize,
                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                              fontWeight = FontWeight(400)))
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                      IconWithText(
                          icon = heartIcon,
                          iconDescription = "Like",
                          text = sample.likes.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_LIKES)
                                  .semantics { stateDescription = likeDescription }
                                  .clickable { clickHandlers.onLikeClick(!isLiked) },
                          tint = heartColor,
                          iconSize = sampleCardStyle.iconSize,
                          fontSize = smallFontSize)
                      IconWithTextPainter(
                          icon = painterResource(R.drawable.comments),
                          iconDescription = "Comments",
                          text = sample.comments.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_COMMENTS).clickable {
                                clickHandlers.onCommentClick()
                              },
                          iconSize = sampleCardStyle.iconSize,
                          fontSize = smallFontSize)
                      IconWithTextPainter(
                          icon = painterResource(R.drawable.download),
                          iconDescription = "Downloads",
                          text = sample.downloads.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_DOWNLOADS)
                                  .clickable(onClick = clickHandlers.onDownloadClick),
                          iconSize = sampleCardStyle.iconSize,
                          fontSize = smallFontSize)
                    }
              }
        }
      }
}

@Composable
private fun SampleCardBoxContent(
    resourceState: SampleResourceState,
    imagePartHeight: Dp,
    sample: Sample,
    testTags: BaseSampleTestTags,
    regularFontSize: TextUnit
) {
  if (resourceState.coverImageUrl != null) {
    AsyncImage(
        model =
            ImageRequest.Builder(LocalContext.current)
                .data(resourceState.coverImageUrl)
                .crossfade(true)
                .build(),
        contentDescription = "Sample Cover",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize())
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)))))
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.SpaceBetween) {
        // Waveform
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          SampleWaveform(
              amplitudes = resourceState.waveform,
              color = NepTuneTheme.colors.onBackground,
              modifier = Modifier.fillMaxWidth(0.95f).height(imagePartHeight * 0.7f))
        }

        // Sample name and duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom) {
              Text(
                  sample.name,
                  color = NepTuneTheme.colors.onBackground,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f).testTag(testTags.SAMPLE_NAME),
                  style =
                      TextStyle(
                          fontSize = regularFontSize,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(400)))

              // compute duration
              val seconds = sample.durationMillis / 1000
              val millis = (sample.durationMillis % 1000) / 10

              Text(
                  String.format(Locale.getDefault(), "%02d.%02d", seconds, millis).plus(" sec"),
                  color = NepTuneTheme.colors.onBackground,
                  modifier = Modifier.padding(start = 8.dp).testTag(testTags.SAMPLE_DURATION),
                  style =
                      TextStyle(
                          fontSize = regularFontSize,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(400)))
            }
      }
}

data class CommentDialogAction(
    val onAddComment: (sampleId: String, commentText: String) -> Unit,
    val isAnonymous: Boolean = false,
    val onProfileClicked: (String) -> Unit = {},
    val onDeleteComment: (sampleId: String, authorId: String, timestamp: Timestamp?) -> Unit =
        { _, _, _ ->
        },
)

// Comment overlay
@Composable
fun CommentDialog(
    sampleId: String,
    comments: List<Comment>,
    usernames: Map<String, String>,
    onDismiss: () -> Unit,
    sampleOwnerId: String? = null,
    currentUserId: String? = null,
    commentDialogAction: CommentDialogAction
) {
  var commentText by remember { mutableStateOf("") }
  val listScrollingState = rememberLazyListState()

  LaunchedEffect(comments.size) {
    if (comments.isNotEmpty()) {
      listScrollingState.animateScrollToItem(comments.lastIndex)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier =
            Modifier.fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f)
                .background(NepTuneTheme.colors.background, RoundedCornerShape(10.dp))
                .padding(16.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NepTuneTheme.colors.background)) {
          Column(
              modifier = Modifier.fillMaxSize().testTag(MainScreenTestTags.COMMENT_SECTION),
              verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Comments",
                    style =
                        TextStyle(
                            fontSize = 37.sp,
                            lineHeight = 90.sp,
                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                            fontWeight = FontWeight(300),
                            color = NepTuneTheme.colors.onBackground),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

                LazyColumn(
                    state = listScrollingState,
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .padding(vertical = 8.dp)
                            .testTag(MainScreenTestTags.COMMENT_LIST),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                      items(comments) { comment ->
                        val username = usernames[comment.authorId] ?: comment.authorName
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          AsyncImage(
                              model =
                                  ImageRequest.Builder(LocalContext.current)
                                      .data(
                                          comment.authorProfilePicUrl.ifEmpty {
                                            R.drawable.profile
                                          })
                                      .crossfade(true)
                                      .build(),
                              contentDescription = "Profile Picture",
                              modifier =
                                  Modifier.size(32.dp)
                                      .clip(CircleShape)
                                      .border(1.dp, NepTuneTheme.colors.onBackground, CircleShape)
                                      .clickable {
                                        commentDialogAction.onProfileClicked(comment.authorId)
                                      }
                                      .testTag(MainScreenTestTags.COMMENT_PICTURE),
                              contentScale = ContentScale.Crop,
                              placeholder = painterResource(R.drawable.profile),
                              error = painterResource(R.drawable.profile))
                          Spacer(Modifier.width(8.dp))
                          Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                  Text(
                                      text = "${username}:",
                                      style =
                                          TextStyle(
                                              fontSize = 18.sp,
                                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                                              fontWeight = FontWeight(300),
                                              color = NepTuneTheme.colors.onBackground),
                                      modifier =
                                          Modifier.clickable {
                                            commentDialogAction.onProfileClicked(comment.authorId)
                                          })
                                  Text(
                                      text = "• " + formatTime(comment.timestamp),
                                      style =
                                          TextStyle(
                                              fontSize = 14.sp,
                                              fontFamily = FontFamily(Font(R.font.markazi_text)),
                                              fontWeight = FontWeight(300),
                                              color =
                                                  NepTuneTheme.colors.onBackground.copy(
                                                      alpha = 0.9f)))
                                }
                            Text(
                                text = comment.text,
                                style =
                                    TextStyle(
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily(Font(R.font.markazi_text)),
                                        fontWeight = FontWeight(300),
                                        color = NepTuneTheme.colors.onBackground))
                          }
                          DeleteButton(
                              currentUserId,
                              comment,
                              sampleOwnerId,
                              commentDialogAction.onDeleteComment,
                              sampleId)
                        }
                      }
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      TextField(
                          value = commentText,
                          onValueChange = {
                            if (!commentDialogAction.isAnonymous) commentText = it
                          },
                          placeholder = {
                            Text(
                                commentDialogText(commentDialogAction.isAnonymous),
                                style =
                                    TextStyle(
                                        fontSize = 25.sp,
                                        fontFamily = FontFamily(Font(R.font.markazi_text)),
                                        fontWeight = FontWeight(300),
                                        color =
                                            NepTuneTheme.colors.onBackground.copy(alpha = 0.5f)),
                            )
                          },
                          modifier =
                              Modifier.weight(1f)
                                  .heightIn(min = 56.dp)
                                  .testTag(MainScreenTestTags.COMMENT_TEXT_FIELD),
                          textStyle =
                              TextStyle(
                                  fontSize = 25.sp,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(300),
                                  color = NepTuneTheme.colors.onBackground),
                          colors =
                              TextFieldDefaults.colors(
                                  focusedContainerColor = NepTuneTheme.colors.background,
                                  unfocusedContainerColor = NepTuneTheme.colors.background,
                              ))
                      Button(
                          onClick = {
                            if (commentText.isNotBlank()) {
                              commentDialogAction.onAddComment(sampleId, commentText)
                              commentText = ""
                            }
                          },
                          enabled = !commentDialogAction.isAnonymous,
                          shape = RoundedCornerShape(15.dp),
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor = NepTuneTheme.colors.indicatorColor),
                          modifier =
                              Modifier.height(35.dp)
                                  .testTag(MainScreenTestTags.COMMENT_POST_BUTTON),
                          contentPadding = PaddingValues(0.dp)) {
                            Text(
                                "Post",
                                style =
                                    TextStyle(
                                        fontSize = 25.sp,
                                        fontFamily = FontFamily(Font(R.font.markazi_text)),
                                        fontWeight = FontWeight(300),
                                        color = NepTuneTheme.colors.onBackground))
                          }
                    }
              }
        }
  }
}
// Delete button shown only to the comment author or the sample owner
// (UI-level hint).
@Composable
fun DeleteButton(
    currentUserId: String?,
    comment: Comment,
    sampleOwnerId: String?,
    onDeleteComment: (sampleId: String, authorId: String, timestamp: Timestamp?) -> Unit,
    sampleId: String
) {
  val canDelete =
      currentUserId != null && (currentUserId == comment.authorId || currentUserId == sampleOwnerId)
  if (canDelete) {
    IconButton(
        modifier = Modifier.testTag(MainScreenTestTags.COMMENT_DELETE_BUTTON),
        onClick = { onDeleteComment(sampleId, comment.authorId, comment.timestamp) }) {
          Icon(
              imageVector = Icons.Filled.Delete,
              contentDescription = "Delete comment",
              tint = NepTuneTheme.colors.onBackground)
        }
  }
}

private fun commentDialogText(isAnonymous: Boolean): String {
  return if (isAnonymous) "Cannot comment" else "Add a comment…"
}

// Helper function for icons with text
@Composable
fun IconWithText(
    icon: ImageVector,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = NepTuneTheme.colors.background,
    iconSize: Dp = 16.dp,
    fontSize: TextUnit = 10.sp
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(
        icon, contentDescription = iconDescription, tint = tint, modifier = Modifier.size(iconSize))
    Spacer(Modifier.width(3.dp))
    Text(text, color = NepTuneTheme.colors.background, fontSize = fontSize)
  }
}

// Helper function for icons with text but from a painterResource
@Composable
fun IconWithTextPainter(
    icon: Painter,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    fontSize: TextUnit = 10.sp
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(
        icon,
        contentDescription = iconDescription,
        tint = NepTuneTheme.colors.background,
        modifier = Modifier.size(iconSize))
    Spacer(Modifier.width(3.dp))
    Text(text, color = NepTuneTheme.colors.background, fontSize = fontSize)
  }
}

@Composable
fun SampleWaveform(amplitudes: List<Float>, color: Color, modifier: Modifier = Modifier) {
  if (amplitudes.isNotEmpty()) {
    Canvas(modifier = modifier) {
      val barWidth = 2.dp.toPx()
      val gapWidth = 2.dp.toPx()
      val totalBars = (size.width / (barWidth + gapWidth)).toInt()
      val centerY = size.height / 2f

      val step = (amplitudes.size / totalBars.toFloat()).coerceAtLeast(1f).toInt()

      for (i in 0 until totalBars) {
        val dataIndex = (i * step).coerceIn(amplitudes.indices)
        val normalizedAmp = amplitudes[dataIndex]

        val barHeight = normalizedAmp * size.height
        val startX = i * (barWidth + gapWidth)
        val startY = centerY - (barHeight / 2)
        val endY = centerY + (barHeight / 2)

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(startX, endY),
            strokeWidth = barWidth,
            cap = StrokeCap.Round)
      }
    }
  } else {
    Image(
        painter = painterResource(R.drawable.waveform),
        contentDescription = "Waveform",
        modifier = modifier.padding(vertical = 12.dp, horizontal = 20.dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color))
  }
}
