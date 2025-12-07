package com.neptune.neptune.model

import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.messages.MessageRepositoryFirebase
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Instrumented tests for MessageRepositoryFirebase. This has been
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
class MessageRepositoryFirebaseTest {
  private val host = "10.0.2.2"
  private val firestorePort = 8080

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: MessageRepositoryFirebase
  private lateinit var mockedProfileRepo: ProfileRepository

  @Before
  fun setup() {
    runBlocking {
      db = FirebaseFirestore.getInstance()
      try {
        db.useEmulator(host, firestorePort)
      } catch (_: IllegalStateException) {
        "database emulator not running?"
      }

      mockedProfileRepo = mockk(relaxed = true)
      repo = MessageRepositoryFirebase(db, mockedProfileRepo)

      cleanUp()
    }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun observeMessagePreviews() = runBlocking {
    val currentUid = "me"
    val otherUid = "other"

    // mock profile
    coEvery { mockedProfileRepo.getProfile(otherUid) } returns
        Profile(
            uid = otherUid, username = "OtherUser", avatarUrl = "", bio = "", isAnonymous = false)

    // insert message doc
    val doc = db.collection("messages").document("m1")
    doc.set(
            mapOf(
                "participants" to listOf(currentUid, otherUid),
                "lastMessage" to "Hello there",
                "lastTimestamp" to Timestamp.now()))
        .await()

    repo.observeMessagePreviews(currentUid).test {
      val item = awaitItem()

      Assert.assertEquals(1, item.size)
      val preview = item.first()

      Assert.assertEquals(otherUid, preview.profile.uid)
      Assert.assertEquals("Hello there", preview.lastMessage)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun observeUserOnlineState_emitsCorrectValues() = runBlocking {
    val uid = "user123"

    // Mock Firebase Realtime Database
    val mockDatabase = mockk<FirebaseDatabase>()
    val mockRef = mockk<DatabaseReference>()

    // to trigger event manually
    val listenerSlot = slot<ValueEventListener>()

    mockkStatic(FirebaseDatabase::class)
    every {
      FirebaseDatabase.getInstance(
          "https://neptune-e2728-default-rtdb.europe-west1.firebasedatabase.app/")
    } returns mockDatabase

    every { mockDatabase.getReference("status/$uid") } returns mockRef
    every { mockRef.addValueEventListener(capture(listenerSlot)) } answers { listenerSlot.captured }
    every { mockRef.removeEventListener(any<ValueEventListener>()) } returns Unit

    // Create repo ;firestore and profileRepo are unused for this test
    val repo = MessageRepositoryFirebase(firestore = mockk(), profileRepo = mockk())

    val snapshotOnline = mockk<com.google.firebase.database.DataSnapshot>()
    every { snapshotOnline.child("state").getValue(String::class.java) } returns "online"

    val snapshotOffline = mockk<com.google.firebase.database.DataSnapshot>()
    every { snapshotOffline.child("state").getValue(String::class.java) } returns "offline"

    val flow = repo.observeUserOnlineState(uid)

    flow.test {
      // Simulate Firebase sending ONLINE
      listenerSlot.captured.onDataChange(snapshotOnline)
      Assert.assertEquals(true, awaitItem())

      // Simulate Firebase sending OFFLINE
      listenerSlot.captured.onDataChange(snapshotOffline)
      Assert.assertEquals(false, awaitItem())

      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun cleanUp() {
    runBlocking {
      db.collection("messages").get().await().documents.forEach {
        db.collection("messages").document(it.id).delete().await()
      }
    }
  }
}
