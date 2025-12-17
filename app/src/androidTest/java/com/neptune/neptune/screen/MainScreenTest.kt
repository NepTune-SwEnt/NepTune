package com.neptune.neptune.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainScreen
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.main.SampleUiActionsTestTags
import com.neptune.neptune.ui.navigation.NavigationTestTags
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** This has been written with the help of LLMs. */
private val testSamples =
    listOf(
        Sample(
            id = "sample1",
            name = "Discover Sample",
            description = "A sample for discover feed",
            durationMillis = 60,
            tags = listOf("test", "discover"),
            ownerId = "user1",
            storagePreviewSamplePath = "not_blank", // Important for the new logic
            storageProcessedSamplePath = "not_blank",
            likes = 2,
            usersLike = emptyList(),
            comments = 3,
            downloads = 2,
            isPublic = true),
        Sample(
            id = "sample3",
            name = "Private Sample",
            description = "A private sample",
            durationMillis = 45,
            tags = listOf("private"),
            ownerId = "test-user",
            storagePreviewSamplePath = "not_blank", // Important for the new logic
            storageProcessedSamplePath = "not_blank",
            likes = 5,
            usersLike = emptyList(),
            comments = 1,
            downloads = 0,
            isPublic = false),
        Sample(
            id = "sample2",
            name = "Followed Sample",
            description = "A sample from a followed user",
            durationMillis = 120,
            tags = listOf("test", "followed"),
            ownerId = "user2",
            storagePreviewSamplePath = "not_blank", // Important for the new logic
            storageProcessedSamplePath = "not_blank",
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

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    // Use fake repo with initial data
    fakeSampleRepo = FakeSampleRepository(initialSamples = testSamples)
    val fakeProfileRepo = FakeProfileRepository()
    val fakeUser =
        mockk<FirebaseUser>(relaxed = true) {
          every { uid } returns "test-user"
          every { isAnonymous } returns false
        }

    val fakeAuth = mockk<FirebaseAuth>(relaxed = true) { every { currentUser } returns fakeUser }
    viewModel =
        spyk(
            MainViewModel(
                sampleRepo = fakeSampleRepo,
                profileRepo = fakeProfileRepo,
                useMockData = false, // Set to false to use the real logic with our fake repo
                auth = fakeAuth))
    // Prevent real download side effects (Firebase / storage / coroutines timing in CI)
    every { viewModel.onDownloadZippedSample(any()) } just runs
    every { viewModel.onDownloadProcessedSample(any()) } just runs

    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        MainScreen(
            mainViewModel = viewModel,
            navigateToOtherUserProfile = { id -> navigateToOtherUserProfileCallback?.invoke(id) })
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
  fun discoverSectionDisplaysSample() {
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

  private fun cardMatcherForSampleName(sampleName: String): SemanticsMatcher =
      hasTestTag(MainScreenTestTags.SAMPLE_CARD)
          .and(hasAnyDescendant(hasText(sampleName, substring = true)))

  private fun likesNodeForSample(composeTestRule: ComposeTestRule, sampleName: String) =
      composeTestRule
          .onAllNodesWithTag(MainScreenTestTags.SAMPLE_LIKES, useUnmergedTree = true)
          .filterToOne(hasAnyAncestor(cardMatcherForSampleName(sampleName)))

  // NEW TEST: liking updates like count immediately
  @Test
  fun likingSampleUpdatesLikeCountImmediatelyWithoutRefresh() = runTest {
    val base =
        Sample(
            id = "like-test",
            name = "Like Test",
            description = "desc",
            durationMillis = 10,
            tags = emptyList(),
            ownerId = "user1",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "not_blank",
            likes = 2,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            isPublic = true)

    composeTestRule.runOnIdle { runBlocking { fakeSampleRepo.addSample(base) } }
    composeTestRule.waitForIdle()

    // Scroll until the card is on screen
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasText("Like Test"))

    // BEFORE: assert the likes text inside *that card's likes row*
    likesNodeForSample(composeTestRule, "Like Test").assert(hasAnyDescendant(hasText("2")))

    // Click like on *that same likes row*
    likesNodeForSample(composeTestRule, "Like Test").performClick()

    composeTestRule.waitForIdle()

    // AFTER
    likesNodeForSample(composeTestRule, "Like Test").assert(hasAnyDescendant(hasText("3")))
  }

  @Test
  fun sampleCardDisplaysActions() {
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
            durationMillis = 32,
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

  @Test
  fun sampleOwnerCanDeleteAnyComment() {
    // Get a sample owned by the test user
    val sample = viewModel.discoverSamples.value.first { it.ownerId == "test-user" }

    // Add a comment from another user
    val otherUserId = "otherUser"
    val commentText = "This is a comment from another user"
    val timestamp = Timestamp.now()
    fakeSampleRepo.addComment(sample.id, otherUserId, "Other User", commentText, timestamp)

    // Open the comment section
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS)[1].performClick()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    // The sample owner should see the delete button and be able to click it
    composeTestRule.onNodeWithText(commentText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_DELETE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun userCannotDeleteOthersComment() {
    val sample = viewModel.discoverSamples.value.first()
    val otherUserId = "otherUser"

    // Pretend to be a random user
    viewModel.setCurrentUserId("randomUser")

    // Add a comment from another user
    val commentText = "This is a comment from another user"
    val timestamp = Timestamp.now()
    fakeSampleRepo.addComment(sample.id, otherUserId, "Other User", commentText, timestamp)

    // Open the comment section
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS).onFirst().performClick()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    // The user should not see the delete button
    composeTestRule.onNodeWithText(commentText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_DELETE_BUTTON).assertDoesNotExist()
  }

  fun clickingDownloadOpensDownloadChoiceDialog() {
    // Click first download icon in Discover feed
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS, useUnmergedTree = true)
        .onFirst()
        .assertHasClickAction()
        .performClick()

    // Dialog buttons should appear (meaning: showDownloadPicker == true)
    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
        .assertIsDisplayed()
  }

  @Test
  fun downloadProcessedButtonCallsViewModelAndDismissesDialog() {
    val firstSample = viewModel.discoverSamples.value.first()

    // Open dialog
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS, useUnmergedTree = true)
        .onFirst()
        .performClick()

    // Click processed
    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN).performClick()

    // Verify VM call
    verify { viewModel.onDownloadProcessedSample(firstSample) }

    // Dialog should be dismissed (buttons disappear)
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
        .assertDoesNotExist()
  }

  @Test
  fun downloadZipButtonCallsViewModelAndDismissesDialog() {
    val firstSample = viewModel.discoverSamples.value.first()

    // Open dialog
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS, useUnmergedTree = true)
        .onFirst()
        .performClick()

    // Click zip
    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).performClick()

    // Verify VM call
    verify { viewModel.onDownloadZippedSample(firstSample) }

    // Dialog should be dismissed
    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).assertDoesNotExist()
  }

  @Test
  fun processedOptionDisabledOrHiddenWhenNoProcessedPath() {
    val sampleNoProcessed =
        Sample(
            id = "no-processed",
            name = "No Processed",
            description = "",
            durationMillis = 10,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u3",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "" // <- makes processedAvailable false
            )

    composeTestRule.runOnIdle {
      // Put it first so clicking first download targets it deterministically
      viewModel.discoverSamples.value = listOf(sampleNoProcessed) + viewModel.discoverSamples.value
    }
    composeTestRule.waitForIdle()

    // Open dialog for the first card
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS, useUnmergedTree = true)
        .onFirst()
        .performClick()

    // Depending on your dialog implementation:
    // - either the processed button is hidden
    // - or it exists but disabled
    val processed =
        composeTestRule.onAllNodesWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
    val nodes = processed.fetchSemanticsNodes()

    if (nodes.isEmpty()) {
      // Hidden: OK
      return
    } else {
      // Present but should be disabled
      composeTestRule
          .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
          .assertIsNotEnabled()
    }
  }

  @Test
  fun postAndDeleteComment() {
    // 1. Setup: Define user and sample
    // We use "test-user" (defined in setup) who is NOT the owner of sample1 ("user1")
    viewModel.setCurrentUserId("test-user")

    // 2. Open the comment section of the first sample
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS).onFirst().performClick()

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    // 3. Post a new comment
    val commentText = "Test Comment"
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.COMMENT_TEXT_FIELD)
        .performTextInput(commentText)

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_POST_BUTTON).performClick()

    // 4. Verify the comment appears on screen
    composeTestRule.onNodeWithText(commentText).assertIsDisplayed()

    // 5. Delete the comment
    // Since "test-user" is not the sample owner, they only see the delete button for their own
    // comment.
    // This ensures we are clicking the correct delete button.
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.COMMENT_DELETE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // 6. Verify the comment is removed
    composeTestRule.onNodeWithText(commentText).assertDoesNotExist()
  }
}
// Add this helper to your MainViewModel to allow setting the current user for tests
fun MainViewModel.setCurrentUserId(userId: String) {
  val user = mockk<FirebaseUser>(relaxed = true)
  every { user.uid } returns userId

  try {
    val field = MainViewModel::class.java.getDeclaredField("_currentUserFlow")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val flow = field.get(this) as MutableStateFlow<FirebaseUser?>
    flow.value = user
  } catch (e: NoSuchFieldException) {
    throw e
  }
}
