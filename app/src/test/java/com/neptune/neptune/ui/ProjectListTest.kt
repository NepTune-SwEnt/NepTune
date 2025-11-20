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
        .assertCountEquals(4) // *2 because of the way they are composed
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

  @Test
  fun showsEmptyList_whenNoItems() {
    compose.setContent { MaterialTheme { ProjectList(emptyList()) } }

    // The LazyColumn exists…
    compose.onNodeWithTag(ProjectListTestTags.PROJECT_LIST).assertExists()

    // …but no rows (filenames) should be found.
    compose
        .onAllNodesWithText(ProjectListTestTexts.URI_EXTENSION, substring = true)
        .assertCountEquals(0)
  }

  @Test
  fun headlineUsesWholeString_whenNoSlashInUri() {
    // substringAfterLast('/') should return the whole string when there is no '/'
    val bare = "bare-name.zip"
    val item = MediaItem(id = "x", projectUri = bare)

    compose.setContent { MaterialTheme { ProjectList(listOf(item)) } }

    // Headline shows the filename (the whole string in this case)
    compose
        .onAllNodes(hasText(bare))
        .assertCountEquals(2) // *2 because of the way they are composed
    // Supporting content shows the full "URI" we passed (same string)
    compose
        .onAllNodes(hasText(bare))
        .assertCountEquals(2) // *2 because of the way they are composed
  }

  @Test
  fun headlineBecomesEmpty_whenUriEndsWithSlash_butUriStillShown() {
    // Edge case: substringAfterLast('/') == "" if the URI ends with '/'
    val base = ProjectListTestTexts.URI_BASE_PATH + "folder/"
    val item = MediaItem(id = "x", projectUri = base)

    compose.setContent { MaterialTheme { ProjectList(listOf(item)) } }

    // Supporting content (full URI) should still be present
    compose.onNodeWithText(base).assertExists()

    // Headline text is empty string. Compose won't surface an empty Text node for querying,
    // so we assert that no ".zip" filename nodes exist (i.e., we did NOT falsely derive one).
    compose
        .onAllNodesWithText(ProjectListTestTexts.URI_EXTENSION, substring = true)
        .assertCountEquals(0)
  }
}
