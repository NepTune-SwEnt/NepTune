package com.neptune.neptune.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepositoryFirebase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Instrumented tests for SampleRepositoryFirebase. This has been
 * * written with the help of LLMs.
 *
 * REQUIREMENTS:
 * - Firestore emulator on port 8080
 * - Auth emulator on port 9099
 * - Start them: firebase emulators:start --only firestore,auth
 *
 * On Android emulator, host is 10.0.2.2
 *
 * @author Angéline Bignens
 */
class SampleRepositoryFirebaseTest {
  private val host = "10.0.2.2"
  private val firestorePort = 8080
  private val authPort = 9099
  private lateinit var repository: SampleRepositoryFirebase
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth

  @Before
  fun setup() {
    runBlocking {
      db = FirebaseFirestore.getInstance()
      try {
        db.useEmulator(host, firestorePort)
      } catch (_: IllegalStateException) {
        "database emulator not running?"
      }
      repository = SampleRepositoryFirebase(db)

      auth = FirebaseAuth.getInstance()
      try {
        auth.useEmulator(host, authPort)
      } catch (_: IllegalStateException) {
        "auth emulator not running?"
      }
      // You need to be sign in to like
      if (auth.currentUser == null) {
        auth.signInAnonymously().await()
      }
      // Clean-up
      cleanUp()
    }
  }

  @After
  fun tearDown() {
    cleanUp()
  }

  @Test
  fun addAndFetchSample() = runBlocking {
    val sample =
        Sample(
            id = "1",
            name = "Test Sample",
            description = "Testing Firestore add",
            durationSeconds = 30,
            tags = listOf("#easy", "#sample"),
            likes = 10,
            usersLike = emptyList(),
            comments = 1,
            downloads = 2)

    repository.addSample(sample)
    val fetched = repository.getSamples()
    assertEquals(1, fetched.size)
    assertEquals(sample.name, fetched.first().name)
  }

  @Test
  fun toggleLikeIncrementsAndDecrements() = runBlocking {
    // initial sample with 5 likes
    val sample =
        Sample(
            id = "2",
            name = "Test Sample",
            description = "Test Sample",
            durationSeconds = 10,
            tags = listOf("#easy"),
            likes = 5,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0)
    repository.addSample(sample)

    // Check we are logged in
    assertTrue(auth.currentUser != null)

    // Increase to 6
    repository.toggleLike("2", true)
    var fetched = repository.getSamples().first { it.id == "2" }
    assertEquals(6, fetched.likes)

    // Unlike it (-1) so 5
    repository.toggleLike("2", false)
    fetched = repository.getSamples().first { it.id == "2" }
    assertEquals(5, fetched.likes)

    // Like again -> go to 6
    repository.toggleLike("2", true)
    fetched = repository.getSamples().first { it.id == "2" }
    assertEquals(6, fetched.likes)
  }

  @Test
  fun addAndObserveComments() = runBlocking {
    val sample =
        Sample(
            id = "3",
            name = "Comment Test",
            description = "Testing comments",
            durationSeconds = 20,
            tags = listOf("#test"),
            likes = 0,
            comments = 0,
            downloads = 0,
            usersLike = emptyList())
    repository.addSample(sample)

    // Add a comment
    repository.addComment("3", "1", "Alice", "Hello world!")

    // Observe the comments in real time
    val commentsFlow = repository.observeComments("3")
    val firstEmission = commentsFlow.first()

    // Adding a comment should increment by 1
    assertEquals(1, firstEmission.size)
    assertEquals("1", firstEmission.first().authorId)
    assertEquals("Alice", firstEmission.first().authorName)
    assertEquals("Hello world!", firstEmission.first().text)
  }

  @Test
  fun hasUserLikedReflectsLikeStatus() = runBlocking {
    val sample =
        Sample(
            id = "4",
            name = "Like status",
            description = "Description like status",
            durationSeconds = 15,
            tags = listOf("#check"),
            likes = 0,
            comments = 0,
            downloads = 0,
            usersLike = emptyList())
    repository.addSample(sample)

    val initialLiked = repository.hasUserLiked("4")
    assertFalse(initialLiked)

    repository.toggleLike("4", true)

    val likedAfter = repository.hasUserLiked("4")
    assertTrue(likedAfter)
  }

  private fun cleanUp() {
    runBlocking {
      val col = db.collection("samples")
      val snaps = col.get().await()
      for (doc in snaps.documents) {
        col.document(doc.id).delete().await()
      }
    }
  }
}
