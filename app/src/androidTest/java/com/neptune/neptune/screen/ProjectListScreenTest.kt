package com.neptune.neptune.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.ui.navigation.Screen
import com.neptune.neptune.ui.project.ProjectListScreenTestTags
import com.neptune.neptune.ui.project.ProjectListViewModel
import com.neptune.neptune.ui.sampler.SamplerTestTags
import org.junit.Rule
import org.junit.Test

class ProjectListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(viewModel: ProjectListViewModel = ProjectListViewModel()) {
    composeTestRule.setContent { NeptuneApp(startDestination = Screen.ProjectList.route) }
  }

  /** Tests that the initial UI displays all elements */
  @Test
  fun testTagsAreCorrect() {
    setContent()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN).assertIsDisplayed()
    composeTestRule
        .onAllNodesWithTag(ProjectListScreenTestTags.PROJECT_CARD)
        .onFirst()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.PROJECT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }

  /** Tests that clicking on a project navigate to the sampler Screen */
  @Test
  fun projectListScreen_canSelectProjectAndNavigate() {
    setContent()

    val firstCard =
        composeTestRule.onAllNodesWithTag(ProjectListScreenTestTags.PROJECT_CARD).onFirst()

    firstCard.assertIsDisplayed().assertHasClickAction()
    firstCard.performClick()

    // Should navigate to the Sampler Screen
    composeTestRule.onNodeWithTag(SamplerTestTags.SCREEN_CONTAINER).assertIsDisplayed()
  }

  /** Tests that you can type on the search bar */
  @Test
  fun searchBar_canType() {
    setContent()

    // Simulate typing
    composeTestRule
        .onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD)
        .performTextInput("Test")

    composeTestRule
        .onNode(hasText("Test"))
        .assertExists("Text input wasn't updated in the search field.")
  }

  /** Tests that the search icon exists and that the topBar has the right default text */
  @Test
  fun searchBar_hasDefaultText() {
    setContent()

    // Assert that the search icon exist
    composeTestRule.onNodeWithContentDescription("Search Icon").assertExists()

    composeTestRule.onNode(hasText("Search for a Project")).assertIsDisplayed()
  }
}
