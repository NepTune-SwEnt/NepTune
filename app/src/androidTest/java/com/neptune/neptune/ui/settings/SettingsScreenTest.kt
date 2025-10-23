package com.neptune.neptune.ui.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neptune.neptune.ui.theme.SampleAppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** UI tests for the [SettingsScreen]. The tests where made with AI assistance. */
class SettingsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Mock dependencies
  private lateinit var viewModel: SettingsViewModel
  private val themeStateFlow = MutableStateFlow(ThemeSetting.SYSTEM)
  private var goBackCalled = false

  /**
   * Sets up the composable under test. We mock the SettingsViewModel and control its theme
   * StateFlow. We wrap the screen in [SampleAppTheme] because it relies on [NepTuneTheme.colors].
   */
  private fun setupScreen() {
    // Reset goBack flag for each test
    goBackCalled = false

    // Mock the ViewModel
    viewModel =
        mockk(relaxed = true) {
          // "every { theme }" returns our controllable state flow
          every { theme } returns themeStateFlow
        }

    // Set the content for the test
    composeTestRule.setContent {
      SampleAppTheme {
        SettingsScreen(settingsViewModel = viewModel, goBack = { goBackCalled = true })
      }
    }
  }

  @Test
  fun settingsScreenDisplaysTitleAndAllThemeOptions() {
    // Arrange: Set initial state and load the screen
    themeStateFlow.value = ThemeSetting.SYSTEM
    setupScreen()

    // Assert: Check that all UI elements are displayed
    composeTestRule.onNodeWithText("Theme").assertExists()
    composeTestRule.onNodeWithText("System Default").assertExists()
    composeTestRule.onNodeWithText("Light").assertExists()
    composeTestRule.onNodeWithText("Dark").assertExists()
  }

  @Test
  fun settingsScreenSystemThemeIsSelectedWhenStateIsSystem() {
    // Arrange: Set state to SYSTEM
    themeStateFlow.value = ThemeSetting.SYSTEM
    setupScreen()

    // Act & Assert: Find the selectable node that contains "System Default"
    composeTestRule.onNodeWithText("System Default").assertIsSelected()
  }

  @Test
  fun settingsScreenLightThemeIsSelectedWhenStateIsLight() {
    // Arrange: Set state to LIGHT
    themeStateFlow.value = ThemeSetting.LIGHT
    setupScreen()

    // Act & Assert: Find the selectable node that contains "Light"
    composeTestRule.onNodeWithText("Light").assertIsSelected()
  }

  @Test
  fun settingsScreenDarkThemeIsSelectedWhenStateIsDark() {
    // Arrange: Set state to DARK
    themeStateFlow.value = ThemeSetting.DARK
    setupScreen()

    // Act & Assert: Find the selectable node that contains "Dark"
    composeTestRule.onNodeWithText("Dark").assertIsSelected()
  }

  @Test
  fun settingsScreenClickingLightOptionCallsViewModel() {
    // Arrange: Start with SYSTEM theme
    themeStateFlow.value = ThemeSetting.SYSTEM
    setupScreen()

    // Act: Click on the "Light" option
    composeTestRule.onNodeWithText("Light").performClick()

    // Assert: Verify the ViewModel's updateTheme function was called with LIGHT
    verify { viewModel.updateTheme(ThemeSetting.LIGHT) }
  }

  @Test
  fun settingsScreenClickingDarkOptionCallsViewModel() {
    // Arrange: Start with LIGHT theme
    themeStateFlow.value = ThemeSetting.LIGHT
    setupScreen()

    // Act: Click on the "Dark" option
    composeTestRule.onNodeWithText("Dark").performClick()

    // Assert: Verify the ViewModel's updateTheme function was called with DARK
    verify { viewModel.updateTheme(ThemeSetting.DARK) }
  }

  @Test
  fun settingsScreen_clickingSystemOptionCallsViewModel() {
    // Arrange: Start with DARK theme
    themeStateFlow.value = ThemeSetting.DARK
    setupScreen()

    // Act: Click on the "System Default" option
    composeTestRule.onNodeWithText("System Default").performClick()

    // Assert: Verify the ViewModel's updateTheme function was called with SYSTEM
    verify { viewModel.updateTheme(ThemeSetting.SYSTEM) }
  }

  @Test
  fun settingsScreen_clickingBackButtonTriggersGoBackLambda() {
    // Arrange
    setupScreen()

    // Act: Click the back button (identified by its content description)
    composeTestRule.onNodeWithContentDescription("Go Back").performClick()

    // Assert: Verify our goBackCalled flag was set to true
    assertTrue("The goBack lambda was not called.", goBackCalled)
  }
}
