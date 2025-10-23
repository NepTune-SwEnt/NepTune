package com.neptune.neptune.ui.settings

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.platform.app.InstrumentationRegistry
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Top-level delegate to access DataStore in the test environment.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_test")

/**
 * UI tests for the [SettingsThemeScreen]. These tests use a real ViewModel and DataStore
 * to verify UI state and data persistence.
 * The tests where made with AI assistance.
 */
class SettingsThemeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var themeDataStore: ThemeDataStore
  private lateinit var settingsViewModel: SettingsViewModel
  private var goBackCalled = false

  @Before
  fun setup() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    // Use a test-specific DataStore
    themeDataStore = ThemeDataStore(context)

    // Clear DataStore before each test to ensure isolation
    runBlocking {
      context.dataStore.edit { it.clear() }
    }

    settingsViewModel = SettingsViewModel(themeDataStore)
    goBackCalled = false

    composeTestRule.setContent {
      SampleAppTheme(themeSetting = settingsViewModel.theme.collectAsState().value) {
        SettingsThemeScreen(settingsViewModel = settingsViewModel, goBack = { goBackCalled = true })
      }
    }
  }

  @Test
  fun themeScreenDisplaysTitleAndOptions() {
    composeTestRule.onNodeWithText("Theme").assertExists()
    composeTestRule.onNodeWithText("System Default").assertExists()
    composeTestRule.onNodeWithText("Light").assertExists()
    composeTestRule.onNodeWithText("Dark").assertExists()
  }

  @Test
  fun clickingBackButtonCallsGoBackLambda() {
    composeTestRule.onNodeWithContentDescription("Go Back").performClick()
    assertTrue("goBack lambda was not called", goBackCalled)
  }

  @Test
  fun selectingNewThemeUpdatesUIAndPersistsState() = runBlocking {
    // Act: Click on the "Dark" theme option
    composeTestRule.onNodeWithText("Dark").performClick()

    // Assert: UI is updated, and the "Dark" radio button is selected
    composeTestRule.onNodeWithText("Dark").assertIsSelected()

    // Assert: The change is persisted in the DataStore
    val savedTheme = themeDataStore.theme.first()
    assertEquals(ThemeSetting.DARK, savedTheme)
  }

  @Test
  fun selectingNewThemeUpdatesUIAndPersistsStateLight() = runBlocking {
    // Act: Click on the "Light" theme option
    composeTestRule.onNodeWithText("Light").performClick()

    // Assert: UI is updated, and the "Light" radio button is selected
    composeTestRule.onNodeWithText("Light").assertIsSelected()

    // Assert: The change is persisted in the DataStore
    val savedTheme = themeDataStore.theme.first()
    assertEquals(ThemeSetting.LIGHT, savedTheme)
  }
}
