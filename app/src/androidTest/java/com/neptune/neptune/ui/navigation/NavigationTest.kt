package com.neptune.neptune.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.model.FakeProfileRepository
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.main.MainScreenTestTags
import com.neptune.neptune.ui.main.MainViewModel
import com.neptune.neptune.ui.post.PostScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTagsPerSampleCard
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private fun setContent(mainViewModel: MainViewModel = MainViewModel()) {
    composeTestRule.setContent { NeptuneApp(startDestination = Screen.Main.route) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).isDisplayed()
  }

  private lateinit var previousRepo: ProfileRepository

  @Before
  fun setUp() {
    previousRepo = ProfileRepositoryProvider.repository
    ProfileRepositoryProvider.repository = FakeProfileRepository(initial = null)
  }

  @After
  fun tearDown() {
    ProfileRepositoryProvider.repository = previousRepo
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
  fun otherUsersProfileScreenFromSearchTab() {
    val testTags = SearchScreenTestTagsPerSampleCard(1)
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(testTags.SAMPLE_PROFILE_ICON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.OTHER_USER).assertIsDisplayed()
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

  @Test
  fun postButtonNavigateToPostScreen() {
    setContent()
    composeTestRule.onNodeWithTag(MainScreenTestTags.POST_BUTTON).performClick()
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_SCREEN).assertIsDisplayed()
  }

  @Test
  fun postButtonNavigateToMainScreen() {
    composeTestRule.setContent {
      NeptuneApp(navController = rememberNavController(), startDestination = Screen.Post.route)
    }
    composeTestRule
        .onNodeWithTag(PostScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Sweetie Banana")
    composeTestRule.onNodeWithTag(PostScreenTestTags.POST_BUTTON).performScrollTo().performClick()
    composeTestRule.onNodeWithTag(MainScreenTestTags.MAIN_SCREEN).assertIsDisplayed()
  }

  /** Test that the main screen has a bottom bar */
  @Test
  fun mainScreen_displaysBottomNav() {
    setContent()
    composeTestRule.onNodeWithTag(MainScreenTestTags.MAIN_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
  }

  /** Test that the bottom bar has all the button displayed */
  @Test
  fun mainScreen_bottomNavigationBar_hasAllButton() {
    setContent()
    // Original order: MAIN, SEARCH, PROJECTLIST, POST (now IMPORT_FILE)
    composeTestRule.onNodeWithTag(NavigationTestTags.MAIN_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROJECTLIST_TAB).assertIsDisplayed()
    // The changed/new tab
    composeTestRule.onNodeWithTag(NavigationTestTags.IMPORT_FILE).assertIsDisplayed()
  }

  /** Test that we can click on all of the bottom bar buttons */
  @Test
  fun mainScreen_bottomNavigationBar_canClickAllButtons() {
    setContent()
    listOf(
            NavigationTestTags.MAIN_TAB,
            NavigationTestTags.SEARCH_TAB,
            NavigationTestTags.PROJECTLIST_TAB, // Retained original position (3rd)
            NavigationTestTags.IMPORT_FILE) // Replaces POST_TAB (4th)
        .forEach { tag -> composeTestRule.onNodeWithTag(tag).assertHasClickAction().performClick() }
  }
}
