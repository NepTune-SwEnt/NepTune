package com.neptune.neptune.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.ui.picker.ProjectList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Object containing all test tags used in the UI for centralized management. */
object ProjectListTestTags {
  const val PROJECT_LIST = "project_list"
}

/** Object containing all non-code-generated text strings used for test assertions. */
object ProjectListTestTexts {
  // Shared text for URI structure verification
  const val URI_BASE_PATH = "file:///storage/projects/"
  const val URI_EXTENSION = ".zip"
}

/*
 * Basic tests for ProjectList composable.
 * Verifies that items are shown correctly, and that scrolling works.
 * Written partially with ChatGPT
 */
@RunWith(RobolectricTestRunner::class)
class ProjectListTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun showsFilenameAndFullUriForEachItem() {
    val items =
        listOf(
            MediaItem(id = "1", projectUri = ProjectListTestTexts.URI_BASE_PATH + "a-1.zip"),
            MediaItem(id = "2", projectUri = ProjectListTestTexts.URI_BASE_PATH + "b.zip"))

    compose.setContent { MaterialTheme { ProjectList(items) } }

    // Each row is one merged node; both filename + URI are present in that node's text.
    compose.onNodeWithText("a-1.zip").assertExists()
    compose.onNodeWithText(ProjectListTestTexts.URI_BASE_PATH + "a-1.zip").assertExists()
    compose.onNodeWithText("b.zip").assertExists()
    compose.onNodeWithText(ProjectListTestTexts.URI_BASE_PATH + "b.zip").assertExists()

    // Use the centralized extension constant for the check
    compose
        .onAllNodesWithText(ProjectListTestTexts.URI_EXTENSION, substring = true)
        .assertCountEquals(2)
  }

  @Test
  fun handlesManyItemsScrollingList() {
    val items =
        (1..30).map { i ->
          MediaItem(
              id = "$i",
              projectUri =
                  ProjectListTestTexts.URI_BASE_PATH + "p$i" + ProjectListTestTexts.URI_EXTENSION)
        }

    compose.setContent { MaterialTheme { ProjectList(items) } }

    val lastFileName = "p30" + ProjectListTestTexts.URI_EXTENSION
    val middleFullUri =
        ProjectListTestTexts.URI_BASE_PATH + "p15" + ProjectListTestTexts.URI_EXTENSION

    // Scroll the LazyColumn until the node with the last item is composed.
    compose
        .onNodeWithTag(ProjectListTestTags.PROJECT_LIST)
        .performScrollToNode(hasText(lastFileName))

    // Now it exists.
    compose.onNodeWithText(lastFileName).assertExists()

    // Also verify a middle full URI (scroll will bring it in if needed)
    compose
        .onNodeWithTag(ProjectListTestTags.PROJECT_LIST)
        .performScrollToNode(hasText(middleFullUri))
    compose.onNodeWithText(middleFullUri).assertExists()
  }
}
