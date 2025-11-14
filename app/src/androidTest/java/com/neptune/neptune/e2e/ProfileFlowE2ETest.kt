package com.neptune.neptune.e2e

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.profile.ProfileScreenTestTags
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test using Compose test rule. Signs in anonymously, launches the app at the Main
 * screen, navigates to Profile, edits the name field and saves.
 */
@RunWith(AndroidJUnit4::class)
class ProfileFlowE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun anonymousUser_canEditProfile_composeRule() {
    // Configure Firebase emulators (use localhost host for Android emulator)
    val host = "10.0.2.2"
    val authPort = 9099
    val firestorePort = 8080

    val auth = FirebaseAuth.getInstance()
    try {
      auth.useEmulator(host, authPort)
    } catch (_: IllegalStateException) {
      // emulator already configured
    }

    try {
      FirebaseFirestore.getInstance().useEmulator(host, firestorePort)
    } catch (_: IllegalStateException) {
      // emulator already configured
    }

    // Ensure we are signed out and then sign in anonymously (blocking)
    runCatching { auth.signOut() }
    val signInTask = auth.signInAnonymously()
    Tasks.await(signInTask, 10, TimeUnit.SECONDS)
    assertTrue("Firebase anonymous sign-in failed", signInTask.isSuccessful)

    // Render the app starting at the Main screen so the signed-in user is picked up by ViewModels
    composeTestRule.activity.setContent { NeptuneApp() }
    composeTestRule.waitForIdle()

    // Navigate to profile using the navigation test tag defined in MainScreen
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EDIT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val username = "E2EETestUser"

    // Modify fields
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIELD_NAME).performTextClearance()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIELD_NAME).performTextInput(username)

    // Save
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify the new name is displayed in view mode
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.NAME).assertTextEquals(username)
  }
}
