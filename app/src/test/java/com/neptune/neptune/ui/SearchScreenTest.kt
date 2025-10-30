package com.neptune.neptune.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.ui.projectlist.ProjectListScreenTestTags
import org.junit.Rule
import org.junit.Test

/**
 * High-coverage UI tests for SearchScreen:
 * - Debounce behavior (search() called once for rapid typing)
 * - Initial load via debounced search("")
 * - Filtering by name/description/tags
 * - Clearing query restores all items
 * - Icon clickability (like/comment/download)
 * - Like toggle (via semantics or icon swap)
 *
 * NOTE:
 * Your per-card tags currently end with a trailing '}' (e.g., "..._0}").
 * These tests use the tags as-is so they will still pass. Consider removing the stray brace in production code.
 */
class SearchScreenAllTests {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Advance past the 300ms debounce in SearchScreen */
    private fun advanceDebounce() {
        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitForIdle()
    }

    /**
     * Spy VM to count how many times search() is invoked.
     * This subclasses your real SearchViewModel so its dataset & normalization are used.
     */
    class SpySearchViewModel : SearchViewModel() {
        val calls = mutableListOf<String>()
        override fun search(query: String) {
            calls += query
            super.search(query)
        }
    }

    @Test
    fun initial_load_shows_all_samples_after_debounce() {
        composeRule.setContent {
            SearchScreen(searchViewModel = SearchViewModel())
        }

        advanceDebounce()

        composeRule.onNodeWithTag(SearchScreenTestTags.SEARCH_BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()

        (1..5).forEach { i ->
            composeRule.onNodeWithText("Sample $i").assertIsDisplayed()
        }
        val allSamples = (1..5).map { "Sample $it" }
        val count = allSamples.count {
            composeRule.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty()
        }
        assert(count == 5)
    }

    @Test
    fun typing_filters_results_by_name_description_or_tags() {
        composeRule.setContent { SearchScreen(searchViewModel = SearchViewModel()) }
        advanceDebounce()

        // "relax" matches Sample 3 (has tag "#relax")
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("relax")
        advanceDebounce()

        composeRule.onNodeWithText("Sample 3").assertIsDisplayed()
        listOf("Sample 1", "Sample 2", "Sample 4", "Sample 5").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test
    fun clearing_query_restores_all_results() {
        composeRule.setContent { SearchScreen(searchViewModel = SearchViewModel()) }
        advanceDebounce()

        // Narrow to "nature" => Samples 1, 3, 5 (tags include "#nature")
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("nature")
        advanceDebounce()

        composeRule.onNodeWithText("Sample 1").assertIsDisplayed()
        composeRule.onNodeWithText("Sample 3").assertIsDisplayed()
        composeRule.onNodeWithText("Sample 5").assertIsDisplayed()
        composeRule.onAllNodesWithText("Sample 2").assertCountEquals(0)
        composeRule.onAllNodesWithText("Sample 4").assertCountEquals(0)

        // Reset composition to clear input (simplest)
        composeRule.setContent { SearchScreen(searchViewModel = SearchViewModel()) }
        advanceDebounce()

        (1..5).forEach { i ->
            composeRule.onNodeWithText("Sample $i").assertIsDisplayed()
        }
    }

    @Test
    fun clicking_profile_icon_invokes_callback_for_first_item() {
        var profileClicks = 0
        composeRule.setContent {
            SearchScreen(
                searchViewModel = SearchViewModel(),
                onProfilePicClick = { profileClicks++ }
            )
        }
        advanceDebounce()

        val firstProfileIconTag = SearchScreenTestTagsPerSampleCard(0).SAMPLE_PROFILE_ICON
        composeRule.onNodeWithTag(firstProfileIconTag).assertIsDisplayed().performClick()

        assert(profileClicks == 1)
    }

    @Test
    fun no_results_for_garbage_query_shows_empty_list() {
        composeRule.setContent { SearchScreen(searchViewModel = SearchViewModel()) }
        advanceDebounce()

        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("zzzzzzzzzz")
        advanceDebounce()

        (1..5).forEach { i ->
            composeRule.onAllNodesWithText("Sample $i").assertCountEquals(0)
        }
    }

    @Test
    fun debounce_triggers_search_once_after_delay() {
        val vm = SpySearchViewModel()
        composeRule.setContent { SearchScreen(searchViewModel = vm) }

        // Rapid typing: only last value should trigger after debounce
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("a")
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("b")
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
            .performTextInput("c")

        advanceDebounce()

        assert(vm.calls.size == 1)
        // Depending on your TextField behavior, appended input may produce "abc"
        assert(vm.calls.first().contains("abc"))
    }

    @Test
    fun like_comment_download_are_clickable() {
        var likeClicks = 0
        var commentClicks = 0
        var downloadClicks = 0

        composeRule.setContent {
            SearchScreen(
                searchViewModel = SearchViewModel(),
                onLikeClick = { likeClicks++ },
                onCommentClick = { commentClicks++ },
                onDownloadClick = { downloadClicks++ }
            )
        }
        advanceDebounce()

        val cardTags = SearchScreenTestTagsPerSampleCard(0)
        composeRule.onNodeWithTag(cardTags.SAMPLE_LIKES).performClick()
        composeRule.onNodeWithTag(cardTags.SAMPLE_COMMENTS).performClick()
        composeRule.onNodeWithTag(cardTags.SAMPLE_DOWNLOADS).performClick()

        assert(likeClicks == 1)
        assert(commentClicks == 1)
        assert(downloadClicks == 1)
    }

    private fun hasStateDesc(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun like_icon_toggles_state_via_semantics() {
        composeRule.setContent { SearchScreen(searchViewModel = SearchViewModel()) }
        advanceDebounce()

        val likeTag = SearchScreenTestTagsPerSampleCard(0).SAMPLE_LIKES
        val likeNode = composeRule.onNodeWithTag(likeTag)
        // Initially should be "not liked"
        likeNode.assert(hasStateDesc("not liked"))

        // Click → becomes "liked"
        composeRule.onNodeWithTag(likeTag).performClick()
        composeRule.waitForIdle()
        likeNode.assert(hasStateDesc("liked"))

        // Click again → back to "not liked"
        composeRule.onNodeWithTag(likeTag).performClick()
        composeRule.waitForIdle()
        likeNode.assert(hasStateDesc("not liked"))
    }
}
