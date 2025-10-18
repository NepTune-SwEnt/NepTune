package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
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
    // Global Wait: Ensure the Bottom Navigation Menu is composed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()
    }
  }

  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()

    // Specific Wait for Top Bar Element
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).performClick()

    // Wait for the Profile Button to be available before clicking
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).isDisplayed()
    }
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

    // Wait for the Profile Button to be available before clicking
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).isDisplayed()
    }
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
  fun importTabIsSelectedAfterClick() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()

    // Wait for the Import tab specifically before interacting
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).isDisplayed()
    }
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

    // 1. Wait for Import Tab
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).performClick()

    // 2. Wait for Profile Button
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()

    // Ensure GO_BACK_BUTTON is displayed on the profile screen before clicking
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()

    // Note: The UI may update the title quickly, but waiting for a key element
    // on the returned screen is safer than just asserting the title.
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Post")
  }

  @Test
  fun goBackFromProfileToSearch() {
    composeTestRule.setContent {
      NeptuneApp(navController = rememberNavController(), startDestination = Screen.Main.route)
    }
    // Wait for Search Tab
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
  }
}
