package com.neptune.neptune.ui.sampler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpDialogTest {
  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun helpDialog_shows_and_navigates() {
    // Wait for sampler screen to appear
    composeRule.onNodeWithTag(SamplerTestTags.SCREEN_CONTAINER).assertExists()

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
  fun helpButton_hidden_when_disabled_in_settings() {
    // Open settings via FAB
    composeRule.onNodeWithTag(SamplerTestTags.SETTINGS_BUTTON).performClick()

    // Settings dialog should be visible
    composeRule.onNodeWithTag(SamplerTestTags.SETTINGS_DIALOG).assertExists()

    // Find the disable help row by semantics
    composeRule.onNode(hasContentDescription("disableHelpRow")).assertExists()

    // Toggle the switch: tap the first toggleable node we can find inside the settings dialog
    composeRule
        .onAllNodes(isToggleable())
        .filterToOne(hasParent(hasTestTag(SamplerTestTags.SETTINGS_DIALOG)))
        .performClick()

    // Close settings dialog by pressing Save & Close
    composeRule.onNodeWithTag(SamplerTestTags.SETTINGS_CONFIRM_BUTTON).performClick()

    // Now the help button should not exist
    composeRule.onNodeWithTag(SamplerTestTags.HELP_BUTTON).assertDoesNotExist()
  }
}
