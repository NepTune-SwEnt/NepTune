package com.neptune.neptune.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.neptune.neptune.ui.messages.MessagesScreen
import com.neptune.neptune.ui.messages.MessagesScreenTestTags
import com.neptune.neptune.ui.messages.MessagesViewModel
import com.neptune.neptune.ui.messages.SelectMessagesScreenTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for the MessagesScreen.This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class MessagesScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(
      uid: String = "123",
      goBack: () -> Unit = {},
  ) {
    composeTestRule.setContent { MessagesScreen(uid = uid, goBack = goBack) }
  }

  @Test
  fun topBarIsDisplayed() {
    setContent()

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.AVATAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.USERNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.ONLINE_INDICATOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun sendMessagesBarIsDisplayed() {
    setContent()

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.MESSAGES_SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.INPUT_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.INPUT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON).assertIsDisplayed()
  }

  @Test
  fun sendButtonIsClickable() {
    setContent()
    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()
  }

  @Test
  fun canWriteANewMessage() {
    val testViewModel = MessagesViewModel(otherUserId = "21", initialMessages = emptyList())
    // Set content with the test ViewModel
    composeTestRule.setContent {
      MessagesScreen(uid = "21", goBack = {}, messagesViewModel = testViewModel, autoScroll = false)
    }

    // Write a message
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.INPUT_FIELD).performTextInput("Banana")

    // Click send
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule.waitUntil(
        timeoutMillis = 5_000,
    ) {
      composeTestRule.onAllNodesWithText("Banana").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule
        .onNodeWithText("Banana")
        .assertExists("Message `Banana` not found after sending.")
  }

  /** Tests that bubbles appear when a new message is written */
  @Test
  fun messagesAddsBubble() {
    val testViewModel = MessagesViewModel(otherUserId = "234", initialMessages = emptyList())
    // Set content with the test ViewModel
    composeTestRule.setContent {
      MessagesScreen(
          uid = "234", goBack = {}, messagesViewModel = testViewModel, autoScroll = false)
    }

    // Write a message
    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.INPUT_FIELD)
        .performTextInput("Hello world")

    // Click send
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.MESSAGE_LIST)
        .assertIsDisplayed()
        .performScrollToNode(hasTestTag(MessagesScreenTestTags.MESSAGE_BUBBLE))
  }

  /** Tests that clicking on the Back Button correctly trigger a callback */
  @Test
  fun testBackButtonTriggersCallback() {
    var backClicked = false
    setContent(goBack = { backClicked = true })
    composeTestRule.onNodeWithTag(SelectMessagesScreenTestTags.BACK_BUTTON).performClick()
    assertTrue(backClicked)
  }
}
