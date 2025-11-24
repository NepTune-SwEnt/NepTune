package com.neptune.neptune.model.profile

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ProfileRepositoryFirebaseFollowTest {

  @RelaxedMockK private lateinit var firestore: FirebaseFirestore
  @RelaxedMockK private lateinit var functions: FirebaseFunctions
  @RelaxedMockK private lateinit var callable: HttpsCallableReference
  @RelaxedMockK private lateinit var auth: FirebaseAuth
  @RelaxedMockK private lateinit var callableResult: HttpsCallableResult

  private lateinit var repo: ProfileRepositoryFirebase
  private val currentUser: FirebaseUser = mockk()

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    every { currentUser.uid } returns CURRENT_UID
    every { auth.currentUser } returns currentUser
    every { functions.getHttpsCallable("followUser") } returns callable

    repo = ProfileRepositoryFirebase(firestore, functions, auth = auth)
  }

  @Test
  fun followUserPassesTargetAndFollowTrue() = runBlocking {
    val payloadSlot = slot<Any>()
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(callableResult)

    repo.followUser("target-123")

    val data = payloadSlot.captured as Map<*, *>
    assertEquals("target-123", data["targetUid"])
    assertEquals(true, data["follow"])
    verify(exactly = 1) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun unfollowUserPassesFollowFalse() = runBlocking {
    val payloadSlot = slot<Any>()
    every { callable.call(capture(payloadSlot)) } returns Tasks.forResult(callableResult)

    repo.unfollowUser("another")

    val data = payloadSlot.captured as Map<*, *>
    assertEquals("another", data["targetUid"])
    assertEquals(false, data["follow"])
    verify(exactly = 1) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun followUserRejectsBlankUid() {
    runBlocking {
      assertFailsWith<IllegalArgumentException> { repo.followUser("   ") }
    }
    verify(exactly = 0) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun followUserRejectsSelfFollow() {
    runBlocking {
      assertFailsWith<IllegalArgumentException> { repo.followUser(CURRENT_UID) }
    }
    verify(exactly = 0) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun followUserPropagatesCallableException() {
    val payloadSlot = slot<Any>()
    every { callable.call(capture(payloadSlot)) } returns
            Tasks.forException(IllegalStateException("firestore down"))

    runBlocking {
      assertFailsWith<IllegalStateException> { repo.followUser("abc") }
    }
  }


  companion object {
    private const val CURRENT_UID = "current-user"
  }
}
