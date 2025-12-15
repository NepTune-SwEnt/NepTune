package com.neptune.neptune.screen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.ui.authentification.SignInViewModel
import com.neptune.neptune.ui.messages.MessagesRoute
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
        .onAllNodesWithText("Banana")
        .onFirst()
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

  /** Tests that messages Route with user correctly display MessageScreen */
  @Test
  fun messagesRouteWithUserShowsMessagesScreen() {
    val otherUserId = "other123"
    val currentUserId = "me123"

    val firebaseUser = mockk<FirebaseUser> { every { uid } returns currentUserId }

    val fakeSignInViewModel = mockk<SignInViewModel>()
    every { fakeSignInViewModel.currentUser } returns MutableStateFlow(firebaseUser)

    composeTestRule.setContent {
      MessagesRoute(otherUserId = otherUserId, signInViewModel = fakeSignInViewModel, goBack = {})
    }

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.MESSAGES_SCREEN).assertIsDisplayed()
  }

  /** Tests that messages Route without user doesn't display MessageScreen */
  @Test
  fun messagesRouteWithoutUserDoesNotShowScreen() {
    val fakeSignInViewModel = mockk<SignInViewModel>()
    every { fakeSignInViewModel.currentUser } returns MutableStateFlow(null)

    composeTestRule.setContent {
      MessagesRoute(otherUserId = "other", signInViewModel = fakeSignInViewModel, goBack = {})
    }

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.MESSAGES_SCREEN).assertDoesNotExist()
  }

  /** Tests that messages Route correctly reads UID Argument */
  @Test
  fun messagesNavRouteReadsUidArgument() {
    val otherUserId = "other123"
    val currentUserId = "me123"

    val firebaseUser = mockk<FirebaseUser> { every { uid } returns currentUserId }

    val signInViewModel = mockk<SignInViewModel>()
    every { signInViewModel.currentUser } returns MutableStateFlow(firebaseUser)

    composeTestRule.setContent {
      val navController = rememberNavController()

      NavHost(navController = navController, startDestination = "messages/{uid}") {
        composable(
            route = "messages/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })) { backStackEntry
              ->
              val otherUserIdFromNav =
                  backStackEntry.arguments?.getString("uid") ?: return@composable

              MessagesRoute(
                  otherUserId = otherUserIdFromNav, signInViewModel = signInViewModel, goBack = {})
            }
      }

      // Trigger navigation
      LaunchedEffect(Unit) { navController.navigate("messages/$otherUserId") }
    }

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.MESSAGES_SCREEN).assertIsDisplayed()
  }
}
