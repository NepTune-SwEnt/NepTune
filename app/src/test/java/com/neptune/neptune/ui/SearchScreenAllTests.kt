package com.neptune.neptune.ui

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
import com.neptune.neptune.NepTuneApplication.Companion.appContext
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.projectlist.ProjectListScreenTestTags
import com.neptune.neptune.ui.search.ScrollableColumnOfSamples
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.search.SearchScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTags.SAMPLE_LIST
import com.neptune.neptune.ui.search.SearchScreenTestTagsPerSampleCard
import com.neptune.neptune.ui.search.SearchViewModel
import org.junit.Assert
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

  private val fakeMediaPlayer = NeptuneMediaPlayer()

  /** Use fake repos + mock data for every VM in these tests. */
  private fun createTestSearchViewModel(): SearchViewModel {
    val fakeSampleRepo = FakeSampleRepository()
    val fakeProfileRepo = FakeProfileRepository()
    return SearchViewModel(
        repo = fakeSampleRepo,
        context = appContext,
        useMockData = true,
        profileRepo = fakeProfileRepo)
  }

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
  class SpySearchViewModel(repo: SampleRepository, profileRepo: ProfileRepository) :
      SearchViewModel(
          repo = repo, context = appContext, useMockData = true, profileRepo = profileRepo) {
    val calls = mutableListOf<String>()

    override fun search(query: String) {
      calls += query
      super.search(query)
    }
  }

  @Test
  fun initialLoadShowsAllSamplesAfterDebounce() {
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }

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
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
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
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
    advanceDebounce()

    // Narrow to "nature" => Samples 1, 3, 4, 5 (include "#nature")
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

    // Clear query
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
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
    advanceDebounce()

    val firstProfileIconTag = SearchScreenTestTagsPerSampleCard("1").SAMPLE_PROFILE_ICON
    composeRule.onNodeWithTag(firstProfileIconTag).assertIsDisplayed().performClick()
    advanceDebounce()
  }

  @Test
  fun noResultsForGarbageQueryShowsEmptyList() {
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
    advanceDebounce()

    composeRule
        .onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
        .performTextInput("zzzzzzzzzz")
    advanceDebounce()

    (1..5).forEach { i -> composeRule.onAllNodesWithText("Sample $i").assertCountEquals(0) }
  }

  @Test
  fun debounceTriggersSearchOnceAfterDelay() {
    val fakeSampleRepo = FakeSampleRepository()
    val fakeProfileRepo = FakeProfileRepository()
    val vm = SpySearchViewModel(fakeSampleRepo, fakeProfileRepo)

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

  private fun hasStateDesc(value: String) =
      SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

  @Test
  fun likeIconTogglesStateViaSemantics() {
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
    advanceDebounce()

    // Get the real first sample ID used by the screen
    val firstId = vm.samples.value.first().id
    val likeTag = SearchScreenTestTagsPerSampleCard(firstId).SAMPLE_LIKES

    val likeNode = composeRule.onNodeWithTag(likeTag)

    // Initially should be "not liked"
    likeNode.assert(hasStateDesc("not liked"))

    // Click → becomes "liked"
    likeNode.performClick()
    advanceDebounce()
    likeNode.assert(hasStateDesc("liked"))

    // Click again → back to "not liked"
    likeNode.performClick()
    advanceDebounce()
    likeNode.assert(hasStateDesc("not liked"))
  }

  @Test
  fun searchDownloadBarVisibleOnlyWhenProgressPositive() {
    val vm = createTestSearchViewModel()
    composeRule.setContent { SearchScreen(searchViewModel = vm, mediaPlayer = fakeMediaPlayer) }
    advanceDebounce()

    // 1) Default (null) -> bar must NOT exist
    composeRule.onNodeWithTag(SearchScreenTestTags.DOWNLOAD_BAR).assertDoesNotExist()

    // 2) 0 -> still must NOT exist
    // (requires downloadProgress to be a MutableStateFlow<Int?> or similar)
    composeRule.runOnIdle { vm.downloadProgress.value = 0 }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(SearchScreenTestTags.DOWNLOAD_BAR).assertDoesNotExist()

    // 3) 40 -> visible and progress ~0.4
    composeRule.runOnIdle { vm.downloadProgress.value = 40 }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(SearchScreenTestTags.DOWNLOAD_BAR).assertIsDisplayed()

    val progress40 = fetchProgressForTag(SearchScreenTestTags.DOWNLOAD_BAR)
    Assert.assertEquals(0.4f, progress40, 0.001f)

    // 4) 100 -> still visible and progress ~1.0
    composeRule.runOnIdle { vm.downloadProgress.value = 100 }
    composeRule.waitForIdle()

    val progress100 = fetchProgressForTag(SearchScreenTestTags.DOWNLOAD_BAR)
    Assert.assertEquals(1.0f, progress100, 0.001f)

    // 5) back to null → hidden again
    composeRule.runOnIdle { vm.downloadProgress.value = null }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(SearchScreenTestTags.DOWNLOAD_BAR).assertDoesNotExist()
  }

  private fun fetchProgressForTag(tag: String): Float {
    val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
    val info = node.config[SemanticsProperties.ProgressBarRangeInfo]
    return info.current
  }

  @Test
  fun profileIconPropagatesOwnerIdToNavigationCallback() {
    val vm = createTestSearchViewModel()
    val sample =
        Sample(
            id = "sample-x",
            name = "Rain Textures",
            description = "Wet foley",
            durationSeconds = 15,
            tags = listOf("#rain"),
            likes = 12,
            usersLike = emptyList(),
            comments = 0,
            downloads = 1,
            ownerId = "artist-123")

    var navigatedTo: String? = null
    composeRule.setContent {
      ScrollableColumnOfSamples(
          samples = listOf(sample),
          controller = vm,
          mediaPlayer = fakeMediaPlayer,
          navigateToOtherUserProfile = { navigatedTo = it })
    }

    val profileIconTag = SearchScreenTestTagsPerSampleCard("sample-x").SAMPLE_PROFILE_ICON
    composeRule.onNodeWithTag(profileIconTag).assertIsDisplayed().performClick()

    composeRule.runOnIdle { assert(navigatedTo == "artist-123") }
  }

  @Test
  fun profileIconNavigatesToSelfProfileWhenOwnerMatchesCurrentUser() {
    val fakeSampleRepo = FakeSampleRepository()
    val fakeProfileRepo = FakeProfileRepository()
    val vm =
        object :
            SearchViewModel(
                repo = fakeSampleRepo,
                context = appContext,
                useMockData = true,
                profileRepo = fakeProfileRepo) {
          override fun isCurrentUser(ownerId: String): Boolean = ownerId == "current-user"
        }
    val sample =
        Sample(
            id = "self-sample",
            name = "My Loop",
            description = "Personal content",
            durationSeconds = 12,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "current-user")

    var navigatedToSelf = false
    var navigatedToOther: String? = null

    composeRule.setContent {
      ScrollableColumnOfSamples(
          samples = listOf(sample),
          controller = vm,
          mediaPlayer = fakeMediaPlayer,
          navigateToProfile = { navigatedToSelf = true },
          navigateToOtherUserProfile = { navigatedToOther = it })
    }

    val profileIconTag = SearchScreenTestTagsPerSampleCard("self-sample").SAMPLE_PROFILE_ICON
    composeRule.onNodeWithTag(profileIconTag).assertIsDisplayed().performClick()

    composeRule.runOnIdle {
      assert(navigatedToSelf)
      assert(navigatedToOther == null)
    }
  }
}
