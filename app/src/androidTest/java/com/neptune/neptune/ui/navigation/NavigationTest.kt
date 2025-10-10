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
import org.junit.Rule
import org.junit.Test

class NavigationTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun testTagsAreCorrect() {
    composeTestRule.setContent { NeptuneApp() }
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
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Edit")
  }

  @Test
  fun navigationToMainTabShowsMainScreen() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Neptune")
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("My Profile")
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun goBackFromProfileToMain() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Neptune")
  }

  @Test
  fun goBackFromProfileToEdit() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Edit")
  }

  @Test
  fun bottomBarIsHiddenOnProfileScreen() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun mainTabIsSelectedByDefault() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsNotSelected()
  }

  @Test
  fun editTabIsSelectedAfterClick() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun mainTabIsSelectedAfterNavigatingBackFromEdit() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_TAB).assertIsNotSelected()
  }

  @Test
  fun navigationToPostTabShowsPostScreen() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.POST_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Post")
  }

  @Test
  fun navigationToSearchTabShowsSearchScreen() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Search")
  }

  @Test
  fun postTabIsSelectedAfterClick() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.POST_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.POST_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun searchTabIsSelectedAfterClick() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun goBackFromProfileToPost() {
    composeTestRule.setContent { NeptuneApp() }
    composeTestRule.onNodeWithTag(NavigationTestTags.POST_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Post")
  }

  @Test
  fun goBackFromProfileToSearch() {
    composeTestRule.setContent { NeptuneApp(navController = rememberNavController()) }
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Search")
  }
}
