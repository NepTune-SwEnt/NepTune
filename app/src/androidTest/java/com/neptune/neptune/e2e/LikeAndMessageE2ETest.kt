package com.neptune.neptune.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.neptune.neptune.MainActivity
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.main.MainScreenTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// run with firebase emulators:start --only firestore,auth
// written with help from ChatGPT
@RunWith(AndroidJUnit4::class)
class LikeAndMessageE2ETest {

  @get:Rule val composeTestRule = createEmptyComposeRule()

  @Test
  fun likeAndCommentFirstPost() {
    // MUST happen before MainActivity (or any repo) starts
    configureFirebaseEmulatorsAndLogin()
    seedProfileForCurrentUser()
    seedSample()

    // now launch app
    ActivityScenario.launch(MainActivity::class.java)
    // Sample visible
    composeTestRule.onNodeWithTag(MainScreenTestTags.SAMPLE_CARD).assertIsDisplayed()

    // Like 0 -> 1
    composeTestRule
        .onNodeWithTag(MainScreenTestTags.SAMPLE_LIKES)
        .assertTextContains("0")
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MainScreenTestTags.SAMPLE_LIKES).assertTextContains("1")

    // Comments
    composeTestRule.onNodeWithTag(MainScreenTestTags.SAMPLE_COMMENTS).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_SECTION).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(MainScreenTestTags.COMMENT_TEXT_FIELD)
        .performTextInput("hello from emulator test")

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_POST_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MainScreenTestTags.COMMENT_LIST).assertIsDisplayed()
  }

  private fun configureFirebaseEmulatorsAndLogin() {
    val host = "10.0.2.2"

    val auth = FirebaseAuth.getInstance()
    runCatching { auth.useEmulator(host, 9099) }

    val db = FirebaseFirestore.getInstance()
    runCatching { db.useEmulator(host, 8080) }

    runCatching { auth.signOut() }

    val email = "ci-user@test.local"
    val password = "password123"

    runCatching { Tasks.await(auth.signInWithEmailAndPassword(email, password)) }
        .getOrElse {
          Tasks.await(auth.createUserWithEmailAndPassword(email, password))
          Tasks.await(auth.signInWithEmailAndPassword(email, password))
        }

    check(auth.currentUser != null) { "Auth login failed" }
  }

  private fun seedProfileForCurrentUser() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("no user")
    val db = FirebaseFirestore.getInstance()

    val doc = db.collection("profiles").document(uid)

    // Upsert (no GET)
    val task =
        doc.set(
            mapOf(
                "uid" to uid,
                "username" to "ciUser",
                "avatarUrl" to "",
                "isAnonymous" to false,
                "likes" to 0),
            SetOptions.merge())

    Tasks.await(task)
    assertTrue(task.isSuccessful)
  }

  private fun seedSample() {
    val repo = SampleRepositoryProvider.repository
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("no user")

    val sample =
        Sample(
            id = "sample_test",
            name = "Test Sample",
            description = "Seeded sample for UI test",
            durationMillis = 10_000,
            tags = listOf("test"),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            isPublic = true,
            ownerId = uid,
            storagePreviewSamplePath = "x")

    runBlocking { repo.addSample(sample) }
  }
}
