package com.neptune.neptune.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
        timeoutMillis = 5000,
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
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.BACK_BUTTON).performClick()
    assertTrue(backClicked)
  }

  /** Tests that u2 fake data loads correctly */
  @Test
  fun messagesScreenLoadsFakeDataForU2() {
    composeTestRule.setContent { MessagesScreen(uid = "u2", goBack = {}) }

    // Username
    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains("test2")

    // Online indicator
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.ONLINE_INDICATOR).assertIsDisplayed()

    // Messages
    composeTestRule.waitUntil(5000) {
      composeTestRule.onAllNodesWithText("BLEH😝").fetchSemanticsNodes().size == 3
    }

    composeTestRule.onAllNodesWithText("BLEH😝").assertCountEquals(3)
  }

  /** Tests that u3 fake data loads correctly */
  @Test
  fun messagesScreenLoadsFakeDataForU3() {
    composeTestRule.setContent { MessagesScreen(uid = "u3", goBack = {}) }

    // Username
    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains("test3")

    // Online indicator
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.ONLINE_INDICATOR).assertIsDisplayed()

    // Messages
    composeTestRule.waitUntil(5000) {
      composeTestRule.onAllNodesWithText("Banana").fetchSemanticsNodes().isNotEmpty() &&
          composeTestRule.onAllNodesWithText("21").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Banana").assertIsDisplayed()
    composeTestRule.onNodeWithText("21").assertIsDisplayed()
  }
}
