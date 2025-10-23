package com.neptune.neptune.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.theme.SampleAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for the [SettingsScreen]. The tests where made with AI assistance. */
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Lambdas flags to verify navigation
  private var goBackCalled = false
  private var goThemeCalled = false
  private var goAccountCalled = false

  /** Sets up the composable under test with mocked navigation lambdas. */
  private fun setupScreen() {
    // Reset flags before each test
    goBackCalled = false
    goThemeCalled = false
    goAccountCalled = false

    composeTestRule.setContent {
      SampleAppTheme {
        SettingsScreen(
            goBack = { goBackCalled = true },
            goTheme = { goThemeCalled = true },
            goAccount = { goAccountCalled = true })
      }
    }
  }

  @Test
  fun settingsScreen_displaysTitleAndItems() {
    // Arrange
    setupScreen()

    // Assert
    composeTestRule.onNodeWithText("Settings").assertExists()
    composeTestRule.onNodeWithText("Theme").assertExists()
    composeTestRule.onNodeWithText("Account").assertExists()
  }

  @Test
  fun clickingBackButton_callsGoBackLambda() {
    // Arrange
    setupScreen()

    // Act
    composeTestRule.onNodeWithContentDescription("Go Back").performClick()

    // Assert
    assertTrue("goBack lambda was not called", goBackCalled)
  }

  @Test
  fun clickingThemeCard_callsGoThemeLambda() {
    // Arrange
    setupScreen()

    // Act
    composeTestRule.onNodeWithText("Theme").performClick()

    // Assert
    assertTrue("goTheme lambda was not called", goThemeCalled)
  }

  @Test
  fun clickingAccountCard_callsGoAccountLambda() {
    // Arrange
    setupScreen()

    // Act
    composeTestRule.onNodeWithText("Account").performClick()

    // Assert
    assertTrue("goAccount lambda was not called", goAccountCalled)
  }
}
