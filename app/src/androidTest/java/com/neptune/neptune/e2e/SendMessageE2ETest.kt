package com.neptune.neptune.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.authentification.SignInScreenTags
import com.neptune.neptune.ui.messages.MessagesScreenTestTags
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.profile.ProfileScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val otherUser = "otherUser"
private const val mailSender = "sender@test.com"
private const val mailReceiver = "receiver@test.com"
private const val password = "password123"

@RunWith(AndroidJUnit4::class)
class SendMessageE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun setupFirebaseEmulators() {
    val auth = FirebaseAuth.getInstance()
    runCatching { auth.useEmulator("10.0.2.2", 9099) }
    runCatching { FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080) }
    if (auth.currentUser != null) {
      auth.signOut()
    }
  }

  private fun seedReceiverUser() {
    // Create the receiver user account
    composeTestRule.onNodeWithTag(SignInScreenTags.TOGGLE_REGISTER).performClick()
    composeTestRule.onNodeWithTag(SignInScreenTags.EMAIL_FIELD).performTextInput(mailReceiver)
    composeTestRule.onNodeWithTag(SignInScreenTags.PASSWORD_FIELD).performTextInput(password)
    composeTestRule
        .onNodeWithTag(SignInScreenTags.CONFIRM_PASSWORD_FIELD)
        .performTextInput(password)
    composeTestRule.onNodeWithTag(SignInScreenTags.SUBMIT_EMAIL).performClick()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.PROFILE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val repo = ProfileRepositoryProvider.repository

    runBlocking { repo.ensureProfile(suggestedUsernameBase = otherUser, name = otherUser) }

    // Go to profile and set username
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    // logout
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ProfileScreenTestTags.SETTINGS_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON).performClick()
    composeTestRule.onNodeWithText("Account").performClick()
    composeTestRule.onNodeWithText("Log Out").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTags.TOGGLE_REGISTER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun sendMessageToUserFlow() {
    // --- SETUP ---
    setupFirebaseEmulators()
    seedReceiverUser()

    composeTestRule.waitForIdle()

    // --- REGISTER SENDER ---
    composeTestRule.onNodeWithTag(SignInScreenTags.TOGGLE_REGISTER).performClick()
    composeTestRule.onNodeWithTag(SignInScreenTags.EMAIL_FIELD).performTextInput(mailSender)
    composeTestRule.onNodeWithTag(SignInScreenTags.PASSWORD_FIELD).performTextInput(password)
    composeTestRule
        .onNodeWithTag(SignInScreenTags.CONFIRM_PASSWORD_FIELD)
        .performTextInput(password)
    composeTestRule.onNodeWithTag(SignInScreenTags.SUBMIT_EMAIL).performClick()

    // Wait for app load (Navigation bar visible)
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.SEARCH_TAB)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // --- SEARCH AND FOLLOW USER ---

    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()

    val seeUsersNode = composeTestRule.onAllNodesWithText("See Users")
    if (seeUsersNode.fetchSemanticsNodes().isNotEmpty()) {
      seeUsersNode.onFirst().performClick()
    }

    // Search for the username
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.SEARCH_BAR)
        .onChildren()
        .filter(hasSetTextAction())
        .onFirst()
        .performTextInput(otherUser)

    // Wait for the result to appear and click it
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithContentDescription("User Avatar")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onAllNodesWithContentDescription("User Avatar").onFirst().performClick()

    // Click Follow on the user's profile
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ProfileScreenTestTags.FOLLOW_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FOLLOW_BUTTON).performClick()

    // --- NAVIGATE TO MESSAGES ---
    composeTestRule.onNodeWithTag(NavigationTestTags.MESSAGE_BUTTON).performClick()

    // --- USER SELECTION ---
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("User").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onAllNodesWithContentDescription("User").onFirst().performClick()

    // --- ENVOI MESSAGE ---
    val messageToSend = "Hello from E2E Test!"

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MessagesScreenTestTags.INPUT_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.INPUT_BAR).performTextInput(messageToSend)
    composeTestRule.onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON).performClick()

    // --- VERIFICATION ---
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(messageToSend).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText(messageToSend).assertExists()
  }
}
