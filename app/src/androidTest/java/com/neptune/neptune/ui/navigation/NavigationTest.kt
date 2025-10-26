package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.ui.main.MainViewModel
import org.junit.Rule
import org.junit.Test

class NavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun setContent(mainViewModel: MainViewModel = MainViewModel()) {
    composeTestRule.setContent { NeptuneApp(startDestination = Screen.Main.route) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
  }

  @Test
  fun goBackFromProfileToMain() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
  }

  @Test
  fun bottomBarIsHiddenOnProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun mainTabIsSelectedByDefault() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun bottomBarIsHiddenOnImportScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun editTabIsSelectedAfterClick() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun mainTabIsSelectedAfterNavigatingBackFromEdit() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun navigationToSearchTabShowsSearchScreen() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
  }

  @Test
  fun searchTabIsSelectedAfterClick() {

    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun goBackFromProfileToSearch() {

    composeTestRule.setContent {
      NeptuneApp(navController = rememberNavController(), startDestination = Screen.Main.route)
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
  }
}
