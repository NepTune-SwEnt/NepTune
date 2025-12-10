package com.neptune.neptune.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.feed.FeedScreen
import com.neptune.neptune.ui.feed.FeedScreenTestTag
import com.neptune.neptune.ui.feed.FeedType
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.main.SampleResourceState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the FeedScreen. This has been written with the help of LLMs.
 *
 * @author Grégory Blanc
 */
class FeedScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockViewModel: MainViewModel
  private lateinit var mediaPlayer: NeptuneMediaPlayer

  // StateFlows to control UI state
  private val discoverFlow = MutableStateFlow<List<Sample>>(emptyList())
  private val followedFlow = MutableStateFlow<List<Sample>>(emptyList())
  private val likedSamplesFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  private val sampleResourcesFlow = MutableStateFlow<Map<String, SampleResourceState>>(emptyMap())
  private val isRefreshingFlow = MutableStateFlow(false)
  private val downloadProgressFlow = MutableStateFlow<Int?>(null)

  // Flows required by SampleCommentManager and internal logic
  private val activeCommentSampleIdFlow = MutableStateFlow<String?>(null)
  private val commentsFlow = MutableStateFlow<List<Comment>>(emptyList())
  private val usernamesFlow = MutableStateFlow<Map<String, String>>(emptyMap())
  private val userAvatarFlow = MutableStateFlow<String?>(null)

  @Before
  fun setup() {
    mockViewModel = mockk(relaxed = true)
    mediaPlayer = mockk(relaxed = true)

    // Mock all flows required by FeedScreen and its sub-components
    every { mockViewModel.discoverSamples } returns discoverFlow
    every { mockViewModel.followedSamples } returns followedFlow
    every { mockViewModel.likedSamples } returns likedSamplesFlow
    every { mockViewModel.sampleResources } returns sampleResourcesFlow
    every { mockViewModel.isRefreshing } returns isRefreshingFlow
    every { mockViewModel.downloadProgress } returns downloadProgressFlow
    every { mockViewModel.activeCommentSampleId } returns activeCommentSampleIdFlow
    every { mockViewModel.comments } returns commentsFlow
    every { mockViewModel.usernames } returns usernamesFlow
    every { mockViewModel.userAvatar } returns userAvatarFlow

    // Mock methods called in LaunchedEffects
    every { mockViewModel.loadSampleResources(any()) } just runs
    every { mockViewModel.refresh() } just runs
  }

  private fun setContent(
      initialType: FeedType = FeedType.DISCOVER,
      navigateToProfile: () -> Unit = {},
      navigateToOtherUserProfile: (String) -> Unit = {}
  ) {
    composeTestRule.setContent {
      // Provide the LocalMediaPlayer as FeedScreen uses it via SampleItem
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        FeedScreen(
            mainViewModel = mockViewModel,
            initialType = initialType,
            goBack = {},
            navigateToProfile = navigateToProfile,
            navigateToOtherUserProfile = navigateToOtherUserProfile)
      }
    }
  }

  @Test
  fun displaysDiscoverFeedByDefault() {
    val sample =
        Sample(
            id = "1",
            name = "Discover Sample",
            description = "Desc",
            durationSeconds = 10,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path")
    discoverFlow.value = listOf(sample)

    setContent(initialType = FeedType.DISCOVER)

    // Check Header Title
    composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    // Check Switch Button Text (Logic: if Discover, button says "See Followed")
    composeTestRule.onNodeWithText("See Followed").assertIsDisplayed()
    // Check Content (Sample Name)
    composeTestRule.onNodeWithText("Discover Sample").assertIsDisplayed()
  }

  @Test
  fun displaysFollowedFeedByDefault() {
    val sample =
        Sample(
            id = "2",
            name = "Followed Sample",
            description = "Desc",
            durationSeconds = 10,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u2",
            storagePreviewSamplePath = "path")
    followedFlow.value = listOf(sample)

    setContent(initialType = FeedType.FOLLOWED)

    // Check Header Title
    composeTestRule.onNodeWithText("Followed").assertIsDisplayed()
    // Check Switch Button Text (Logic: if Followed, button says "See Discover")
    composeTestRule.onNodeWithText("See Discover").assertIsDisplayed()
    // Check Content
    composeTestRule.onNodeWithText("Followed Sample").assertIsDisplayed()
  }

  @Test
  fun switchingFeedUpdatesContentAndButton() {
    val discoverSample =
        Sample(
            id = "1",
            name = "Disc Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path")
    val followedSample =
        Sample(
            id = "2",
            name = "Foll Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u2",
            storagePreviewSamplePath = "path")

    discoverFlow.value = listOf(discoverSample)
    followedFlow.value = listOf(followedSample)

    setContent(initialType = FeedType.DISCOVER)

    // Initially shows Discover
    composeTestRule.onNodeWithText("Disc Sample").assertIsDisplayed()

    // Click to switch
    composeTestRule.onNodeWithText("See Followed").performClick()

    // Should now show Followed content and title
    composeTestRule.onNodeWithText("Foll Sample").assertIsDisplayed()
    composeTestRule.onNodeWithText("Followed").assertIsDisplayed() // TopBar title
    composeTestRule.onNodeWithText("See Discover").assertIsDisplayed() // Button updated
  }

  @Test
  fun profileClickNavigatesToSelfWhenCurrentUser() {
    val sample =
        Sample(
            id = "1",
            name = "My Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "me",
            storagePreviewSamplePath = "path")
    discoverFlow.value = listOf(sample)

    // Mock isCurrentUser to return true
    every { mockViewModel.isCurrentUser("me") } returns true

    var navigatedToSelf = false
    setContent(navigateToProfile = { navigatedToSelf = true })

    // Click the profile icon (Using MainScreenTestTags as SampleItem uses them)
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON)
        .onFirst()
        .performClick()

    assertTrue(navigatedToSelf)
  }

  @Test
  fun profileClickNavigatesToOtherWhenNotCurrentUser() {
    val sample =
        Sample(
            id = "1",
            name = "Other Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "other",
            storagePreviewSamplePath = "path")
    discoverFlow.value = listOf(sample)

    // Mock isCurrentUser to return false
    every { mockViewModel.isCurrentUser("other") } returns false

    var navigatedToId: String? = null
    setContent(navigateToOtherUserProfile = { id -> navigatedToId = id })

    // Click the profile icon
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON)
        .onFirst()
        .performClick()

    assertEquals("other", navigatedToId)
  }

  @Test
  fun interactionButtonsTriggerViewModelActions() {
    val sample =
        Sample(
            id = "1",
            name = "Action Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path")
    discoverFlow.value = listOf(sample)
    setContent()

    // Like
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_LIKES).onFirst().performClick()
    verify { mockViewModel.onLikeClick(sample, any()) }

    // Download
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS).onFirst().performClick()
    verify { mockViewModel.onDownloadSample(sample) }

    // Comment
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS).onFirst().performClick()
    verify { mockViewModel.openCommentSection(sample) }
  }

  @Test
  fun downloadProgressBarAppearsWhenProgressIsSet() {
    discoverFlow.value =
        listOf(
            Sample(
                id = "1",
                name = "S",
                description = "",
                durationSeconds = 0,
                tags = listOf(),
                likes = 0,
                usersLike = listOf(),
                comments = 0,
                downloads = 0,
                ownerId = "u",
                storagePreviewSamplePath = "p"))
    setContent()

    // Initially hidden
    composeTestRule.onNodeWithTag(FeedScreenTestTag.DOWNLOAD_PROGRESS).assertDoesNotExist()

    // Set progress
    downloadProgressFlow.value = 50
    composeTestRule.waitForIdle()

    // Should be visible
    composeTestRule.onNodeWithTag(FeedScreenTestTag.DOWNLOAD_PROGRESS).assertIsDisplayed()
  }

  @Test
  fun pullToRefreshTriggersRefresh() {
    // Need content to scroll
    val sample =
        Sample(
            id = "1",
            name = "Scroll Sample",
            description = "",
            durationSeconds = 0,
            tags = listOf(),
            likes = 0,
            usersLike = listOf(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "path")
    discoverFlow.value = listOf(sample)
    setContent()

    composeTestRule.onNodeWithTag(MainScreenTestTags.SAMPLE_CARD).performTouchInput {
      swipe(start = center, end = center.copy(y = center.y + 500f), durationMillis = 1500)
    }
    composeTestRule.waitForIdle()

    // Verify refresh was called
    verify { mockViewModel.refresh() }
  }
}
