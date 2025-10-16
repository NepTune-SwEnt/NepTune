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

/*
    * Basic tests for ProjectList composable.
    * Verifies that items are shown correctly, and that scrolling works.
    * Written partially with ChatGPT
 */
@RunWith(RobolectricTestRunner::class)
class ProjectListTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun shows_filename_and_full_uri_for_each_item() {
    val items =
        listOf(
            MediaItem(id = "1", projectUri = "file:///storage/projects/a-1.zip"),
            MediaItem(id = "2", projectUri = "file:///storage/projects/b.zip"))

    compose.setContent { MaterialTheme { ProjectList(items) } }

    // Each row is one merged node; both filename + URI are present in that node's text.
    compose.onNodeWithText("a-1.zip").assertExists()
    compose.onNodeWithText("file:///storage/projects/a-1.zip").assertExists()
    compose.onNodeWithText("b.zip").assertExists()
    compose.onNodeWithText("file:///storage/projects/b.zip").assertExists()

    // Because of merged semantics, we have 2 rows -> 2 nodes containing ".zip"
    compose.onAllNodesWithText(".zip", substring = true).assertCountEquals(2)
  }

  @Test
  fun handles_many_items_scrolling_list() {
    val items =
        (1..30).map { i -> MediaItem(id = "$i", projectUri = "file:///storage/projects/p$i.zip") }

    compose.setContent { MaterialTheme { ProjectList(items) } }

    // Scroll the LazyColumn until the node with "p30.zip" is composed.
    compose.onNodeWithTag("project_list").performScrollToNode(hasText("p30.zip"))

    // Now it exists.
    compose.onNodeWithText("p30.zip").assertExists()

    // Also verify a middle full URI (scroll will bring it in if needed)
    compose
        .onNodeWithTag("project_list")
        .performScrollToNode(hasText("file:///storage/projects/p15.zip"))
    compose.onNodeWithText("file:///storage/projects/p15.zip").assertExists()
  }
}
