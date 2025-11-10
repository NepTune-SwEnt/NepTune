package com.neptune.neptune.ui.search

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.ui.main.onClickFunctions
import com.neptune.neptune.ui.projectlist.ProjectListScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTags.SAMPLE_LIST
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * High-coverage UI tests for SearchScreen:
 * - Debounce behavior (search() called once for rapid typing)
 * - Initial load via debounced search("")
 * - Filtering by name/description/tags
 * - Clearing query restores all items
 * - Icon clickability (like/comment/download)
 * - Like toggle (via semantics or icon swap) written with assistance from ChatGPT
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Robolectric API level
class SearchScreenAllTests {
  @get:Rule val composeRule = createComposeRule()
  val fakeMediaPlayer = NeptuneMediaPlayer()

  /** Advance past the 300ms debounce in SearchScreen */
  private fun advanceDebounce() {
    composeRule.mainClock.advanceTimeBy(550L)
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeByFrame()
    composeRule.waitForIdle()
  }
  /**
   * Spy VM to count how many times search() is invoked. This subclasses your real SearchViewModel
   * so its dataset & normalization are used.
   */
  class SpySearchViewModel : SearchViewModel() {
    val calls = mutableListOf<String>()

    override fun search(query: String) {
      calls += query
      super.search(query)
    }
  }

  @Test
  fun initialLoadShowsAllSamplesAfterDebounce() {
    composeRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), mediaPlayer = fakeMediaPlayer)
    }

    advanceDebounce()

    composeRule.onNodeWithTag(SearchScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
    advanceDebounce()
    var numberOfSamples = 0
    (1..5).forEach { i ->
      composeRule
          .onNodeWithTag(SAMPLE_LIST)
          .performScrollToNode(hasTestTag("SearchScreen/sampleCard_$i"))
      advanceDebounce()
      composeRule.onNodeWithTag("SearchScreen/sampleCard_$i").assertIsDisplayed()
      numberOfSamples += 1
    }
    assert(numberOfSamples == 5)
  }

  @Test
  fun typingFiltersResultsByNameDescriptionOrTags() {
    composeRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    // "relax" matches Sample 3 (has tag "#relax")
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("relax")
    advanceDebounce()

    composeRule.onNodeWithText("Sample 3").assertIsDisplayed()
    listOf("Sample 1", "Sample 2", "Sample 4", "Sample 5").forEach {
      composeRule.onAllNodesWithText(it).assertCountEquals(0)
    }
  }

  @Test
  fun clearingQueryRestoresAllResults() {
    composeRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    // Narrow to "nature" => Samples 1, 3, 5 (include "#nature")
    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
        .performTextInput("nature")
    advanceDebounce()
    (1..5).forEach { i ->
      if (i != 2) {
        composeRule
            .onNodeWithTag(SAMPLE_LIST)
            .performScrollToNode(hasTestTag("SearchScreen/sampleCard_$i"))
        composeRule.onNodeWithTag("SearchScreen/sampleCard_$i").assertIsDisplayed()
      }
    }
    // Reset composition to clear input (simplest)
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextClearance()
    advanceDebounce()
    composeRule.runOnIdle {
      (1..5).forEach { i ->
        composeRule
            .onNodeWithTag(SAMPLE_LIST)
            .performScrollToNode(hasTestTag("SearchScreen/sampleCard_$i"))
        composeRule.onNodeWithTag("SearchScreen/sampleCard_$i").assertIsDisplayed()
      }
    }
  }

  @Test
  fun clickingProfileIconInvokesCallbackForFirstItem() {
    var profileClicks = 0
    composeRule.setContent {
      SearchScreen(
          searchViewModel = SearchViewModel(),
          clickHandlers = onClickFunctions(onProfileClick = { profileClicks++ }),
          mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    val firstProfileIconTag = SearchScreenTestTagsPerSampleCard(1).SAMPLE_PROFILE_ICON
    composeRule.onNodeWithTag(firstProfileIconTag).assertIsDisplayed().performClick()
    advanceDebounce()
    assert(profileClicks == 1)
  }

  @Test
  fun noResultsForGarbageQueryShowsEmptyList() {
    composeRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
        .performTextInput("zzzzzzzzzz")
    advanceDebounce()

    (1..5).forEach { i -> composeRule.onAllNodesWithText("Sample $i").assertCountEquals(0) }
  }

  @Test
  fun debounceTriggersSearchOnceAfterDelay() {
    val vm = SpySearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }

    // Rapid typing: only last value should trigger after debounce
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("a")
    composeRule.mainClock.advanceTimeBy(100)
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("b")
    composeRule.mainClock.advanceTimeBy(100)
    composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).performTextInput("c")

    advanceDebounce()

    assert(vm.calls.size == 1)
    // Depending on your TextField behavior, appended input may produce "abc"
    assert(vm.calls.first().contains("abc"))
  }

  @Test
  fun likeCommentDownloadAreClickable() {
    var likeClicks = 0
    var commentClicks = 0
    var downloadClicks = 0

    composeRule.setContent {
      SearchScreen(
          searchViewModel = SearchViewModel(),
          clickHandlers =
              onClickFunctions(
                  onLikeClick = { likeClicks++ },
                  onCommentClick = { commentClicks++ },
                  onDownloadClick = { downloadClicks++ }),
          mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    val cardTags = SearchScreenTestTagsPerSampleCard(1)
    composeRule.onNodeWithTag(cardTags.SAMPLE_LIKES).performClick()
    composeRule.onNodeWithTag(cardTags.SAMPLE_COMMENTS).performClick()
    composeRule.onNodeWithTag(cardTags.SAMPLE_DOWNLOADS).performClick()
    advanceDebounce()
    assert(likeClicks == 1)
    assert(commentClicks == 1)
    assert(downloadClicks == 1)
  }

  private fun hasStateDesc(value: String) =
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

  @Test
  fun likeIconTogglesStateViaSemantics() {
    composeRule.setContent {
      SearchScreen(searchViewModel = SearchViewModel(), mediaPlayer = fakeMediaPlayer)
    }
    advanceDebounce()

    val likeTag = SearchScreenTestTagsPerSampleCard(1).SAMPLE_LIKES
    val likeNode = composeRule.onNodeWithTag(likeTag)
    // Initially should be "not liked"
    likeNode.assert(hasStateDesc("not liked"))

    // Click → becomes "liked"
    composeRule.onNodeWithTag(likeTag).performClick()
    advanceDebounce()
    likeNode.assert(hasStateDesc("liked"))

    // Click again → back to "not liked"
    composeRule.onNodeWithTag(likeTag).performClick()
    advanceDebounce()
    likeNode.assert(hasStateDesc("not liked"))
  }
}
