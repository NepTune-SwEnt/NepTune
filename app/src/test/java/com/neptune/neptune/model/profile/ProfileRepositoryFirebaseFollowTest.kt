package com.neptune.neptune.model.profile

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
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
    runBlocking { assertFailsWith<IllegalArgumentException> { repo.followUser("   ") } }
    verify(exactly = 0) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun followUserRejectsSelfFollow() {
    runBlocking { assertFailsWith<IllegalArgumentException> { repo.followUser(CURRENT_UID) } }
    verify(exactly = 0) { functions.getHttpsCallable("followUser") }
  }

  @Test
  fun followUserPropagatesCallableException() {
    val payloadSlot = slot<Any>()
    every { callable.call(capture(payloadSlot)) } returns
        Tasks.forException(IllegalStateException("firestore down"))

    runBlocking { assertFailsWith<IllegalStateException> { repo.followUser("abc") } }
  }

  @Test
  fun getFollowingIdsReturnsListFromFirestore() = runBlocking {
    val expectedIds = listOf("id1", "id2", "id3")
    val docRef = mockk<DocumentReference>()
    val collection = mockk<CollectionReference>()
    val querySnapshot = mockk<QuerySnapshot>()
    val doc1 = mockk<QueryDocumentSnapshot>()
    val doc2 = mockk<QueryDocumentSnapshot>()
    val doc3 = mockk<QueryDocumentSnapshot>()

    every { firestore.collection("profiles").document(CURRENT_UID) } returns docRef
    every { docRef.collection("following") } returns collection
    every { collection.get() } returns Tasks.forResult(querySnapshot)
    every { querySnapshot.documents } returns listOf(doc1, doc2, doc3)
    every { doc1.id } returns "id1"
    every { doc1.getString("uid") } returns null
    every { doc2.id } returns "id2"
    every { doc2.getString("uid") } returns null
    every { doc3.id } returns "id3"
    every { doc3.getString("uid") } returns null

    val followingIds = repo.getFollowingIds(uid = CURRENT_UID)

    assertEquals(expectedIds, followingIds)
    verify(exactly = 1) { firestore.collection("profiles").document(CURRENT_UID) }
    verify(exactly = 1) { docRef.collection("following") }
  }

  @Test
  fun getFollowersIdsReturnsListFromFirestore() = runBlocking {
    val expectedIds = listOf("idA", "idB")
    val docRef = mockk<DocumentReference>()
    val collection = mockk<CollectionReference>()
    val querySnapshot = mockk<QuerySnapshot>()
    val docA = mockk<QueryDocumentSnapshot>()
    val docB = mockk<QueryDocumentSnapshot>()

    every { firestore.collection("profiles").document(CURRENT_UID) } returns docRef
    every { docRef.collection("followers") } returns collection
    every { collection.get() } returns Tasks.forResult(querySnapshot)
    every { querySnapshot.documents } returns listOf(docA, docB)
    every { docA.id } returns "idA"
    every { docA.getString("uid") } returns null
    every { docB.id } returns "idB"
    every { docB.getString("uid") } returns null

    val followerIds = repo.getFollowersIds(uid = CURRENT_UID)

    assertEquals(expectedIds, followerIds)
    verify(exactly = 1) { firestore.collection("profiles").document(CURRENT_UID) }
    verify(exactly = 1) { docRef.collection("followers") }
  }

  companion object {
    private const val CURRENT_UID = "current-user"
  }
}
