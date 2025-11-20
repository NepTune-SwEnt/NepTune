package com.neptune.neptune.ui.main

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.BaseSampleTestTags
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.theme.NepTuneTheme
import com.neptune.neptune.util.formatTime

object MainScreenTestTags : BaseSampleTestTags {
  override val prefix = "MainScreen"

  // General
  const val MAIN_SCREEN = "mainScreen"
  const val POST_BUTTON = "postButton"
  const val DOWNlOAD_PROGRESS = "downloadProgressBar"

  // Top Bar
  const val TOP_BAR = "topBar"
  const val TOP_BAR_TITLE = "topBarTitle"

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
}

private fun factory(application: Application) =
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
          @Suppress("UNCHECKED_CAST") return MainViewModel(context = application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
      }
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
    mainViewModel: MainViewModel =
        viewModel(factory = factory(LocalContext.current.applicationContext as Application))
) {
  val discoverSamples by mainViewModel.discoverSamples.collectAsState()
  val followedSamples by mainViewModel.followedSamples.collectAsState()
  val userAvatar by mainViewModel.userAvatar.collectAsState()
  val likedSamples by mainViewModel.likedSamples.collectAsState()
  val comments by mainViewModel.comments.collectAsState()
  var activeCommentSampleId by remember { mutableStateOf<String?>(null) }

  val screenWidth = LocalConfiguration.current.screenWidthDp.dp
  val horizontalPadding = 30.dp
  val spacing = 25.dp
  // Depends on the size of the screen
  val maxColumns = if (screenWidth < 360.dp) 1 else 2
  val cardWidth = (screenWidth - horizontalPadding * 2 - spacing) / 2
  val downloadProgress by mainViewModel.downloadProgress.collectAsState()
  val lifecycleOwner = LocalLifecycleOwner.current
  // This effect was created using AI assistance
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        mainViewModel.onResume()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun onCommentClicked(sample: Sample) {
    mainViewModel.observeCommentsForSample(sample.id)
    activeCommentSampleId = sample.id
  }

  fun onAddComment(sampleId: String, text: String) {
    mainViewModel.addComment(sampleId, text)
    mainViewModel.observeCommentsForSample(sampleId)
  }
  Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
          Column {
            CenterAlignedTopAppBar(
                modifier =
                    Modifier.fillMaxWidth().height(112.dp).testTag(MainScreenTestTags.TOP_BAR),
                title = {
                  Text(
                      text = "NepTune",
                      style =
                          TextStyle(
                              fontSize = 45.sp,
                              fontFamily = FontFamily(Font(R.font.lily_script_one)),
                              fontWeight = FontWeight(149),
                              color = NepTuneTheme.colors.onBackground,
                          ),
                      modifier = Modifier.padding(25.dp).testTag(MainScreenTestTags.TOP_BAR_TITLE),
                      textAlign = TextAlign.Center)
                },
                actions = {
                  IconButton(
                      onClick = navigateToProfile,
                      modifier =
                          Modifier.padding(vertical = 25.dp, horizontal = 17.dp)
                              .size(57.dp)
                              .testTag(NavigationTestTags.PROFILE_BUTTON)) {
                        AsyncImage(
                            model = userAvatar ?: R.drawable.profile,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.profile),
                            error = painterResource(id = R.drawable.profile))
                      }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = NepTuneTheme.colors.background))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.75.dp,
                color = NepTuneTheme.colors.onBackground)
          }
        },
        floatingActionButton = {
          FloatingActionButton(
              onClick = navigateToProjectList,
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
        },
        modifier = Modifier.testTag(MainScreenTestTags.MAIN_SCREEN),
        containerColor = NepTuneTheme.colors.background) { innerPadding ->
          Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 30.dp)
                        .testTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)) {
                  // ----------------Discover Section-----------------
                  item { SectionHeader(title = "Discover") }
                  item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()) {
                          // As this element is horizontally scrollable,we can let 2
                          val columns = discoverSamples.chunked(2)

                          items(columns) { samplesColumn ->
                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                              samplesColumn.forEach { sample ->
                                val clickHandlers =
                                    onClickFunctions(
                                        onDownloadClick = {
                                          mainViewModel.onDownloadSample(sample)
                                        },
                                        onLikeClick = { isLiked ->
                                          mainViewModel.onLikeClicked(sample, isLiked)
                                        },
                                        onCommentClick = { onCommentClicked(sample) })
                                SampleCard(
                                    sample = sample,
                                    width = cardWidth,
                                    isLiked = likedSamples[sample.id] == true,
                                    clickHandlers = clickHandlers)
                              }
                            }
                          }
                        }
                  }
                  // ----------------Followed Section-----------------
                  item { SectionHeader(title = "Followed") }
                  // If the screen is too small, it will display 1 Card instead of 2
                  items(followedSamples.chunked(maxColumns)) { samples ->
                    SampleCardRow(
                        samples = samples,
                        cardWidth = cardWidth,
                        likedSamples = likedSamples,
                        onLikeClick = { sample, isLiked ->
                          mainViewModel.onLikeClicked(sample, isLiked)
                        },
                        onCommentClick = { sample -> onCommentClicked(sample) },
                        onDownloadClick = { sample -> mainViewModel.onDownloadSample(sample) })
                  }
                }
            // Comment Overlay
            if (activeCommentSampleId != null) {
              CommentDialog(
                  sampleId = activeCommentSampleId!!,
                  comments = comments,
                  onDismiss = { activeCommentSampleId = null },
                  onAddComment = { id, text -> onAddComment(id, text) })
            }
          }
        }
    if (downloadProgress != null && downloadProgress != 0) {
      DownloadProgressBar(
          downloadProgress = downloadProgress!!, MainScreenTestTags.DOWNlOAD_PROGRESS)
    }
  }
}

