package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import com.neptune.neptune.NeptuneApp
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // ---------- Helpers ----------

  /** Find a node by tag in the unmerged tree (more reliable on CI). */
  private fun node(tag: String): SemanticsNodeInteraction =
      composeRule.onNodeWithTag(tag, useUnmergedTree = true)

  /** Wait until at least one node with [tag] exists (no assertions inside the wait). */
  private fun waitForTag(tag: String, timeoutMs: Long = 10_000) {
    composeRule.waitUntil(timeoutMs) {
      composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /** Set content and wait for the bottom nav to appear. */
  private fun setContent() {
    composeRule.setContent {
      // If you have a "testMode" or fake DI entrypoint, pass it here.
      NeptuneApp(startDestination = Screen.Main.route)
    }
    waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    node(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Before
  fun before() {
    // Keeps animations/timers from causing idle flakiness.
    composeRule.mainClock.autoAdvance = true
  }

  @After
  fun dumpTreeOnCI() {
    // Handy when CI can’t find a node—check the run log.
    composeRule.onRoot(useUnmergedTree = true).printToLog("ComposeTree")
  }

  // ---------- Tests ----------

  @Test
  fun testTagsAreCorrect() {
    setContent()

    node(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    node(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()

    waitForTag(NavigationTestTags.PROFILE_BUTTON)
    node(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun mainTabIsSelectedByDefault() {
    setContent()
    node(NavigationTestTags.MAIN_TAB).assertIsSelected()
    node(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun editTabIsSelectedAfterClick() {
    setContent()

    node(NavigationTestTags.PROJECTLIST_TAB).performScrollTo().performClick()

    composeRule.waitForIdle()

    node(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()
    node(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun mainTabIsSelectedAfterNavigatingBackFromEdit() {
    setContent()

    node(NavigationTestTags.PROJECTLIST_TAB).performScrollTo().performClick()

    node(NavigationTestTags.PROJECTLIST_TAB).assertIsSelected()

    node(NavigationTestTags.MAIN_TAB).performScrollTo().performClick()

    node(NavigationTestTags.MAIN_TAB).assertIsSelected()
    node(NavigationTestTags.PROJECTLIST_TAB).assertIsNotSelected()
  }

  @Test
  fun navigationToSearchTabShowsSearchScreen() {
    setContent()
    waitForTag(NavigationTestTags.SEARCH_TAB)

    node(NavigationTestTags.SEARCH_TAB).performScrollTo().performClick()

    node(NavigationTestTags.SEARCH_TAB).assertIsSelected()
  }

  @Test
  fun importTabIsSelectedAfterClick() {
    setContent()

    node(NavigationTestTags.MAIN_TAB).assertIsSelected()

    waitForTag(NavigationTestTags.IMPORT_FILE)
    node(NavigationTestTags.IMPORT_FILE).performScrollTo().performClick()

    composeRule.waitForIdle()

    node(NavigationTestTags.IMPORT_FILE).assertIsSelected()
    node(NavigationTestTags.MAIN_TAB).assertIsNotSelected()
  }

  @Test
  fun profileButtonNavigatesToProfileScreen() {
    setContent()

    node(NavigationTestTags.MAIN_TAB).performClick()

    waitForTag(NavigationTestTags.PROFILE_BUTTON)
    node(NavigationTestTags.PROFILE_BUTTON).performScrollTo().performClick()

    // On profile screen the bottom bar is hidden.
    waitForTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)
    node(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsNotDisplayed()
  }

  @Test
  fun goBackFromProfileToPost() {
    setContent()

    waitForTag(NavigationTestTags.IMPORT_FILE)
    node(NavigationTestTags.IMPORT_FILE).performScrollTo().performClick()

    waitForTag(NavigationTestTags.PROFILE_BUTTON)
    node(NavigationTestTags.PROFILE_BUTTON).performScrollTo().performClick()

    waitForTag(NavigationTestTags.GO_BACK_BUTTON)
    node(NavigationTestTags.GO_BACK_BUTTON).performScrollTo().performClick()

    // Verify we navigated back by asserting the top-bar title.
    waitForTag(NavigationTestTags.TOP_BAR_TITLE)
    node(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Post")
  }
}
