package com.neptune.neptune.ui.navigation

import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class BottomNavigationMenuTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun bottomNavigationMenuAllTabsAreDisabledWhenNavigationActionsAreNull() {
    composeTestRule.setContent { BottomNavigationMenu() }

    // Verify that both tabs are displayed but are not enabled (not clickable).
    composeTestRule
        .onNodeWithTag(NavigationTestTags.MAIN_TAB)
        .assertIsDisplayed()
        .assertIsNotEnabled()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB)
        .assertIsDisplayed()
        .assertIsNotEnabled()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.SEARCH_TAB)
        .assertIsDisplayed()
        .assertIsNotEnabled()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun bottomNavigationMenuDisplaysCorrectlyWithValidNavigationActions() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      BottomNavigationMenu(screen = Screen.ProjectList, navigationActions = navigationActions)
    }

    // Verify that the menu and all its items are displayed.
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsDisplayed()
  }

  @Test
  fun bottomNavigationMenuMainTabIsSelectedWhenScreenIsMain() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      // Set the current screen to Main
      BottomNavigationMenu(screen = Screen.Main, navigationActions = navigationActions)
    }

    // Assert that the Main tab is marked as selected
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    // Assert that the other tab are NOT selected
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomNavigationMenuSearchTabIsSelectedWhenScreenIsSearchScreen() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      // Set the current screen to Search Screen
      BottomNavigationMenu(screen = Screen.Search, navigationActions = navigationActions)
    }

    // Assert that the Search tab is marked as selected
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsSelected()

    // Assert that the other tab are NOT selected
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomNavigationMenuImportTabIsSelectedWhenScreenIsImport() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      // Set the current screen to Import
      BottomNavigationMenu(screen = Screen.ImportFile, navigationActions = navigationActions)
    }

    // Assert that the Import tab is marked as selected
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsSelected()

    // Assert that the other tab are NOT selected
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomNavigationMenuProjectTabIsSelectedWhenScreenIsProjectList() {
    composeTestRule.setContent {
      val navController = rememberNavController()
      val navigationActions = NavigationActions(navController)
      // Set the current screen to ProjectList
      BottomNavigationMenu(screen = Screen.ProjectList, navigationActions = navigationActions)
    }

    // Assert that the Project tab is marked as selected
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    // Assert that the other tab are NOT selected
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsNotSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomNavigationMenuIsHiddenWhenScreenShouldNotShowIt() {
    composeTestRule.setContent {
      // Use the Profile screen, which should hide the bottom bar.
      BottomNavigationMenu(screen = Screen.Profile, navigationActions = null)
    }

    // Assert that the bottom navigation menu does not exist in the UI tree.
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertDoesNotExist()
  }

  @Test
  fun bottomNavigationMenuClickOnTabNavigatesToCorrectScreen() {
    var capturedRoute: String? = null
    var fakeNavigationActions: NavigationActions

    composeTestRule.setContent {
      val navController = rememberNavController()
      fakeNavigationActions =
          object : NavigationActions(navController) {
            override fun navigateTo(screen: Screen) {
              capturedRoute = screen.route
            }

            override fun navigateTo(route: String) {
              capturedRoute = route
            }
          }
      BottomNavigationMenu(screen = Screen.Main, navigationActions = fakeNavigationActions)
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()
    assert(capturedRoute == "project_list/edit")
  }

  @Test
  fun bottomNavigationMenuReturnsNullForUnknownRoute() {
    val unknownTab = getTabForRoute("unknown_route")
    assert(unknownTab == null)
  }
}
