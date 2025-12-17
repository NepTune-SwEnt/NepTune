package com.neptune.neptune.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.projectlist.ProjectListScreenTestTags
import com.neptune.neptune.ui.search.SearchScreen
import com.neptune.neptune.ui.search.SearchScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTagsPerUserCard
import com.neptune.neptune.ui.search.SearchViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the SearchScreen.This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class SearchScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private lateinit var viewModel: SearchViewModel
  private lateinit var fakeSampleRepo: FakeSampleRepository
  private lateinit var fakeProfileRepo: FakeProfileRepository

  @Before
  fun setup() {
    mediaPlayer = NeptuneMediaPlayer()
    fakeSampleRepo = FakeSampleRepository()
    fakeProfileRepo = FakeProfileRepository()
    viewModel =
        object :
            SearchViewModel(
                sampleRepo = fakeSampleRepo, profileRepo = fakeProfileRepo, useMockData = true) {
          override val isUserLoggedIn: Boolean = true
        }
    composeTestRule.setContent {
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        SearchScreen(searchViewModel = viewModel)
      }
    }
  }

  @Test
  fun commentDialogDisplaysUsernames() = runTest {
    val sample =
        Sample(
            id = "s1",
            name = "Test Sample",
            description = "Desc",
            durationMilliSecond = 60,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            ownerId = "u1")

    // Tell the fake repo that u1 is Alice
    fakeProfileRepo.setUsernameForTest("u1", "Alice")

    fakeSampleRepo.addComment(sample.id, "u1", "Alice", "Hello!")

    composeTestRule.runOnIdle { viewModel.onCommentClicked(sample) }
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithText("Hello!").assertIsDisplayed()
    composeTestRule.onNode(hasText("Alice", substring = true)).assertIsDisplayed()
  }

  @Test
  fun userSearchDisplaysResultsAndAllowsFollow() = runTest {
    // 1. Setup: Create a test user in the repository
    val testUser =
        Profile(
            uid = "user123", username = "TestUser", name = "Test Name", bio = "Bio", avatarUrl = "")
    fakeProfileRepo.addProfileForTest(testUser)
    composeTestRule.waitForIdle()

    // 2. Switch to User Search Mode
    // The button text is "See Users" when in Sample mode (default)
    composeTestRule.onNodeWithText("See Users").performClick()

    // 3. Perform Search
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
        .performTextInput("TestUser")

    // Wait for debounce (400ms in code) + small buffer
    composeTestRule.mainClock.advanceTimeBy(500)
    composeTestRule.waitForIdle()

    // 4. Verify User Card Displayed
    val userTags = SearchScreenTestTagsPerUserCard(testUser.uid)
    composeTestRule.onNodeWithTag(userTags.CARD).assertIsDisplayed()

    // 5. Test Follow Interaction
    // Initially should say "Follow"
    composeTestRule.onNodeWithTag(userTags.FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Follow").assertIsDisplayed()

    // Click Follow
    composeTestRule.onNodeWithTag(userTags.FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify it changes to "Unfollow"
    composeTestRule.onNodeWithText("Unfollow").assertIsDisplayed()
  }
}
