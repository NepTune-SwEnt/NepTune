package com.neptune.neptune.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.theme.SampleAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for the [SettingsAccountScreen]. The tests where made with AI assistance. */
class SettingsAccountScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private var goBackCalled = false
  private var logoutCalled = false

  private fun setupScreen() {
    goBackCalled = false
    logoutCalled = false

    composeTestRule.setContent {
      SampleAppTheme {
        SettingsAccountScreen(goBack = { goBackCalled = true }, logout = { logoutCalled = true })
      }
    }
  }

  @Test
  fun accountScreenDisplaysTitleAndLogoutButton() {
    // Arrange
    setupScreen()

    // Assert
    composeTestRule.onNodeWithText("Account").assertExists()
    composeTestRule.onNodeWithText("Log Out").assertExists()
  }

  @Test
  fun clickingBackButtonCallsGoBackLambda() {
    // Arrange
    setupScreen()

    // Act
    composeTestRule.onNodeWithContentDescription("Go Back").performClick()

    // Assert
    assertTrue("goBack lambda was not called", goBackCalled)
  }

  @Test
  fun clickingLogoutButtonCallsLogoutLambda() {
    // Arrange
    setupScreen()

    // Act
    composeTestRule.onNodeWithText("Log Out").performClick()

    // Assert
    assertTrue("logout lambda was not called", logoutCalled)
  }
}
