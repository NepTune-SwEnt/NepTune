package com.neptune.neptune.ui.offline

import OfflineScreen
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// This tests were made using AI assistance.
@RunWith(RobolectricTestRunner::class)
class OfflineComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun offlineScreenDisplaysCorrectContent() {
    composeTestRule.setContent { OfflineScreen() }

    composeTestRule.onNodeWithText("No connection").assertIsDisplayed()

    composeTestRule
        .onNodeWithText(
            "You can't see the samples right now, but you can still create and modify your local projects.")
        .assertIsDisplayed()

    composeTestRule.onNodeWithContentDescription("Offline").assertIsDisplayed()
  }

  @Test
  fun offlineBannerDisplaysCorrectMessage() {
    composeTestRule.setContent { OfflineBanner() }

    composeTestRule.onNodeWithText("No connection. Showing offline data.").assertIsDisplayed()
  }
}
