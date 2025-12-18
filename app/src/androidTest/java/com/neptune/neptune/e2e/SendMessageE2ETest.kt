package com.neptune.neptune.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.ui.authentification.SignInScreenTags
import com.neptune.neptune.ui.messages.MessagesScreenTestTags
import com.neptune.neptune.ui.navigation.NavigationTestTags
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val mailSender = "sender@test.com"
private const val mailReceiver = "receiver@test.com"
private const val password = "password123"
private const val otherUser = "testbuddy"

@RunWith(AndroidJUnit4::class)
class SendMessageE2ETest {

  @get:Rule val composeTestRule = createEmptyComposeRule()

  private fun setupFirebaseEmulators() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val host = "10.0.2.2"

    // Configure emulators before the app starts accessing Firebase
    runCatching { auth.useEmulator(host, 9099) }
    runCatching { db.useEmulator(host, 8080) }

    if (auth.currentUser != null) {
      auth.signOut()
    }
  }

  private fun seedReceiverUser() {
    val auth = FirebaseAuth.getInstance()

    // 1. Create the receiver account (Backend only)
    val createJson = auth.createUserWithEmailAndPassword(mailReceiver, password)
    Tasks.await(createJson, 5, TimeUnit.SECONDS)

    // 2. Necessary pause for Auth token propagation to Firestore to avoid PERMISSION_DENIED
    Thread.sleep(1000)

    // 3. Create profile via Repository to ensure valid format and business logic
    val repo = ProfileRepositoryProvider.repository
    runBlocking {
      repo.ensureProfile(suggestedUsernameBase = otherUser, name = "Receiver Test")
      repo.updateBio("Profile seeded via E2E test")
    }

    auth.signOut()
  }

  @Test
  fun sendMessageToUserFlow() {
    // --- INITIALIZATION ---
    setupFirebaseEmulators()
    seedReceiverUser()

    // --- ACTIVITY LAUNCH ---
    ActivityScenario.launch(MainActivity::class.java)

    // --- SENDER REGISTRATION ---
    composeTestRule.onNodeWithTag(SignInScreenTags.TOGGLE_REGISTER).performClick()
    composeTestRule.onNodeWithTag(SignInScreenTags.EMAIL_FIELD).performTextInput(mailSender)
    composeTestRule.onNodeWithTag(SignInScreenTags.PASSWORD_FIELD).performTextInput(password)
    composeTestRule
        .onNodeWithTag(SignInScreenTags.CONFIRM_PASSWORD_FIELD)
        .performTextInput(password)
    composeTestRule.onNodeWithTag(SignInScreenTags.SUBMIT_EMAIL).performClick()

    // --- WAIT FOR NAVIGATION BAR ---
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(NavigationTestTags.MESSAGE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.MESSAGE_BUTTON).performClick()

    // --- USER SELECTION ---
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithContentDescription("Avatar").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onAllNodesWithContentDescription("Avatar").onFirst().performClick()

    // --- MESSAGE SENDING ---
    val messageToSend = "Hello from E2E test!"

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MessagesScreenTestTags.INPUT_BAR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Targeting the child of the container that accepts text input
    composeTestRule
        .onNodeWithTag(MessagesScreenTestTags.INPUT_BAR)
        .onChildren()
        .filter(hasSetTextAction())
        .onFirst()
        .performTextInput(messageToSend)

    composeTestRule.onNodeWithTag(MessagesScreenTestTags.SEND_BUTTON).performClick()

    // --- VERIFICATION ---
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText(messageToSend).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText(messageToSend).assertExists()
  }
}
