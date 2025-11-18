package com.neptune.neptune.screen

import android.content.Context
import android.net.Uri
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
import com.neptune.neptune.ui.post.PostViewModel
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the PostScreen. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class PostScreenTest {
  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private lateinit var context: Context
  private lateinit var viewModel: PostViewModel

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    viewModel =
        PostViewModel().apply {
          loadSample(
              Sample(
                  id = "1",
                  name = "Test Sample",
                  description = "Sample description",
                  durationSeconds = 12,
                  tags = listOf("tag1", "tag2"),
                  likes = 10,
                  usersLike = emptyList(),
                  comments = 5,
                  downloads = 3,
                  uriString = "mock_uri"))
        }
    mediaPlayer = NeptuneMediaPlayer()
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
    val mockMediaPlayer = mockk<NeptuneMediaPlayer>(relaxed = true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mockMediaPlayer) { PostScreen() }
    }
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
    val mockMediaPlayer = mockk<NeptuneMediaPlayer>(relaxed = true)

    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mockMediaPlayer) {
        PostScreen(goBack = goBackMock)
      }
    }
    composeTestRule.onNodeWithTag(PostScreenTestTags.BACK_BUTTON).performClick()
    verify(exactly = 1) { goBackMock() }
  }

  @Test
  fun waveformIconIsDisplayedWhenLocalImageUriIsNull() {
    setContent()
    assertNull(viewModel.localImageUri.value)
    composeTestRule
        .onNodeWithContentDescription("Sample's image", useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /** Tests that the title field initially displays the correct text from the ViewModel */
  @Test
  fun titleFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("Test Sample")).assertIsDisplayed()
  }

  /** Tests that the title field accepts inputs correctly */
  @Test
  fun titleFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Sweetie Banana")
    composeTestRule.onNode(hasText("Sweetie Banana")).assertIsDisplayed()
  }

  /** Tests that the description field initially displays the correct text from the ViewModel */
  @Test
  fun descriptionFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("Sample description")).assertIsDisplayed()
  }

  /** Tests that the description field accepts inputs correctly */
  @Test
  fun descriptionFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DESCRIPTION_FIELD)
        .performTextReplacement("Relax take it easy")
    composeTestRule.onNode(hasText("Relax take it easy")).assertIsDisplayed()
  }

  /** Tests that the tags field initially displays the correct text form the ViewModel */
  @Test
  fun tagsFieldHasRightText() {
    setContent()
    composeTestRule.onNode(hasText("#tag1 #tag2")).assertIsDisplayed()
  }

  /** Tests that the tag field accepts inputs correctly */
  @Test
  fun tagsFieldAcceptsTextInput() {
    setContent()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TAGS_FIELD)
        .performTextReplacement("#banana #sweet")
    composeTestRule.onNode(hasText("#banana #sweet")).assertIsDisplayed()
  }

  /** Tests that the post button is clickable */
  @Test
  fun postButtonIsClickable() {
    setContent()
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_BUTTON).performScrollTo().performClick()
  }

  /** Tests that onImageChanged with a valid URI updates the localImageUri state */
  @Test
  fun onImageChangedUpdatesLocalUri() {
    setContent()
    // Create a dummy file to act as our image
    val fakeImageFile =
        File(context.cacheDir, "fake_image.jpg").apply {
          createNewFile()
          writeText("This is a fake image.")
        }
    val fakeImageUri = Uri.fromFile(fakeImageFile)
    assertNull(viewModel.localImageUri.value)
    viewModel.onImageChanged(fakeImageUri)
    composeTestRule.waitUntil(5000) { viewModel.localImageUri.value != null }
    val newUri = viewModel.localImageUri.value
    assertNotNull(newUri)
    assertTrue(newUri.toString().contains("post_image_for_sample_1.jpg"))
    fakeImageFile.delete()
  }

  /** Tests that onImageChanged with a null URI does not change the state */
  @Test
  fun onImageChangedWithNullUriDoesNothing() {
    setContent()
    assertNull(viewModel.localImageUri.value)
    viewModel.onImageChanged(null)
    Thread.sleep(500)
    assertNull(viewModel.localImageUri.value)
  }
}
