package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.post.PostScreen
import com.neptune.neptune.ui.post.PostScreenTestTags
import com.neptune.neptune.ui.post.PostUiState
import com.neptune.neptune.ui.post.PostViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Tests for the PostScreen. */
class PostScreenTest {
  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private lateinit var context: Context
  private lateinit var viewModel: PostViewModel

  // Mock du state flow pour pouvoir le manipuler dans les tests
  private lateinit var uiStateFlow: MutableStateFlow<PostUiState>
  private lateinit var localImageUriFlow: MutableStateFlow<android.net.Uri?>

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    viewModel = mockk(relaxed = true)

    uiStateFlow =
        MutableStateFlow(
            PostUiState(
                sample =
                    Sample(
                        id = "1",
                        name = "Test Sample",
                        description = "Sample description",
                        durationSeconds = 12,
                        tags = listOf("tag1", "tag2"),
                        likes = 10,
                        usersLike = emptyList(),
                        comments = 5,
                        downloads = 3)))
    localImageUriFlow = MutableStateFlow(null)

    every { viewModel.uiState } returns uiStateFlow as StateFlow<PostUiState>
    every { viewModel.localImageUri } returns localImageUriFlow as StateFlow<android.net.Uri?>
    every { viewModel.isOnline } returns MutableStateFlow(true)
    every { viewModel.audioExist() } returns true
    every { viewModel.isAnonymous } returns false
  }

  private fun setContent(goBack: () -> Unit = {}, navigateToMainScreen: () -> Unit = {}) {
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        PostScreen(
            goBack = goBack, navigateToMainScreen = navigateToMainScreen, postViewModel = viewModel)
      }
    }
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()

    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_SCREEN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.POST_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TAGS_FIELD)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.AUDIENCE_ROW)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DURATION_TEXT, useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DESCRIPTION_FIELD)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.CHANGE_IMAGE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PostScreenTestTags.AUDIO_PREVIEW)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(PostScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun backButtonTriggersNavigation() {
    val goBackMock = mockk<() -> Unit>(relaxed = true)
    setContent(goBack = goBackMock)

    composeTestRule.onNodeWithTag(PostScreenTestTags.BACK_BUTTON).performClick()
    verify(exactly = 1) { goBackMock() }
  }

  @Test
  fun waveformIconIsDisplayedWhenLocalImageUriIsNull() {
    setContent()

    assertNull(viewModel.localImageUri.value)

    composeTestRule
        .onNodeWithContentDescription("Waveform placeholder", useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun titleFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("Test Sample")).assertIsDisplayed()
  }

  @Test
  fun titleFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Sweetie Banana")

    verify { viewModel.updateTitle("Sweetie Banana") }
  }

  @Test
  fun descriptionFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("Sample description")).assertIsDisplayed()
  }

  @Test
  fun descriptionFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DESCRIPTION_FIELD)
        .performTextReplacement("Relax take it easy")

    verify { viewModel.updateDescription("Relax take it easy") }
  }

  @Test
  fun tagsFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("#tag1 #tag2")).assertIsDisplayed()
  }

  @Test
  fun tagsFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TAGS_FIELD)
        .performTextReplacement("#banana #sweet")
    verify { viewModel.updateTags(any()) }
  }

  @Test
  fun postButtonIsClickable() {
    setContent()
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_BUTTON).performScrollTo().performClick()
    verify { viewModel.submitPost() }
  }

  @Test
  fun loadingOverlayIsDisplayedWhenUploading() {
    uiStateFlow.value = uiStateFlow.value.copy(isUploading = true)

    setContent()

    composeTestRule.onNodeWithTag(PostScreenTestTags.LOADING_OVERLAY).assertIsDisplayed()
  }
}
