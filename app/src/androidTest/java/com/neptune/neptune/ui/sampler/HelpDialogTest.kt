package com.neptune.neptune.ui.sampler

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.MainActivity
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.screen.FakeSamplerViewModel
import com.neptune.neptune.screen.SamplerViewModelFactory
import com.neptune.neptune.ui.theme.SampleAppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpDialogTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
  private lateinit var fakeViewModel: FakeSamplerViewModel

  @Before
  fun setUp() {
    fakeViewModel = FakeSamplerViewModel()
    val factory = SamplerViewModelFactory(fakeViewModel)
    composeRule.activity.setContent {
      val mediaPlayer = NeptuneMediaPlayer()
      CompositionLocalProvider(LocalMediaPlayer provides mediaPlayer) {
        SampleAppTheme {
          Surface(color = MaterialTheme.colorScheme.background) {
            SamplerScreen(viewModel = viewModel(factory = factory), zipFilePath = null)
          }
        }
      }
    }
    composeRule.waitForIdle()
  }

  @Test
  fun helpDialogShowsAndNavigates() {
    // Tap the help button
    composeRule.onNodeWithTag(SamplerTestTags.HELP_BUTTON).performClick()

    // Dialog should be visible
    composeRule.onNodeWithTag(SamplerTestTags.HELP_DIALOG).assertIsDisplayed()

    // Page indicator should exist and show initial page (dot 0 selected)
    composeRule.onNodeWithTag(SamplerTestTags.HELP_PAGE_INDICATOR).assertExists()

    // Tap the right nav button to go to next page
    composeRule.onNodeWithTag(SamplerTestTags.HELP_NAV_RIGHT).performClick()

    // Page indicator should now reflect page 1 selected — verify by ensuring the second dot is
    // clickable
    // We can't directly read which dot is selected, but the nav button state changes; check that
    // left nav becomes enabled
    composeRule.onNodeWithTag(SamplerTestTags.HELP_NAV_LEFT).assertIsEnabled()

    // Navigate back left
    composeRule.onNodeWithTag(SamplerTestTags.HELP_NAV_LEFT).performClick()

    // Close the dialog
    composeRule.onNodeWithText("Close").performClick()

    // Dialog should be dismissed
    composeRule.onNodeWithTag(SamplerTestTags.HELP_DIALOG).assertDoesNotExist()
  }
}
