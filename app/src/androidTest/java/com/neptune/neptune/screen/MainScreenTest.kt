package com.neptune.neptune.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.R
import com.neptune.neptune.ui.main.IconWithText
import com.neptune.neptune.ui.main.IconWithTextPainter
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.navigation.Screen
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(mainViewModel: MainViewModel = MainViewModel()) {
    composeTestRule.setContent { NeptuneApp(startDestination = Screen.Main.route) }
  }

  @Test
  fun mainScreen_displaysBottomNav() {
    setContent()

    composeTestRule.onNodeWithTag(MainScreenTestTags.MAIN_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  @Test
  fun mainScreen_bottomNavigationBar_hasAllButton() {
    setContent()

    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
  }

  @Test
  fun mainScreen_bottomNavigationBar_canClickAllButtons() {
    setContent()
    listOf(
            NavigationTestTags.MAIN_TAB,
            NavigationTestTags.SEARCH_TAB,
            NavigationTestTags.IMPORT_FILE
            NavigationTestTags.PROJECTLIST_TAB)
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertHasClickAction().performClick() }
  }

  @Test
  fun mainScreen_topAppNavBar_canClickOnProfile() {
    setContent()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.PROFILE_BUTTON)
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
  fun followedSection_isDisplayed() {
    setContent()
    val lazyColumn = composeTestRule.onNodeWithTag(MainScreenTestTags.LAZY_COLUMN_SAMPLE_LIST)

    // Scroll to the Followed Section
    lazyColumn.performScrollToNode(hasText("Followed"))
    composeTestRule.onNodeWithText("Followed").assertIsDisplayed()

    val sampleCards =
        composeTestRule.onAllNodesWithTag(MainScreenTestTags.SAMPLE_CARD).fetchSemanticsNodes()
    assert(sampleCards.isNotEmpty()) {
      "At least one sample card should be in the Followed section"
    }
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

  @Test
  fun iconWithText_defaultModifier() {
    composeTestRule.setContent {
      IconWithText(Icons.Default.FavoriteBorder, "Like", "0") // uses default Modifier
    }
  }

  @Test
  fun iconWithTextPainter_defaultModifier() {
    composeTestRule.setContent {
      IconWithTextPainter(
          icon = painterResource(R.drawable.download), "Downloads", "0") // uses default Modifier
    }
  }
}
