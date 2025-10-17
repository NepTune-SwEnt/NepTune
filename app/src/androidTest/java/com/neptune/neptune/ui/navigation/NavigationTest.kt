package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
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
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsNotDisplayed()
  }

  @Test
  fun navigationToEditTabShowsEditScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Project List")
  }

  @Test
  fun navigationToMainTabShowsMainScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Neptune")
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("My Profile")
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun goBackFromProfileToMain() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Neptune")
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
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsNotSelected()
  }

  @Test
  fun editTabIsSelectedAfterClick() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun mainTabIsSelectedAfterNavigatingBackFromEdit() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsNotSelected()
  }

  @Test
  fun navigationToSearchTabShowsSearchScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Search")
  }

  @Test
  fun importTabIsSelectedAfterClick() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
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
  fun goBackFromProfileToPost() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Post")
  }

  @Test
  fun goBackFromProfileToSearch() {
    composeTestRule.setContent {
      NeptuneApp(navController = rememberNavController(), startDestination = Screen.Main.route)
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Search")
  }
}
