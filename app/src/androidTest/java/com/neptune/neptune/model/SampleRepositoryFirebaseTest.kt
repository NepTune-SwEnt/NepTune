package com.neptune.neptune.model

import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepositoryFirebase
import junit.framework.TestCase.assertEquals
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
 * - Start them: firebase emulators:start --only firestore
 *
 * On Android emulator, host is 10.0.2.2
 *
 * @author Angéline Bignens
 */
class SampleRepositoryFirebaseTest {
  private val host = "10.0.2.2"
  private val firestorePort = 8080
  private lateinit var repository: SampleRepositoryFirebase
  private lateinit var db: FirebaseFirestore

  @Before
  fun setup() {
    runBlocking {
      db = FirebaseFirestore.getInstance()
      try {
        db.useEmulator(host, firestorePort)
      } catch (e: IllegalStateException) {
        "database emulator not running?"
      }
      repository = SampleRepositoryFirebase(db)

      // Clean-up
      cleanUp()
    }
  }

  @After
  fun tearDown() {
    cleanUp()
  }

  @Test
  fun addAndFetchSampleWorks() = runBlocking {
    val sample =
        Sample(
            id = 1,
            name = "Test Sample",
            description = "Testing Firestore add",
            durationSeconds = 30,
            tags = listOf("#easy", "#sample"),
            likes = 10,
            comments = 1,
            downloads = 2)

    repository.addSample(sample)
    val fetched = repository.getSamples()
    assertEquals(1, fetched.size)
    assertEquals(sample.name, fetched.first().name)
  }

  @Test
  fun toggleLikeIncrementsAndDecrementsProperly() = runBlocking {
    // initial sample with 5 likes
    val sample =
        Sample(
            id = 2,
            name = "Test Sample",
            description = "Test Sample",
            durationSeconds = 10,
            tags = listOf("#easy"),
            likes = 5,
            comments = 0,
            downloads = 0)
    repository.addSample(sample)

    // Increase to 6
    repository.toggleLike(2, true)
    var fetched = repository.getSamples().first { it.id == 2 }
    assertEquals(6, fetched.likes)

    // Unlike it (-1) so 5
    repository.toggleLike(2, false)
    fetched = repository.getSamples().first { it.id == 2 }
    assertEquals(5, fetched.likes)

    // Like again -> go to 6
    repository.toggleLike(2, true)
    fetched = repository.getSamples().first { it.id == 2 }
    assertEquals(6, fetched.likes)
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
