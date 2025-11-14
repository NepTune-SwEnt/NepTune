package com.neptune.neptune.e2e

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.ui.navigation.NavigationTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTags
import com.neptune.neptune.ui.search.SearchScreenTestTagsPerSampleCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchE2ETest {

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
  fun search_flow_typeSample5_verifyOnlySample5Shown() = runBlocking {

    // --- LOGIN ---
    loginToFirebaseEmulator()

    // --- BOOT APP ---
    composeTestRule.activity.setContent { NeptuneApp() }
    composeTestRule.waitForIdle()

    // --- GO TO SEARCH TAB ---
    composeTestRule.onNodeWithTag(NavigationTestTags.SEARCH_TAB).performClick()
    composeTestRule.waitForIdle()

    // Ensure screen exists
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_SCREEN).assertIsDisplayed()

    // --- TYPE IN SEARCH BAR ---
    composeTestRule
        .onNode(hasSetTextAction() and hasParent(hasTestTag(SearchScreenTestTags.SEARCH_BAR)))
        .performTextInput("Sample5")

    composeTestRule.waitUntil(2000L) {
      composeTestRule
          .onAllNodesWithTag(SearchScreenTestTagsPerSampleCard("5").SAMPLE_CARD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Wait for debounce
    delay(600L)
    composeTestRule.waitForIdle()
    delay(3000L)

    // --- CHECK RESULTS ---
    // SAMPLE 5 MUST appear
    composeTestRule.onNodeWithText("Sample 5").assertIsDisplayed()

    // SAMPLE 4 MUST NOT appear
    composeTestRule.onNodeWithText("Sample 4").assertDoesNotExist()
  }
}
