package com.neptune.neptune.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.firebase.Timestamp
import com.neptune.neptune.model.sample.Comment
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CommentSectionTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun profilePictureInCommentIsClickableAndNavigates() {
    val onProfileClickedMock: (String) -> Unit = mock()
    val sampleId = "sample1"
    val comments =
        listOf(
            Comment(
                authorId = "user123",
                authorName = "John Doe",
                text = "Great sample!",
                timestamp = Timestamp.now(),
                authorProfilePicUrl = "https://example.com/profile.jpg"))

    composeTestRule.setContent {
      CommentDialog(
          sampleId = sampleId,
          comments = comments,
          usernames = mapOf("user123" to "John Doe"),
          onDismiss = {},
          commentDialogAction =
              CommentDialogAction(
                  onAddComment = { _, _ -> }, onProfileClicked = onProfileClickedMock))
    }

    composeTestRule.onNodeWithText("John Doe:").assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_PICTURE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_PICTURE).performClick()

    verify(onProfileClickedMock).invoke("user123")
  }
}
