package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.google.firebase.Timestamp
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
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val testSamples =
    listOf(
        Sample(
            id = "sample1",
            name = "Discover Sample",
            description = "A sample for discover feed",
            durationSeconds = 60,
            tags = listOf("test", "discover"),
            ownerId = "user1",
            storagePreviewSamplePath = "not_blank", // Important for the new logic
            likes = 2,
            usersLike = emptyList(),
            comments = 3,
            downloads = 2,
            isPublic = true),
        Sample(
            id = "sample2",
            name = "Followed Sample",
            description = "A sample from a followed user",
            durationSeconds = 120,
            tags = listOf("test", "followed"),
            ownerId = "user2",
            storagePreviewSamplePath = "not_blank", // Important for the new logic
            likes = 2,
            usersLike = emptyList(),
            comments = 3,
            downloads = 2,
            isPublic = true))

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

  private var navigateToMessagesCallback: (() -> Unit)? = null

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    // Use fake repo with initial data
    fakeSampleRepo = FakeSampleRepository(initialSamples = testSamples)
    val fakeProfileRepo = FakeProfileRepository()
    viewModel =
        MainViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            useMockData = false // Set to false to use the real logic with our fake repo
            )
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        MainScreen(
            mainViewModel = viewModel,
            navigateToOtherUserProfile = { id -> navigateToOtherUserProfileCallback?.invoke(id) },
            navigateToSelectMessages = { navigateToMessagesCallback?.invoke() })
      }
    }
    // Wait for the initial data to be loaded and UI to be ready
    composeTestRule.waitForIdle()
  }

  @Test
  fun mainScreenTopAppNavBarCanClickOnProfile() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun mainScreenTopAppNavBarCanClickOnMessages() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MESSAGE_BUTTON)
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
  /** Tests that clicking on the Messages button triggers the callback */
  @Test
  fun testClickingMessagesButtonTriggersCallback() {
    var messagesClicked = false
    navigateToMessagesCallback = { messagesClicked = true }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.MESSAGE_BUTTON)
        .assertHasClickAction()
        .performClick()

    assertTrue("Messages button click did not trigger callback", messagesClicked)
  }

  @Test
  fun canScrollToLastSampleCard() {
    val discoverSamples = viewModel.discoverSamples.value
    if (discoverSamples.isEmpty()) {
      return
    }
    val lastSample = discoverSamples.last()

    val itemsPerColumn = 2

    val columnCount = (discoverSamples.size + itemsPerColumn - 1) / itemsPerColumn
    val lastColumnIndex = if (columnCount > 0) columnCount - 1 else 0

    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasText("Discover"))
    composeTestRule.waitForIdle()

    val discoverLazyRow =
        composeTestRule
            .onAllNodes(hasAnyChild(hasTestTag(MainScreenTestTags.SAMPLE_CARD)))
            .onFirst()

    discoverLazyRow.performScrollToIndex(lastColumnIndex)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(lastSample.name, substring = true).assertIsDisplayed()
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

  @Test
  fun downloadProgressBarIsVisibleWhenProgressNonZeroOnMainScreenUseMockDataTrue() {
    // Simulate 40% progress directly on the MainViewModel
    composeTestRule.runOnIdle { viewModel.downloadProgress.value = 40 }
    composeTestRule.waitForIdle()

    // Check that the bar is displayed
    val barNode = composeTestRule.onNodeWithTag(MainScreenTestTags.DOWNLOAD_PROGRESS)
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
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.DOWNLOAD_PROGRESS).assertCountEquals(0)

    // Case 2: zero
    viewModel.downloadProgress.value = 0
    composeTestRule.waitForIdle()
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.DOWNLOAD_PROGRESS).assertCountEquals(0)
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
            Comment("1", "A", "a1", oneMinuteAgo),
            Comment("2", "B", "a2", oneHourAgo),
            Comment("3", "C", "a3", oneDayAgo),
            Comment("4", "D", "a4", oneMonthAgo),
            Comment("5", "E", "a5", oneYearAgo),
            Comment("6", "F", "a6", now))

    composeTestRule.runOnIdle {
      testComments.forEach { comment ->
        fakeSampleRepo.addComment(
            sampleId, comment.authorId, comment.authorName, comment.text, comment.timestamp!!)
      }
      viewModel.observeCommentsForSamplePublic(sampleId)
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

    composeTestRule.runOnIdle { Assert.assertEquals("artist-discover", navigatedTo) }
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
            ownerId = "artist-followed",
            storagePreviewSamplePath = "not_blank")
    composeTestRule.runOnIdle {
      try {
        val followedSamplesField = MainViewModel::class.java.getDeclaredField("_followedSamples")
        followedSamplesField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val followedSamplesFlow =
            followedSamplesField.get(viewModel) as MutableStateFlow<List<Sample>>
        followedSamplesFlow.value = listOf(followedSample)

        viewModel.discoverSamples.value = emptyList()
      } catch (e: Exception) {
        Assert.fail("Failed to set _followedSamples via reflection: ${e.message}")
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasText("Followed"))

    composeTestRule.onNodeWithText("Followed Pad").assertIsDisplayed()

    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON, useUnmergedTree = true)
        .onFirst()
        .assertHasClickAction()
        .performClick()

    composeTestRule.runOnIdle { Assert.assertEquals("artist-followed", navigatedTo) }
  }
}
