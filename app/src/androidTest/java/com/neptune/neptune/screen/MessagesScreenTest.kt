package com.neptune.neptune.screen

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.ui.messages.MessagesScreen
import com.neptune.neptune.ui.messages.MessagesScreenTestTags
import com.neptune.neptune.ui.messages.MessagesViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
      otherUserId: String = "123",
      currentUserId: String = "me",
      goBack: () -> Unit = {},
      messagesViewModel: MessagesViewModel? = null
  ) {
    composeTestRule.setContent {
      MessagesScreen(
          otherUserId = otherUserId,
          currentUserId = currentUserId,
          goBack = goBack,
          messagesViewModel =
              messagesViewModel
                  ?: MessagesViewModel(otherUserId = otherUserId, currentUserId = currentUserId))
    }
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
    val testViewModel = MessagesViewModel(otherUserId = "21", currentUserId = "me")
    // Set content with the test ViewModel
    composeTestRule.setContent {
      MessagesScreen(
          otherUserId = "21",
          currentUserId = "me",
          goBack = {},
          messagesViewModel = testViewModel,
          autoScroll = false)
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
    val testViewModel = MessagesViewModel(otherUserId = "234", currentUserId = "me")
    // Set content with the test ViewModel
    composeTestRule.setContent {
      MessagesScreen(
          otherUserId = "234",
          currentUserId = "me",
          goBack = {},
          messagesViewModel = testViewModel,
          autoScroll = false)
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

  /** Tests that the messagesScreen receives Uid Argument */
  @Test
  fun messagesScreenReceivesUidArgument() {
    val testOtherUserId = "otherUser123"

    // Mock the SignInViewModel with a currentUser
    val mockSignInViewModel = mockk<SignInViewModel>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { mockUser.uid } returns "currentUserId"
    every { mockSignInViewModel.currentUser } returns MutableStateFlow(mockUser)

    composeTestRule.setContent {
      // Get current user ID from the mocked SignInViewModel
      val firebaseUser by mockSignInViewModel.currentUser.collectAsState()
      val currentUserId = firebaseUser?.uid ?: ""

      // Directly call the composable with test arguments
      MessagesScreen(otherUserId = testOtherUserId, currentUserId = currentUserId, goBack = {})
    }

    // Assert that MessagesScreen content is displayed
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.MESSAGES_SCREEN).assertIsDisplayed()
  }
}
