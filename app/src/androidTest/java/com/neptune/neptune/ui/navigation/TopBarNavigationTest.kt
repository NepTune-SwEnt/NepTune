// Fichier : app/src/test/java/com/neptune/neptune/ui/navigation/TopBarNavigationTest.kt
package com.neptune.neptune.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class TopBarNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testScreen = Screen.Main

  @Test
  fun topBar_showsTitle() {
    composeTestRule.setContent {
      TopBar(currentScreen = testScreen, navigationActions = null, canNavigateBack = false)
    }
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextEquals(testScreen.name)
  }

  @Test
  fun topBar_showsGoBackButton_whenCanNavigateBack() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      TopBar(
          currentScreen = testScreen,
          navigationActions =
              object : NavigationActions(navController) {
                override fun goBack() {}

                override fun navigateTo(screen: Screen) {}
              },
          canNavigateBack = true)
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun topBar_showsProfileButton_whenCannotNavigateBack() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      TopBar(
          currentScreen = testScreen,
          navigationActions =
              object : NavigationActions(navController) {
                override fun goBack() {}

                override fun navigateTo(screen: Screen) {}
              },
          canNavigateBack = false)
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
  }
    @Test
    fun topBar_showsTitle_andProfileButton_whenCannotNavigateBack() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            TopBar(
                currentScreen = Screen.Profile,
                navigationActions = object : NavigationActions(navController) {
                    override fun goBack() {}
                    override fun navigateTo(screen: Screen) {}
                },
                canNavigateBack = false
            )
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
    }
}
