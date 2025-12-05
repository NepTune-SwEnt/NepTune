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
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.screen.FakeSamplerViewModel
import com.neptune.neptune.screen.SamplerViewModelFactory
import com.neptune.neptune.ui.settings.SettingsScreenTestTags.DISABLE_HELP_SWITCH
import com.neptune.neptune.ui.settings.ThemeDataStore
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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

  @Test
  fun helpDialogWhenDisable() {
    // When the datastore flag 'disableHelp' is true, the help FAB/button should not appear.
    val dataStore = ThemeDataStore(NepTuneApplication.appContext)
    try {
      runBlocking { dataStore.setDisableHelp(true) }

      // Wait for the UI to observe the change and recompose
      composeRule.waitForIdle()

      // Help button must not be present
      composeRule.onNodeWithTag(SamplerTestTags.HELP_BUTTON).assertDoesNotExist()
    } finally {
      // Reset to default to avoid polluting other tests
      runBlocking { dataStore.setDisableHelp(false) }
    }
  }

  @Test
  fun disableHelpSettingCorrect() {
    // Verify that toggling the setting in the Settings screen updates the datastore value.
    val dataStore = ThemeDataStore(NepTuneApplication.appContext)

    // Ensure starting from a known state
    runBlocking { dataStore.setDisableHelp(false) }

    // Show the Settings screen directly so we can interact with the disable-help switch/card
    composeRule.activity.setContent {
      SampleAppTheme {
        // Use the SettingsScreen composable from the app
        com.neptune.neptune.ui.settings.SettingsScreen()
      }
    }
    composeRule.waitForIdle()

    // Tap the setting card (has the test tag) which toggles the setting
    composeRule.onNodeWithTag(DISABLE_HELP_SWITCH).performClick()

    // Wait for the coroutine write to DataStore to complete
    composeRule.waitForIdle()

    // Read back the value and assert it is now true
    val disabledValue = runBlocking { dataStore.disableHelp.first() }
    assertTrue(disabledValue)

    // Clean up
    runBlocking { dataStore.setDisableHelp(false) }
  }
}
