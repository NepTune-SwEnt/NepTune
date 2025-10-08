package com.android.sample.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.ui.main.MainScreen
import com.android.sample.ui.main.MainScreenTestTags
import com.android.sample.ui.main.MainViewModel
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(mainViewModel: MainViewModel = MainViewModel()) {
    composeTestRule.setContent { MainScreen(mainViewModel = mainViewModel) }
  }

  @Test
  fun mainScreen_displaysTopAppBarAndBottomNav() {
    setContent()

    composeTestRule.onNodeWithTag(MainScreenTestTags.MAIN_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.APP_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.BOTTOM_NAVIGATION_BAR).assertIsDisplayed()
  }

  @Test
  fun mainScreen_bottomNavigationBar_hasAllButton() {
    setContent()

    composeTestRule.onNodeWithTag(MainScreenTestTags.NAV_SAMPLER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.NAV_HOME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.NAV_SEARCH).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.NAV_NEW_POST).assertIsDisplayed()
  }

  @Test
  fun mainScreen_bottomNavigationBar_canClickAllButtons() {
    setContent()
    listOf(
            MainScreenTestTags.NAV_HOME,
            MainScreenTestTags.NAV_SEARCH,
            MainScreenTestTags.NAV_SAMPLER,
            MainScreenTestTags.NAV_NEW_POST)
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertHasClickAction().performClick() }
  }

  @Test
  fun mainScreen_topAppNavBar_canClickOnProfile() {
    setContent()
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.PROFILE_BUTTON)
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun discoverSection_displaysSample() {
    setContent()

    composeTestRule.onNodeWithText("Discover").assertIsDisplayed()
    // Check that at least one sample card is displayed
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).onFirst().assertIsDisplayed()
  }

  @Test
  fun sampleCard_displaysDetails() {
    setContent()

    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_PROFILE_ICON)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_USERNAME)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_NAME).onFirst().assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DURATION)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_TAGS).onFirst().assertIsDisplayed()
  }

  @Test
  fun sampleCard_displaysActions() {
    setContent()

    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_LIKES).onFirst().assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_COMMENTS)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(MainScreenTestTags.SAMPLE_DOWNLOADS)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun canScrollToLastSampleCard() {
    setContent()

    composeTestRule
        .onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)
        .performScrollToNode(hasTestTag(MainScreenTestTags.SAMPLE_CARD))

    // When scrolling the last card should be visible
    composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).onLast().assertIsDisplayed()
  }
}
