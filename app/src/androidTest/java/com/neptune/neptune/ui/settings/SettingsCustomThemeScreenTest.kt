package com.neptune.neptune.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neptune.neptune.ui.theme.SampleAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsCustomThemeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var themeDataStore: ThemeDataStore
  private lateinit var settingsViewModel: SettingsViewModel

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    themeDataStore = ThemeDataStore(context)
    settingsViewModel = SettingsViewModel(themeDataStore)

    runBlocking {
      themeDataStore.resetCustomColors()
      themeDataStore.setTheme(ThemeSetting.CUSTOM)
    }
  }

  @Test
  fun testApplyCustomTheme() {
    composeTestRule.setContent {
      SampleAppTheme(themeSetting = ThemeSetting.CUSTOM) {
        SettingsCustomThemeScreen(settingsViewModel = settingsViewModel)
      }
    }

    composeTestRule
        .onNodeWithTag(CustomThemeScreenTestTags.COLOR_PREVIEW_BOX_PRIMARY)
        .performClick()

    composeTestRule.onNodeWithTag(CustomThemeScreenTestTags.APPLY_BUTTON).performClick()

    runBlocking {
      val primary = themeDataStore.customPrimaryColor.first()
      assertEquals(ThemeDataStore.DEFAULT_PRIMARY_COLOR, primary)
    }
  }

  @Test
  fun testContrastWarning() {
    composeTestRule.setContent {
      SampleAppTheme(themeSetting = ThemeSetting.CUSTOM) {
        SettingsCustomThemeScreen(settingsViewModel = settingsViewModel)
      }
    }

    // Set colors that will trigger a contrast warning.
    // This is async, so we need to wait for the change to apply.
    settingsViewModel.updateCustomColors(Color.Black, Color.Black, Color.White)

    // Wait until the view model/data store has processed the update
    composeTestRule.waitUntil(5000) {
      runBlocking { themeDataStore.customPrimaryColor.first() == Color.Black }
    }

    composeTestRule.onNodeWithTag(CustomThemeScreenTestTags.APPLY_BUTTON).performClick()

    composeTestRule.onNodeWithTag(CustomThemeScreenTestTags.CONTRAST_WARNING_DIALOG).assertExists()
  }

  @Test
  fun testResetTheme() {
    composeTestRule.setContent {
      SampleAppTheme(themeSetting = ThemeSetting.CUSTOM) {
        SettingsCustomThemeScreen(settingsViewModel = settingsViewModel)
      }
    }

    // Change the color to something custom
    settingsViewModel.updateCustomColors(Color.Red, Color.Green, Color.Blue)
    composeTestRule.waitUntil(5000) {
      runBlocking { themeDataStore.customPrimaryColor.first() == Color.Red }
    }

    // Click the reset button
    composeTestRule.onNodeWithTag(CustomThemeScreenTestTags.RESET_BUTTON).performClick()

    // The reset operation is also async, so wait for it to complete.
    composeTestRule.waitUntil(5000) {
      runBlocking { themeDataStore.customPrimaryColor.first() != Color.Red }
    }

    val resetPrimary = runBlocking { themeDataStore.customPrimaryColor.first() }
    assertNotEquals(Color.Red, resetPrimary)
  }
}
