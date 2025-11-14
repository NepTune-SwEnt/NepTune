package com.neptune.neptune.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.ui.authentification.SignInScreenTags
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.profile.ProfileScreenTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogoutFlowE2ETest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun loginToFirebaseEmulator() {
    val auth = FirebaseAuth.getInstance()
    runCatching { auth.useEmulator("10.0.2.2", 9099) }
    runCatching { FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080) }
    runCatching { auth.signOut() }

    val task = auth.signInAnonymously()
    Tasks.await(task)
    assertTrue(task.isSuccessful)
  }

  @Test
  fun logoutFromProfileNavigatesToLogin() {
    // --- LOGIN ---
    loginToFirebaseEmulator()

    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Assume user is already logged in and app starts at MainScreen
    // Navigate to Profile screen
    composeTestRule
        .onAllNodesWithTag(ProfileScreenTestTags.ROOT)
        .assertCountEquals(2) // Profile screen is visible
    // Open Settings
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.SETTINGS_BUTTON)
        .assertExists()
        .performClick()

    // Now we are on SettingsScreen
    composeTestRule.onNodeWithText("Account").assertExists().performClick()

    // On Account Settings screen
    composeTestRule.onNodeWithText("Log Out").assertExists().performClick()

    // Assert that we are now on the login screen
    // Assume login screen has a node tagged "login/root"
    composeTestRule.onNodeWithTag(SignInScreenTags.LOGIN_TITLE).assertExists()
  }
}
