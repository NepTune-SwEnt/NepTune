package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.neptune.neptune.Sample
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.post.PostScreen
import com.neptune.neptune.ui.post.PostScreenTestTags
import com.neptune.neptune.ui.post.PostViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the PostScreen.This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class PostScreenTest {
  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private lateinit var context: Context

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val viewModel =
      PostViewModel().apply {
        loadSample(
            Sample(
                id = 1,
                name = "Test Sample",
                description = "Sample description",
                durationSeconds = 12,
                tags = listOf("tag1", "tag2"),
                likes = 10,
                comments = 5,
                downloads = 3,
                uriString = "mock_uri"))
      }

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer(context)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        PostScreen(postViewModel = viewModel)
      }
    }
  }

  @Test
  fun testTagsAreCorrect() {
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.POST_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.TAGS_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.AUDIENCE_ROW).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DURATION_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.DESCRIPTION_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.TITLE_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.CHANGE_IMAGE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.AUDIO_PREVIEW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.SELECT_PROJECT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PostScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  /** Tests that the title field initially displays the correct text from the ViewModel */
  @Test
  fun titleFieldHasRightText() {
    composeTestRule.onNode(hasText("Test Sample")).assertIsDisplayed()
  }

  /** Tests that the title field accepts inputs correctly */
  @Test
  fun titleFieldAcceptsTextInput() {
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Sweetie Banana")
    composeTestRule.onNode(hasText("Sweetie Banana")).assertIsDisplayed()
  }

  /** Tests that the description field initially displays the correct text from the ViewModel */
  @Test
  fun descriptionFieldHasRightText() {
    composeTestRule.onNode(hasText("Sample description")).assertIsDisplayed()
  }

  /** Tests that the description field accepts inputs correctly */
  @Test
  fun descriptionFieldAcceptsTextInput() {
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.DESCRIPTION_FIELD)
        .performTextReplacement("Relax take it easy")
    composeTestRule.onNode(hasText("Relax take it easy")).assertIsDisplayed()
  }

  /** Tests that the tags field initially displays the correct text form the ViewModel */
  @Test
  fun tagsFieldHasRightText() {
    composeTestRule.onNode(hasText("#tag1 #tag2")).assertIsDisplayed()
  }

  /** Tests that the tag field accepts inputs correctly */
  @Test
  fun tagsFieldAcceptsTextInput() {
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TAGS_FIELD)
        .performTextReplacement("#banana #sweet")
    composeTestRule.onNode(hasText("#banana #sweet")).assertIsDisplayed()
  }

  /** Tests that the post button is clickable */
  @Test
  fun postButtonIsClickable() {
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_BUTTON).performScrollTo().performClick()
  }
}
