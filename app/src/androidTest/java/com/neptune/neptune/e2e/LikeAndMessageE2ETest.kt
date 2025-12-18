package com.neptune.neptune.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.MainActivity
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.main.MainScreenTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test
// firebase emulators:start --only firestore,auth

@RunWith(AndroidJUnit4::class)
class LikeAndMessageE2ETest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun loginToFirebaseEmulator() {
        val auth = FirebaseAuth.getInstance()
        runCatching { auth.useEmulator("10.0.2.2", 9099) }
        runCatching { FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080) }
        runCatching { auth.signOut() }

        val email = "ci-user@test.local"
        val password = "password123" // fake, local, disposable

        // Try sign-in first
        val signIn = auth.signInWithEmailAndPassword(email, password)
        val result = runCatching { Tasks.await(signIn) }.getOrNull()

        if (result == null || !signIn.isSuccessful) {
            // User doesn't exist → create it (emulator only)
            val create = auth.createUserWithEmailAndPassword(email, password)
            Tasks.await(create)
            check(create.isSuccessful)

            val signInAgain = auth.signInWithEmailAndPassword(email, password)
            Tasks.await(signInAgain)
            check(signInAgain.isSuccessful)
        }
    }
    @Test
    fun likeAndCommentFirstPost() {
        loginToFirebaseEmulator()
        seedProfileForCurrentUser()
        seedSample()
        // Sample is visible
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.SAMPLE_CARD)
            .assertIsDisplayed()

        // Like
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.SAMPLE_LIKES)
            .assertTextContains("0")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.SAMPLE_LIKES)
            .assertTextContains("1")
        // Open comments
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.SAMPLE_COMMENTS)
            .performClick()
        composeTestRule.waitForIdle()
        // Comment dialog shown
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.COMMENT_SECTION)
            .assertIsDisplayed()

        // Type comment
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.COMMENT_TEXT_FIELD)
            .performTextInput("hello from emulator test")
        composeTestRule.waitForIdle()
        // Post
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.COMMENT_POST_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
        // Comment list visible
        composeTestRule
            .onNodeWithTag(MainScreenTestTags.COMMENT_LIST)
            .assertIsDisplayed()
    }
    private fun seedSample() {
        val repo = SampleRepositoryProvider.repository
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
                ownerId = "owner_test",
                storagePreviewSamplePath = "x" // IMPORTANT: must be non-empty
            )

        runBlocking { repo.addSample(sample) }
    }
    private fun seedProfileForCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("no user")
        val db = FirebaseFirestore.getInstance()

        // Create profile if missing (allowed because uid == request.auth.uid)
        val doc = db.collection("profiles").document(uid)
        val snap = Tasks.await(doc.get())

        if (!snap.exists()) {
            Tasks.await(
                doc.set(
                    mapOf(
                        "uid" to uid,
                        "username" to "ciUser",
                        "avatarUrl" to "",
                        "isAnonymous" to false
                    )
                )
            )
        }
    }


}
