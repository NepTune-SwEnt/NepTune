package com.neptune.neptune.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.FakeSampleRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.search.SearchScreen
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
        SearchViewModel(
            sampleRepo = fakeSampleRepo,
            profileRepo = fakeProfileRepo,
            context = composeTestRule.activity.applicationContext,
            useMockData = true)
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
            durationSeconds = 60,
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
}
