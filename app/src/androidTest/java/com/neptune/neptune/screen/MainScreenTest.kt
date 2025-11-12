package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.navigation.NavigationTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the MainScreen.This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: MainViewModel
  private lateinit var context: Context

  private lateinit var mediaPlayer: NeptuneMediaPlayer

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    // Use fake repo
    val fakeSampleRepo = FakeSampleRepository()
    val fakeProfileRepo = FakeProfileRepository()
    viewModel =
        MainViewModel(repo = fakeSampleRepo, profileRepo = fakeProfileRepo, useMockData = true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        MainScreen(mainViewModel = viewModel)
      }
    }
  }

  @Test
  fun mainScreen_topAppNavBar_canClickOnProfile() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun discoverSection_displaysSample() {
    composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    // Check that at least one sample card is displayed
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).onFirst().assertIsDisplayed()
  }

  @Test
  fun followedSection_isDisplayed() {
    val lazyColumn = composeTestRule.onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)

    // Scroll to the Followed Section
    lazyColumn.performScrollToNode(hasText("Followed"))
    composeTestRule.onNodeWithText("Followed").assertIsDisplayed()

    val sampleCards =
        composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).fetchSemanticsNodes()
    assert(sampleCards.isNotEmpty()) {
      "At least one sample card should be in the Followed section"
    }
  }

  @Test
  fun sampleCard_displaysDetails() {
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_USERNAME, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_NAME, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DURATION, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_TAGS, true)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun sampleCard_displaysActions() {
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_LIKES, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS, true)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS, true)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun canScrollToLastSampleCard() {
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasTestTag(MainScreenTestTags.SAMPLE_CARD))

    // When scrolling the last card should be visible
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).onLast().assertIsDisplayed()
  }

  /** Test that like button is clickable */
  @Test
  fun canLike() {
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_LIKES)
        .onFirst()
        .assertHasClickAction()
        .performClick()
  }

  /** Test that a comment can be added on a sample */
  @Test
  fun canAddCommentToSample() {
    // Scroll to a Sample card
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasTestTag(MainScreenTestTags.SAMPLE_CARD))

    // Click on comment icon
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS)
        .onFirst()
        .assertHasClickAction()
        .performClick()

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    // Type a comment
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_TEXT_FIELD).performTextInput("Banana")

    // Send the comment
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.COMMENT_POST_BUTTON)
        .assertHasClickAction()
        .performClick()

    // Verify it appears
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_LIST).assertIsDisplayed()

    composeTestRule.onNodeWithText("Banana").assertIsDisplayed()
  }
}
