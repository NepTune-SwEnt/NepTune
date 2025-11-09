// Kotlin
package com.neptune.neptune.ui.projectlist

import android.util.Log
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryVar
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryCompose
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProjectListScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: ProjectListViewModel
  private lateinit var localRepository: ProjectItemsRepositoryVar
  private lateinit var cloudRepository: ProjectItemsRepositoryVar
  private val navCalls = mutableListOf<String>()

  @Before
  fun setUp() {
    navCalls.clear()

    localRepository = ProjectItemsRepositoryVar()
    cloudRepository = ProjectItemsRepositoryVar()
    runBlocking {
      // create three projects with controlled timestamps and favorite flag
      cloudRepository.addProject(
          ProjectItem(
              uid = "1",
              name = "Old",
              description = "Old desc",
              isFavorite = false,
              lastUpdated = Timestamp(1, 0)))
      cloudRepository.addProject(
          ProjectItem(
              uid = "2",
              name = "Fav",
              description = "Favorite item",
              isFavorite = true,
              lastUpdated = Timestamp(2, 0)))
      cloudRepository.addProject(
          ProjectItem(
              uid = "3",
              name = "New",
              description = "New item",
              isFavorite = false,
              lastUpdated = Timestamp(3, 0)))
    }

    viewModel =
        ProjectListViewModel(TotalProjectItemsRepositoryCompose(localRepository, cloudRepository))

    // Single setContent call per test lifecycle — inject navCalls collector here
    composeTestRule.setContent {
      ProjectListScreen(projectListViewModel = viewModel, navigateToSampler = { navCalls.add("2") })
    }
  }

  @Test
  fun openingProjectCallsNavigate() {
    // Click on project "Fav" card
    composeTestRule.onNodeWithTag("project_2").performClick()

    // Verify navigation was called with id "2"
    assertEquals(listOf("2"), navCalls)
  }

  @Test
  fun sortedListShowsFavoritesFirstAndByDate() {
    composeTestRule.waitForIdle()
    // After view model sorts: "Fav" should be first, then "New", then "Old"
    val nameNodes =
        composeTestRule.onAllNodesWithTag(
            ProjectListScreenTestTags.PROJECT_NAME, useUnmergedTree = true)
    nameNodes[0].assert(hasText("Fav"))
    nameNodes[1].assert(hasText("New"))
    nameNodes[2].assert(hasText("Old"))
  }

  @Test
  fun favoriteToggleUpdatesOrder() {
    composeTestRule.waitForIdle()
    // Initially Fav is first
    composeTestRule
        .onAllNodesWithTag(ProjectListScreenTestTags.PROJECT_NAME, useUnmergedTree = true)[0]
        .assertTextEquals("Fav")

    // Click favorite button to un-favorite project 2
    composeTestRule.onNodeWithTag("favorite_2").performClick()

    // Allow UI to update
    composeTestRule.waitForIdle()

    // Now "New" (most recent non-favorite) should be first
    composeTestRule
        .onAllNodesWithTag(ProjectListScreenTestTags.PROJECT_NAME, useUnmergedTree = true)[0]
        .assertTextEquals("New")
  }

  @Test
  fun renamingProjectUpdatesUi() {
    // Open menu for project 1 and choose Rename
    composeTestRule.onNodeWithTag("menu_1").performClick()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.RENAME_BUTTON).performClick()

    // Input new name and confirm
    composeTestRule.onNodeWithText("New name").performTextClearance()
    composeTestRule.onNodeWithText("New name").performTextInput("Old Renamed")
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()

    composeTestRule.waitForIdle()

    // Verify UI shows renamed project
    composeTestRule.onNodeWithText("Old Renamed").assertIsDisplayed()
  }

  @Test
  fun changeDescriptionUpdatesRepository() {
    val newDesc = "Updated description from test"

    // Open menu for project 3 and choose Change description
    composeTestRule.onNodeWithTag("menu_3").performClick()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.CHANGE_DESCRIPTION_BUTTON).performClick()

    // Input new description and confirm
    composeTestRule
        .onNodeWithTag(ProjectListScreenTestTags.DESCRIPTION_TEXT_FIELD)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(ProjectListScreenTestTags.DESCRIPTION_TEXT_FIELD)
        .performTextInput(newDesc)
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()

    composeTestRule.waitForIdle()

    // Verify repository has the updated description
    runBlocking {
      val p = cloudRepository.getProject("3")
      Log.i(
          "change_description_updates_repositoryTest",
          "Project after description change: ${p.description}")
      assertEquals(newDesc, p.description)
    }
  }

  @Test
  fun deleteProjectRemovesItemFromUi() {
    // Delete project 1
    composeTestRule.onNodeWithTag("menu_1").performClick()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.DELETE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.CONFIRM_DIALOG).performClick()


    composeTestRule.waitForIdle()

    // Project 1 should no longer exist in UI
    composeTestRule.onNodeWithText("Old").assertDoesNotExist()
  }

  @Test
  fun testTagsAreCorrect() {
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.PROJECT_LIST_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.PROJECT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProjectListScreenTestTags.SEARCH_TEXT_FIELD).assertIsDisplayed()
  }

  /** Tests that you can type on the search bar */
  @Test
  fun searchbarCanType() {
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
  fun searchbarHasDefaultText() {

    // Assert that the search icon exist
    composeTestRule.onNodeWithContentDescription("Search Icon").assertExists()

    composeTestRule.onNode(hasText("Search for a Project")).assertIsDisplayed()
  }
}
