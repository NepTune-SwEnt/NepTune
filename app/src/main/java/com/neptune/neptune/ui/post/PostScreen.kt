package com.neptune.neptune.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neptune.neptune.R
import com.neptune.neptune.data.rememberImagePickerLauncher
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.theme.NepTuneTheme

object PostScreenTestTags {
  // General
  const val POST_SCREEN = "postScreen"

  const val DURATION_TEXT = "durationText"
  const val AUDIENCE_ROW = "audienceRow"
  const val AUDIO_PREVIEW = "audioPreview"
  // Fields
  const val TITLE_FIELD = "textField"
  const val DESCRIPTION_FIELD = "descriptionField"
  const val TAGS_FIELD = "tagsField"
  // Button
  const val BACK_BUTTON = "backButton"
  const val SELECT_PROJECT_BUTTON = "selectProjectButton"
  const val CHANGE_IMAGE_BUTTON = "changeImageButton"
  const val POST_BUTTON = "postButton"
}

/**
 * Composable function representing the Post Screen. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    goBack: () -> Unit = {},
    navigateToProjectList: () -> Unit = {},
    navigateToMainScreen: () -> Unit = {},
    postViewModel: PostViewModel = viewModel()
) {
  val uiState by postViewModel.uiState.collectAsState()
  val localImageUri by postViewModel.localImageUri.collectAsState()

  val imagePickerLauncher =
      rememberImagePickerLauncher(
          onImageCropped = { postViewModel.onImageChanged(it) },
          aspectRatioX = 1.6f,
          aspectRatioY = 1f,
          circleDimmedLayer = false)

  var tagText by remember { mutableStateOf("") }
  var selectionTagText by remember { mutableStateOf(TextRange(0)) }
  LaunchedEffect(uiState.sample.tags) {
    // add a # at the beginning
    val text = uiState.sample.tags.joinToString(" ") { "#$it" }
    tagText = text
    selectionTagText = TextRange(text.length)
  }
  val mediaPlayer = LocalMediaPlayer.current

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              // Nothing to display, but needed for the color param
            },
            // Back Button
            navigationIcon = {
              IconButton(
                  onClick = goBack,
                  modifier =
                      Modifier.padding(vertical = 31.dp, horizontal = 7.dp)
                          .testTag(PostScreenTestTags.BACK_BUTTON),
              ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = NepTuneTheme.colors.onBackground)
              }
            },
            // Select Project Box
            actions = {
              Button(
                  onClick = navigateToProjectList,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = NepTuneTheme.colors.listBackground,
                          contentColor = NepTuneTheme.colors.searchBar),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(start = 10.dp),
                  modifier =
                      Modifier.height(40.dp)
                          .width(320.dp)
                          .padding(end = 20.dp)
                          .testTag(PostScreenTestTags.SELECT_PROJECT_BUTTON)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()) {
                          Text(
                              "Select another Project",
                              style =
                                  TextStyle(
                                      fontSize = 25.sp,
                                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                                      fontWeight = FontWeight(200)))
                        }
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NepTuneTheme.colors.background))
      },
      containerColor = NepTuneTheme.colors.background) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .testTag(PostScreenTestTags.POST_SCREEN),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Audio Preview
              Box(
                  modifier =
                      Modifier.padding(7.dp)
                          .background(NepTuneTheme.colors.cardBackground)
                          .fillMaxWidth()
                          .clickable(
                              onClick = {
                                mediaPlayer.togglePlay(
                                    mediaPlayer.getUriFromSampleId(uiState.sample.id))
                              })
                          .aspectRatio(1.6f)
                          .border(1.dp, NepTuneTheme.colors.onBackground, RoundedCornerShape(8.dp))
                          .testTag(PostScreenTestTags.AUDIO_PREVIEW),
                  contentAlignment = Alignment.Center) {
                    if (localImageUri != null) {
                      AsyncImage(
                          model = localImageUri,
                          contentDescription = "Sample's image",
                          modifier = Modifier.align(Alignment.Center).fillMaxSize(),
                          error = painterResource(id = R.drawable.waveform))
                    } else {
                      Icon(
                          painter = painterResource(id = R.drawable.waveform),
                          contentDescription = "Sample's image",
                          tint = NepTuneTheme.colors.onBackground,
                          modifier = Modifier.fillMaxWidth(0.7f).height(100.dp))
                    }
                    // Change image button
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = NepTuneTheme.colors.onBackground,
                                contentColor = NepTuneTheme.colors.background),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier =
                            Modifier.align(Alignment.BottomStart)
                                .padding(start = 8.dp, bottom = 6.dp)
                                .height(28.dp)
                                .testTag(PostScreenTestTags.CHANGE_IMAGE_BUTTON)) {
                          Icon(
                              painter = painterResource(id = R.drawable.changeicon),
                              contentDescription = "Change sample's image",
                              modifier = Modifier.size(20.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                              "Change image",
                              style =
                                  TextStyle(
                                      fontSize = 18.sp,
                                      fontFamily = FontFamily(Font(R.font.markazi_text)),
                                      fontWeight = FontWeight(200)))
                        }

                    // Duration text
                    val durationSeconds = uiState.sample.durationSeconds
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    val durationText = "%02d:%02d".format(minutes, seconds)
                    Text(
                        text = durationText,
                        style =
                            TextStyle(
                                color = NepTuneTheme.colors.onBackground,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200),
                                fontSize = 36.sp),
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 6.dp)
                                .testTag(PostScreenTestTags.DURATION_TEXT))
                  }

              // Title Field
              TextField(
                  value = uiState.sample.name,
                  onValueChange = { postViewModel.updateTitle(it) },
                  label = {
                    Text(
                        text = "Title",
                        color = NepTuneTheme.colors.onBackground,
                        style =
                            TextStyle(
                                fontSize = 28.sp,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200)))
                  },
                  textStyle =
                      TextStyle(
                          color = NepTuneTheme.colors.smallText,
                          fontSize = 24.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(200)),
                  modifier = Modifier.fillMaxWidth().testTag(PostScreenTestTags.TITLE_FIELD),
                  singleLine = true,
                  colors =
                      TextFieldDefaults.colors(
                          focusedContainerColor = Color.Transparent,
                          unfocusedContainerColor = Color.Transparent,
                          focusedIndicatorColor = NepTuneTheme.colors.onBackground,
                          unfocusedIndicatorColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.5f),
                          cursorColor = NepTuneTheme.colors.onBackground,
                          focusedLabelColor = NepTuneTheme.colors.onBackground,
                          unfocusedLabelColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.7f)))

              // Description
              TextField(
                  value = uiState.sample.description,
                  onValueChange = { postViewModel.updateDescription(it) },
                  label = {
                    Text(
                        text = "Introduce your sample",
                        color = NepTuneTheme.colors.onBackground,
                        style =
                            TextStyle(
                                fontSize = 26.sp,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200)))
                  },
                  textStyle =
                      TextStyle(
                          color = NepTuneTheme.colors.smallText,
                          fontSize = 24.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(200)),
                  modifier = Modifier.fillMaxWidth().testTag(PostScreenTestTags.DESCRIPTION_FIELD),
                  singleLine = false,
                  colors =
                      TextFieldDefaults.colors(
                          focusedContainerColor = Color.Transparent,
                          unfocusedContainerColor = Color.Transparent,
                          focusedIndicatorColor = NepTuneTheme.colors.onBackground,
                          unfocusedIndicatorColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.5f),
                          cursorColor = NepTuneTheme.colors.onBackground,
                          focusedLabelColor = NepTuneTheme.colors.onBackground,
                          unfocusedLabelColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.7f)))

              // Tags
              TextField(
                  value = TextFieldValue(tagText, selectionTagText),
                  onValueChange = { newTagValue ->
                    // Split by spaces and ensure each tag starts with #
                    val fixedTags =
                        newTagValue.text.split(" ").joinToString(" ") { tag ->
                          if (tag.isBlank()) "" else if (!tag.startsWith("#")) "#$tag" else tag
                        }

                    // Update the text and the position
                    tagText = fixedTags
                    val newCursor =
                        newTagValue.selection.end + (fixedTags.length - newTagValue.text.length)
                    selectionTagText = TextRange(newCursor.coerceIn(0, fixedTags.length))

                    // Pass tags without # to ViewModel
                    postViewModel.updateTags(fixedTags)
                  },
                  label = {
                    Text(
                        text = "Tags",
                        color = NepTuneTheme.colors.onBackground,
                        style =
                            TextStyle(
                                fontSize = 28.sp,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200)))
                  },
                  textStyle =
                      TextStyle(
                          color = NepTuneTheme.colors.smallText,
                          fontSize = 24.sp,
                          fontFamily = FontFamily(Font(R.font.markazi_text)),
                          fontWeight = FontWeight(200)),
                  modifier = Modifier.fillMaxWidth().testTag(PostScreenTestTags.TAGS_FIELD),
                  singleLine = true,
                  colors =
                      TextFieldDefaults.colors(
                          focusedContainerColor = Color.Transparent,
                          unfocusedContainerColor = Color.Transparent,
                          focusedIndicatorColor = NepTuneTheme.colors.onBackground,
                          unfocusedIndicatorColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.5f),
                          cursorColor = NepTuneTheme.colors.onBackground,
                          focusedLabelColor = NepTuneTheme.colors.onBackground,
                          unfocusedLabelColor =
                              NepTuneTheme.colors.onBackground.copy(alpha = 0.7f)))

              Spacer(modifier = Modifier.height(10.dp))

              // Audience
              Row(
                  modifier = Modifier.fillMaxWidth().testTag(PostScreenTestTags.AUDIENCE_ROW),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                          painter = painterResource(id = R.drawable.audienceicon),
                          contentDescription = "audience",
                          tint = NepTuneTheme.colors.onBackground,
                          modifier = Modifier.size(30.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                          text = "Audience:",
                          style =
                              TextStyle(
                                  color = NepTuneTheme.colors.onBackground,
                                  fontFamily = FontFamily(Font(R.font.markazi_text)),
                                  fontWeight = FontWeight(200),
                                  fontSize = 28.sp))
                    }
                    Text(
                        text = uiState.audience,
                        style =
                            TextStyle(
                                color = NepTuneTheme.colors.onBackground,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200),
                                fontSize = 28.sp))
                  }

              Spacer(modifier = Modifier.weight(1f))

              // Post Button
              Button(
                  onClick = {
                    postViewModel.submitPost()
                    navigateToMainScreen()
                  },
                  enabled = uiState.sample.name.isNotBlank(),
                  modifier =
                      Modifier.fillMaxWidth().height(55.dp).testTag(PostScreenTestTags.POST_BUTTON),
                  shape = RoundedCornerShape(8.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = NepTuneTheme.colors.onBackground,
                          contentColor = NepTuneTheme.colors.background),
                  contentPadding = PaddingValues(0.dp)) {
                    Text(
                        "Post",
                        style =
                            TextStyle(
                                fontSize = 46.sp,
                                fontFamily = FontFamily(Font(R.font.markazi_text)),
                                fontWeight = FontWeight(200)))
                  }

              Spacer(modifier = Modifier.height(12.dp))
            }
      }
}

@Preview
@Composable
fun ProjectListScreenPreview() {
  var previewViewModel: PostViewModel = viewModel()
  previewViewModel =
      previewViewModel.apply {
        loadSample(
            Sample(
                id = 0,
                name = "Grilled Banana",
                description = "Be careful not to grill your bananas",
                durationSeconds = 21,
                tags = listOf("relax", "easy"),
                likes = 123,
                comments = 45,
                downloads = 67,
                uriString = "mock_uri"))
      }

  val context = LocalContext.current
  val fakeMediaPlayer = NeptuneMediaPlayer(context)

  CompositionLocalProvider(LocalMediaPlayer provides fakeMediaPlayer) {
    PostScreen(
        goBack = {},
        navigateToProjectList = {},
        navigateToMainScreen = {},
        postViewModel = previewViewModel)
  }
}
