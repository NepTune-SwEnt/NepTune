package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
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
import androidx.test.espresso.Espresso
import com.google.firebase.Timestamp
import com.neptune.neptune.NepTuneApplication.Companion.appContext
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.navigation.NavigationTestTags
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
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

  private lateinit var fakeSampleRepo: FakeSampleRepository
  private var navigateToOtherUserProfileCallback: ((String) -> Unit)? = null

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    // Use fake repo
    fakeSampleRepo = FakeSampleRepository()
    val fakeProfileRepo = FakeProfileRepository()
    viewModel =
        MainViewModel(
            repo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = appContext,
            useMockData = true)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        MainScreen(
            mainViewModel = viewModel,
            navigateToOtherUserProfile = { id -> navigateToOtherUserProfileCallback?.invoke(id) })
      }
    }
  }

  @Test
  fun mainScreenTopAppNavBarCanClickOnProfile() {
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
  fun followedSectionIsDisplayed() {
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
  fun sampleCardDisplaysDetails() {
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON, true)
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
    try {
      Espresso.closeSoftKeyboard()
    } catch (_: Exception) {}
    composeTestRule.onNodeWithText("Banana").assertIsDisplayed()
  }

  @Test
  fun downloadProgressBarIsVisibleWhenProgressNonZeroOnMainScreenUseMockDataTrue() {
    // Simulate 40% progress directly on the MainViewModel
    composeTestRule.runOnIdle { viewModel.downloadProgress.value = 40 }
    composeTestRule.waitForIdle()

    // Check that the bar is displayed
    val barNode = composeTestRule.onNodeWithTag(MainScreenTestTags.DOWNlOAD_PROGRESS)
    barNode.assertIsDisplayed()

    // Optionally check the semantic progress value ≈ 0.4
    val semantics = barNode.fetchSemanticsNode().config
    val rangeInfo = semantics[SemanticsProperties.ProgressBarRangeInfo]
    Assert.assertEquals(0.4f, rangeInfo.current, 0.01f)
  }

  @Test
  fun downloadProgressBarIsHiddenWhenProgressNullOrZeroOnMainScreenUseMockDataTrue() {
    // Case 1: null
    viewModel.downloadProgress.value = null
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.DOWNlOAD_PROGRESS).assertCountEquals(0)

    // Case 2: zero
    viewModel.downloadProgress.value = 0
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.DOWNlOAD_PROGRESS).assertCountEquals(0)
    /** Test that different timestamps on different comments display correctly */
  }

  @Test
  fun commentsDisplayCorrectTimestampFormats() {
    val sampleId = viewModel.discoverSamples.value.first().id

    // Open first comment
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS).onFirst().performClick()

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    // Fake comments with different TimeStamp
    val now = Timestamp.now()
    val oneMinuteAgo = Timestamp(now.seconds - 60, 0)
    val oneHourAgo = Timestamp(now.seconds - 3600, 0)
    val oneDayAgo = Timestamp(now.seconds - 86400, 0)
    val oneMonthAgo = Timestamp(now.seconds - 30L * 86400, 0)
    val oneYearAgo = Timestamp(now.seconds - 365L * 86400, 0)

    val testComments =
        listOf(
            Comment("A", "a1", oneMinuteAgo),
            Comment("B", "a2", oneHourAgo),
            Comment("C", "a3", oneDayAgo),
            Comment("D", "a4", oneMonthAgo),
            Comment("E", "a5", oneYearAgo),
            Comment("F", "a6", now))

    composeTestRule.runOnUiThread {
      testComments.forEach { comment ->
        fakeSampleRepo.addComment(sampleId, comment.author, comment.text, comment.timestamp!!)
      }
      viewModel.observeCommentsForSample(sampleId)
    }

    // Check that the string is well formated in each case.
    composeTestRule.onNodeWithText("• 1min ago").assertIsDisplayed()
    composeTestRule.onNodeWithText("• 1h ago").assertIsDisplayed()
    composeTestRule.onNodeWithText("• 1d ago").assertIsDisplayed()
    composeTestRule.onNodeWithText("• 1mo ago").assertIsDisplayed()
    composeTestRule.onNodeWithText("• 1y ago").assertIsDisplayed()
    composeTestRule.onNodeWithText("• just now").assertIsDisplayed()
  }

  @Test
  fun discoverCardProfileIconNavigatesWithOwnerId() {
    var navigatedTo: String? = null
    navigateToOtherUserProfileCallback = { id -> navigatedTo = id }

    val discoverSample = viewModel.discoverSamples.value.first().copy(ownerId = "artist-discover")
    composeTestRule.runOnIdle { viewModel.discoverSamples.value = listOf(discoverSample) }

    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON, true)
        .onFirst()
        .assertHasClickAction()
        .performClick()

    composeTestRule.runOnIdle { assert(navigatedTo == "artist-discover") }
  }

  @Test
  fun followedCardProfileIconNavigatesWithOwnerId() {
    var navigatedTo: String? = null
    navigateToOtherUserProfileCallback = { id -> navigatedTo = id }

    val followedSample =
        Sample(
            id = "followed-id",
            name = "Followed Pad",
            description = "Pad loop",
            durationSeconds = 32,
            tags = listOf("#pads"),
            likes = 4,
            usersLike = emptyList(),
            comments = 1,
            downloads = 7,
            ownerId = "artist-followed")
    composeTestRule.runOnIdle {
      viewModel.discoverSamples.value = emptyList()
      (viewModel.followedSamples as MutableStateFlow<List<Sample>>).value = listOf(followedSample)
    }

    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasText("Followed"))
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON, true)
        .onFirst()
        .assertHasClickAction()
        .performClick()

    composeTestRule.runOnIdle { assert(navigatedTo == "artist-followed") }
  }
}