// ----------------Section Header-----------------
@Composable
fun SectionHeader(title: String) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
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
        IconButton(onClick = { /*Does nothing for now, Todo: Add an action with the click*/}) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "See More",
              modifier = Modifier.size(40.dp),
              tint = NepTuneTheme.colors.onBackground)
        }
      }
}

// ----------------Sample Card in Row (2 per row)-----------------
@Composable
fun SampleCardRow(
    samples: List<Sample>,
    cardWidth: Dp,
    likedSamples: Map<String, Boolean> = emptyMap(),
    onLikeClick: (Sample, Boolean) -> Unit = { _, _ -> },
    onCommentClick: (Sample) -> Unit = {},
    onDownloadClick: (Sample) -> Unit = {}
) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(25.dp)) {
        samples.forEach { sample ->
          val isLiked = likedSamples[sample.id] == true
          val clickHandlers =
              onClickFunctions(
                  onLikeClick = { isLiked -> onLikeClick(sample, isLiked) },
                  onCommentClick = { onCommentClick(sample) },
                  onDownloadClick = { onDownloadClick(sample) })
          SampleCard(
              sample = sample, width = cardWidth, isLiked = isLiked, clickHandlers = clickHandlers)
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

// ----------------Sample Card-----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleCard(
    sample: Sample,
    width: Dp = 150.dp,
    height: Dp = 166.dp,
    isLiked: Boolean,
    testTags: BaseSampleTestTags = MainScreenTestTags,
    clickHandlers: ClickHandlers,
    mediaPlayer: NeptuneMediaPlayer = LocalMediaPlayer.current,
) {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.padding(start = 6.dp).weight(1f).testTag(testTags.SAMPLE_NAME),
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
                                  .clickable { clickHandlers.onLikeClick(!isLiked) },
                          tint = heartColor)

                      IconWithTextPainter(
                          icon = painterResource(R.drawable.comments),
                          iconDescription = "Comments",
                          text = sample.comments.toString(),
                          modifier =
                              Modifier.testTag(testTags.SAMPLE_COMMENTS).clickable {
                                clickHandlers.onCommentClick()
                              })
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

// Comment overlay
@Composable
fun CommentDialog(
    sampleId: String,
    comments: List<Comment>,
    onDismiss: () -> Unit,
    onAddComment: (sampleId: String, commentText: String) -> Unit
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
                        Column {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${comment.author}:",
                                    style =
                                        TextStyle(
                                            fontSize = 18.sp,
                                            fontFamily = FontFamily(Font(R.font.markazi_text)),
                                            fontWeight = FontWeight(300),
                                            color = NepTuneTheme.colors.onBackground))
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
                      }
                    }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      TextField(
                          value = commentText,
                          onValueChange = { commentText = it },
                          placeholder = {
                            Text(
                                "Add a comment…",
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
                              onAddComment(sampleId, commentText)
                              commentText = ""
                            }
                          },
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

// Helper function for icons with text
@Composable
fun IconWithText(
    icon: ImageVector,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = NepTuneTheme.colors.background
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(icon, contentDescription = iconDescription, tint = tint, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(3.dp))
    Text(text, color = NepTuneTheme.colors.background, fontSize = 10.sp)
  }
}

// Helper function for icons with text but from a painterResource
@Composable
fun IconWithTextPainter(
    icon: Painter,
    iconDescription: String,
    text: String,
    modifier: Modifier = Modifier
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
    Icon(
        icon,
        contentDescription = iconDescription,
        tint = NepTuneTheme.colors.background,
        modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(3.dp))
    Text(text, color = NepTuneTheme.colors.background, fontSize = 10.sp)
  }
}
