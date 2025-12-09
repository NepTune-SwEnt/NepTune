package com.neptune.neptune.ui.messages

import androidx.lifecycle.ViewModel
import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.messages.MessageRepository
import com.neptune.neptune.model.messages.UserMessagePreview
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests for [SelectMessagesViewModel]. This has been written with the help of LLMs.
 *
 * @author Angéline Bignens
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectMessagesViewModelTest {

  private lateinit var profileRepo: ProfileRepository
  private lateinit var messageRepo: MessageRepository
  private lateinit var auth: FirebaseAuth

  private val testDispatcher = StandardTestDispatcher()
  private val scope = TestScope(testDispatcher)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    profileRepo = mockk(relaxed = true)
    messageRepo = mockk(relaxed = true)

    auth = mockk(relaxed = true)
    val fakeUser = mockk<FirebaseUser>()
    every { auth.currentUser } returns fakeUser
    every { fakeUser.isAnonymous } returns false
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Tests initial data inject */
  @Test
  fun initialUsersIsUsedWhenGiven() =
      scope.runTest {
        val initialList =
            listOf(
                UserMessagePreview(
                    profile = Profile("u1", "User1", "", ""),
                    lastMessage = "hello",
                    lastTimestamp = null,
                    isOnline = false))

        val vm =
            SelectMessagesViewModel(
                currentUid = "me",
                profileRepo = profileRepo,
                messageRepo = messageRepo,
                auth = auth,
                initialUsers = initialList)

        vm.users.test { Assert.assertEquals(initialList, awaitItem()) }
      }

  /** Tests that myself and anonymous are correctly removed of the user entries */
  @Test
  fun profilesFlowUpdateUserList() =
      scope.runTest {
        val profilesFlow = MutableSharedFlow<List<Profile?>>(replay = 1)
        every { profileRepo.observeAllProfiles() } returns profilesFlow

        every { messageRepo.observeMessagePreviews(any()) } returns flowOf(emptyList())
        every { messageRepo.observeUserOnlineState(any()) } returns flowOf(false)

        val vm = SelectMessagesViewModel("me", profileRepo, messageRepo, auth)

        vm.users.test {
          // first emission from VM is empty list
          Assert.assertEquals(emptyList<UserMessagePreview>(), awaitItem())

          val p1 = Profile("u1", "Alice", "", "")
          val p2 = Profile("me", "Self", "", "")
          val p3 = Profile("u2", "BANANA", "", "", isAnonymous = true)

          profilesFlow.emit(listOf(p1, p2, p3))
          runCurrent()

          val emitted = awaitItem()
          Assert.assertEquals(1, emitted.size)
          Assert.assertEquals("u1", emitted.first().profile.uid)
          cancel()
        }
      }

  /** Tests that message preview are correctly merged into existing users */
  @Test
  fun messagePreviewMergeIntoExistingUsers() =
      scope.runTest {
        val profilesFlow = MutableSharedFlow<List<Profile?>>(replay = 1)
        val previewsFlow = MutableSharedFlow<List<UserMessagePreview>>(replay = 1)

        every { profileRepo.observeAllProfiles() } returns profilesFlow
        every { messageRepo.observeMessagePreviews("me") } returns previewsFlow
        every { messageRepo.observeUserOnlineState(any()) } returns flowOf(false)

        val vm = SelectMessagesViewModel("me", profileRepo, messageRepo, auth)

        vm.users.test {
          awaitItem() // empty list

          val p = Profile("u1", "Alice", "", "")
          profilesFlow.emit(listOf(p))
          runCurrent()

          val firstList = awaitItem()
          Assert.assertEquals(1, firstList.size)
          Assert.assertEquals(null, firstList[0].lastMessage)

          val preview =
              UserMessagePreview(
                  profile = p, lastMessage = "Hello!", lastTimestamp = null, isOnline = false)
          previewsFlow.emit(listOf(preview))
          runCurrent()

          val updated = awaitItem()
          Assert.assertEquals("Hello!", updated[0].lastMessage)
          cancel()
        }
      }

  /** Tests that the online state updates propagate */
  @Test
  fun onlineStatusUpdate() =
      scope.runTest {
        val profilesFlow = MutableSharedFlow<List<Profile?>>(replay = 1)
        val onlineFlow = MutableSharedFlow<Boolean>(replay = 1)

        every { profileRepo.observeAllProfiles() } returns profilesFlow
        every { messageRepo.observeMessagePreviews("me") } returns flowOf(emptyList())
        every { messageRepo.observeUserOnlineState("u1") } returns onlineFlow

        val vm = SelectMessagesViewModel("me", profileRepo, messageRepo, auth)

        vm.users.test {
          awaitItem() // empty

          val p = Profile("u1", "Alice", "", "")
          profilesFlow.emit(listOf(p))
          runCurrent()

          val first = awaitItem()
          Assert.assertEquals(false, first[0].isOnline)

          onlineFlow.emit(true)
          runCurrent()

          Assert.assertEquals(true, awaitItem()[0].isOnline)
          cancel()
        }
      }

  /** Tests that it correctly combine the profile,message and online events */
  @Test
  fun combineEvents() =
      scope.runTest {
        val profilesFlow = MutableSharedFlow<List<Profile?>>(replay = 1)
        val previewsFlow = MutableSharedFlow<List<UserMessagePreview>>(replay = 1)
        val onlineFlow = MutableSharedFlow<Boolean>(replay = 1)

        every { profileRepo.observeAllProfiles() } returns profilesFlow
        every { messageRepo.observeMessagePreviews("me") } returns previewsFlow
        every { messageRepo.observeUserOnlineState("u1") } returns onlineFlow

        val vm = SelectMessagesViewModel("me", profileRepo, messageRepo, auth)

        vm.users.test {
          awaitItem()

          val p = Profile("u1", "Alice", "", "")
          profilesFlow.emit(listOf(p))
          runCurrent()

          val a = awaitItem()
          Assert.assertEquals(false, a[0].isOnline)
          Assert.assertEquals(null, a[0].lastMessage)

          onlineFlow.emit(true)
          runCurrent()

          val b = awaitItem()
          Assert.assertEquals(true, b[0].isOnline)

          val prev =
              UserMessagePreview(
                  profile = p, lastMessage = "yo", lastTimestamp = null, isOnline = true)
          previewsFlow.emit(listOf(prev))
          runCurrent()

          val c = awaitItem()
          Assert.assertEquals("yo", c[0].lastMessage)
          Assert.assertEquals(true, c[0].isOnline)
          cancel()
        }
      }

  /** Test that the SelectMessagesFactory correctly throws an exception */
  @Test
  fun factoryThrowsException() {
    val factory = SelectMessagesViewModelFactory("uid")

    class UnknownViewModel : ViewModel()

    val exception =
        Assert.assertThrows(IllegalArgumentException::class.java) {
          factory.create(UnknownViewModel::class.java)
        }

    Assert.assertEquals(
        "Unknown ViewModel class: ${UnknownViewModel::class.java.name}", exception.message)
  }
}
