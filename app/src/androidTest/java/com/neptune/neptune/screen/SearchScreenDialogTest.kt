package com.neptune.neptune.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.SampleUiActionsTestTags
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.search.SearchType
import com.neptune.neptune.ui.search.SearchViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SearchScreenDialogTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockViewModel: SearchViewModel
  private lateinit var mediaPlayer: NeptuneMediaPlayer

  // --- Flows SearchScreen reads ---
  private val samplesFlow = MutableStateFlow<List<Sample>>(emptyList())
  private val downloadProgressFlow = MutableStateFlow<Int?>(null)
  private val sampleResourcesFlow = MutableStateFlow<Map<String, SampleResourceState>>(emptyMap())

  private val searchTypeFlow = MutableStateFlow(SearchType.SAMPLES)
  private val userResultsFlow = MutableStateFlow<List<Profile>>(emptyList())

  private val followingIdsFlow = MutableStateFlow<Set<String>>(emptySet())
  private val currentUserProfileFlow = MutableStateFlow<Profile?>(null)

  private val likedSamplesFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  private val activeCommentSampleIdFlow = MutableStateFlow<String?>(null)
  private val commentsFlow = MutableStateFlow<List<Comment>>(emptyList())
  private val usernamesFlow = MutableStateFlow<Map<String, String>>(emptyMap())

  private val isOnlineFlow = MutableStateFlow(true)

  @Before
  fun setup() {
    mockViewModel = mockk(relaxed = true)
    mediaPlayer = mockk(relaxed = true)

    // Wire flows
    every { mockViewModel.samples } returns samplesFlow
    every { mockViewModel.downloadProgress } returns downloadProgressFlow
    every { mockViewModel.sampleResources } returns sampleResourcesFlow

    every { mockViewModel.searchType } returns searchTypeFlow
    every { mockViewModel.userResults } returns userResultsFlow

    every { mockViewModel.followingIds } returns followingIdsFlow
    every { mockViewModel.currentUserProfile } returns currentUserProfileFlow

    every { mockViewModel.likedSamples } returns likedSamplesFlow
    every { mockViewModel.activeCommentSampleId } returns activeCommentSampleIdFlow
    every { mockViewModel.comments } returns commentsFlow
    every { mockViewModel.usernames } returns usernamesFlow

    every { mockViewModel.isOnline } returns isOnlineFlow

    // Important: SearchScreen checks this once via remember { searchViewModel.isUserLoggedIn }
    every { mockViewModel.isUserLoggedIn } returns true

    // Prevent side-effects
    every { mockViewModel.onDownloadZippedSample(any()) } just runs
    every { mockViewModel.onDownloadProcessedSample(any()) } just runs

    // search() is called from LaunchedEffect debounce; just ignore it
    every { mockViewModel.search(any()) } just runs
  }

  private fun setContent() {
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        SearchScreen(searchViewModel = mockViewModel, mediaPlayer = mediaPlayer)
      }
    }
    composeTestRule.waitForIdle()
  }

  private fun hasTestTagStartingWith(prefix: String) =
      SemanticsMatcher("TestTag startsWith '$prefix'") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(prefix)
      }

  private fun clickFirstDownloadIconOrFail() {
    // Wait until at least one download icon exists
    composeTestRule.waitUntil(timeoutMillis = 8_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagStartingWith("SearchScreen/sampleDownloads_"), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the first download icon (don’t assume suffix is sampleId!)
    composeTestRule
        .onAllNodes(hasTestTagStartingWith("SearchScreen/sampleDownloads_"), useUnmergedTree = true)
        .onFirst()
        .performClick()

    // Dialog should exist if ZIP button exists
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun clickingDownloadOpensDialog_whenProcessedAvailable() {
    val sample =
        Sample(
            id = "s1",
            name = "With processed",
            description = "",
            durationSeconds = 1,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "processed.mp3",
            isPublic = true)

    samplesFlow.value = listOf(sample)
    setContent()

    clickFirstDownloadIconOrFail()

    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
        .assertIsDisplayed()
  }

  @Test
  fun processedButtonCallsVmAndDismissesDialog_whenProcessedAvailable() {
    val sample =
        Sample(
            id = "s1",
            name = "With processed",
            description = "",
            durationSeconds = 1,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "processed.mp3",
            isPublic = true)

    samplesFlow.value = listOf(sample)
    setContent()

    clickFirstDownloadIconOrFail()

    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN).performClick()

    verify { mockViewModel.onDownloadProcessedSample(match { it.id == "s1" }) }

    // Dialog should dismiss: both buttons disappear
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).assertDoesNotExist()
  }

  @Test
  fun zipButtonCallsVmAndDismissesDialog() {
    val sample =
        Sample(
            id = "s1",
            name = "Zip only",
            description = "",
            durationSeconds = 1,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "processed.mp3",
            isPublic = true)

    samplesFlow.value = listOf(sample)
    setContent()

    clickFirstDownloadIconOrFail()

    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).performClick()

    verify { mockViewModel.onDownloadZippedSample(match { it.id == "s1" }) }

    composeTestRule.onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN).assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN)
        .assertDoesNotExist()
  }

  @Test
  fun processedOptionDisabledOrHidden_whenNoProcessedPath() {
    val sample =
        Sample(
            id = "s2",
            name = "No processed",
            description = "",
            durationSeconds = 1,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u2",
            storagePreviewSamplePath = "not_blank",
            storageProcessedSamplePath = "",
            isPublic = true)

    samplesFlow.value = listOf(sample)
    setContent()

    clickFirstDownloadIconOrFail()

    val nodes =
        composeTestRule
            .onAllNodesWithTag(
                SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN, useUnmergedTree = true)
            .fetchSemanticsNodes()

    if (nodes.isEmpty()) {
      // hidden => OK
      return
    }

    // exists => should be disabled
    composeTestRule
        .onNodeWithTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN, useUnmergedTree = true)
        .assertIsNotEnabled()
  }
}
