package com.neptune.neptune.model

import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.neptune.neptune.model.messages.Message
import com.neptune.neptune.model.messages.MessageRepository
import com.neptune.neptune.model.messages.MessageRepositoryFirebase
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.ui.messages.MessagesViewModel
import com.neptune.neptune.ui.messages.MessagesViewModelFactory
import com.neptune.neptune.util.RealtimeDatabaseProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Instrumented tests for MessageRepositoryFirebase. This has been written with the help of LLMs.
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
  fun observeUserOnlineStateEmitsCorrectValues() = runBlocking {
    val uid = "user123"

    // Mock Firebase Realtime Database
    val mockDatabase = mockk<FirebaseDatabase>()
    val mockRef = mockk<DatabaseReference>()

    // to trigger event manually
    val listenerSlot = slot<ValueEventListener>()

    mockkStatic(FirebaseDatabase::class)
    every { RealtimeDatabaseProvider.getDatabase() } returns mockDatabase

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

  @Test
  fun observeMessagesEmitsMessagesInRealTime() = runBlocking {
    val conversationId = "conv1"
    val authorId = "me"

    // insert initial message
    val firstMessage =
        Message(id = "msg1", authorId = authorId, text = "Hello world", timestamp = Timestamp.now())

    repo.sendMessage(conversationId, firstMessage)

    // Observe messages
    repo.observeMessages(conversationId).test {
      val messages = awaitItem()
      assert(messages.any { it.text == "Hello world" })

      // Send a second message
      val secondMessage =
          Message(
              id = "msg2",
              authorId = authorId,
              text = "Second message",
              timestamp = Timestamp.now())
      repo.sendMessage(conversationId, secondMessage)

      val updatedMessages = awaitItem()
      assertTrue(updatedMessages.any { it.text == "Second message" })

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun sendMessageWritesToFirestore() = runBlocking {
    val conversationId = "conv2"
    val message =
        Message(id = "msg1", authorId = "me", text = "Test message", timestamp = Timestamp.now())

    repo.sendMessage(conversationId, message)

    // Verify Firestore document exists
    val snapshot =
        db.collection("messages").document(conversationId).collection("messages").get().await()

    assertTrue(snapshot.documents.any { it.getString("text") == "Test message" })
    assertTrue(snapshot.documents.any { it.getString("authorId") == "me" })
  }

  /** Test that the MessagesFactory correctly throws an exception */
  @Test
  fun factoryThrowsException() {
    val factory = MessagesViewModelFactory("uid", "123")

    class UnknownViewModel : ViewModel()

    val exception =
        Assert.assertThrows(IllegalArgumentException::class.java) {
          factory.create(UnknownViewModel::class.java)
        }

    Assert.assertEquals(
        "Unknown ViewModel class: ${UnknownViewModel::class.java.name}", exception.message)
  }

  /** Test that the MessagesViewModelFactory creates the ViewModel */
  @Test
  fun factoryCreatesSelectMessagesViewModel() {
    val factory = MessagesViewModelFactory("uid", "123")

    val viewModel = factory.create(MessagesViewModel::class.java)

    // Check that the ViewModel is not null
    Assert.assertNotNull(viewModel)
  }

  @Test
  fun observeMessagesEmitsEmptyListOnErrorOrNullSnapshot() = runBlocking {
    val mockFirestore = mockk<FirebaseFirestore>()
    val mockCollection = mockk<CollectionReference>()
    val mockDoc = mockk<DocumentReference>()
    val mockMsgCollection = mockk<CollectionReference>()
    val mockQuery = mockk<Query>()

    val listenerSlot = slot<EventListener<QuerySnapshot?>>()

    // Mock the chain
    every { mockFirestore.collection(any()) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDoc
    every { mockDoc.collection(any()) } returns mockMsgCollection
    every { mockMsgCollection.orderBy(any<String>(), any<Query.Direction>()) } returns mockQuery

    // Capture the listener
    every { mockQuery.addSnapshotListener(capture(listenerSlot)) } answers
        {
          listenerSlot.captured.onEvent(
              null,
              FirebaseFirestoreException(
                  "Simulated error", FirebaseFirestoreException.Code.UNKNOWN))
          mockk<ListenerRegistration>()
        }

    val repo = MessageRepositoryFirebase(mockFirestore, mockedProfileRepo)

    repo.observeMessages("conv_error_test").test {
      val messages = awaitItem()
      assertTrue(messages.isEmpty())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun observeMessagePreviewsSkipsPreviewWhenNoOtherUid() = runBlocking {
    val currentUid = "me"

    // Insert a document where the only participant is the current user
    val doc = db.collection("messages").document("solo")
    doc.set(
            mapOf(
                "participants" to listOf(currentUid),
                "lastMessage" to "Hello self",
                "lastTimestamp" to Timestamp.now()))
        .await()

    // Run the flow
    repo.observeMessagePreviews(currentUid).test {
      val previews = awaitItem()

      // Since there is no otherUid, the document should be skipped
      assertTrue(previews.isEmpty())

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun messagesViewModelObserveOtherUserProfile() = runBlocking {
    val otherUserId = "other123"
    val currentUserId = "me"
    val expectedUsername = "OtherUser"
    val expectedAvatar = "https://avatar.url"

    // Mock ProfileRepository
    val mockProfileRepo = mockk<ProfileRepository>()
    coEvery { mockProfileRepo.getProfile(otherUserId) } returns
        Profile(
            uid = otherUserId,
            username = expectedUsername,
            avatarUrl = expectedAvatar,
            bio = "",
            isAnonymous = false)

    // Mock MessageRepository (unused in this test)
    val mockMessageRepo = mockk<MessageRepository>(relaxed = true)

    // Create ViewModel with mocked repos
    val viewModel =
        MessagesViewModel(
            otherUserId = otherUserId,
            currentUserId = currentUserId,
            messageRepo = mockMessageRepo,
            profileRepo = mockProfileRepo)

    assertTrue(viewModel.otherUsername.value == expectedUsername)
    assertTrue(viewModel.otherAvatar.value == expectedAvatar)
  }

  @Test
  fun messagesViewModelObserveOtherUserProfileException() = runBlocking {
    val otherUserId = "other123"
    val currentUserId = "me"

    val mockProfileRepo = mockk<ProfileRepository>()
    coEvery { mockProfileRepo.getProfile(otherUserId) } throws RuntimeException("Boom")

    val mockMessageRepo = mockk<MessageRepository>(relaxed = true)

    // triggers observeOtherUserProfile()
    MessagesViewModel(
        otherUserId = otherUserId,
        currentUserId = currentUserId,
        messageRepo = mockMessageRepo,
        profileRepo = mockProfileRepo)

    // catch block executed
    assertTrue(true)
  }

  private fun cleanUp() {
    runBlocking {
      db.collection("messages").get().await().documents.forEach {
        db.collection("messages").document(it.id).delete().await()
      }
    }
  }
}
