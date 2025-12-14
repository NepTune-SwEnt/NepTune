package com.neptune.neptune.model

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.profile.PROFILES_COLLECTION_PATH
import com.neptune.neptune.model.profile.Profile
import com.neptune.neptune.model.profile.ProfileRepositoryFirebase
import com.neptune.neptune.model.profile.TAG_WEIGHT_MAX
import com.neptune.neptune.model.profile.USERNAMES_COLLECTION_PATH
import com.neptune.neptune.model.profile.UsernameTakenException
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProfileRepositoryFirebase
 *
 * REQUIREMENTS:
 * - Firestore emulator on port 8080
 * - Auth emulator on port 9099
 * - Start them: firebase emulators:start --only firestore,auth
 *
 * On Android emulator, host is 10.0.2.2
 *
 * Partially implemented by ChatGPT
 *
 * @author Arianna Baur
 */
@RunWith(AndroidJUnit4::class)
class ProfileRepositoryFirebaseTest {

  private val host = "10.0.2.2"
  private val firestorePort = 8080
  private val authPort = 9099

  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var repo: ProfileRepositoryFirebase

  private suspend fun createUniqueUser(emailPrefix: String) {
    val uniqueEmail = "$emailPrefix-${System.nanoTime()}@example.com"
    auth.createUserWithEmailAndPassword(uniqueEmail, "password").await()
  }

  @Before
  fun setUp() {
    runBlocking {
      db = FirebaseFirestore.getInstance()
      auth = FirebaseAuth.getInstance()
      try {
        db.useEmulator(host, firestorePort)
      } catch (_: IllegalStateException) {
        "database emulator not running?"
      }

      try {
        auth.useEmulator(host, authPort)
      } catch (_: IllegalStateException) {
        "auth emulator not running?"
      }

      runCatching { auth.signOut() }
      auth.signInAnonymously().await()

      repo = ProfileRepositoryFirebase(db)
      cleanupCurrentUserDocs()
      populateFirestore()
    }
  }

  @After
  fun tearDown() {
    runBlocking {
      cleanupCurrentUserDocs()
      clearFirestore()
      cleanupAllProfilesAndUsernames()
      if (this@ProfileRepositoryFirebaseTest::auth.isInitialized) {
        runCatching { auth.signOut() }
      }
    }
  }

  private fun currentUid(): String {
    check(this::auth.isInitialized) { "Auth not initialized" }
    return auth.currentUser?.uid ?: error("No authenticated user")
  }

  private fun await(task: com.google.android.gms.tasks.Task<out Any?>): Any? = Tasks.await(task)

  private fun cleanupCurrentUserDocs() {
    if (!this::auth.isInitialized || !this::db.isInitialized) return
    val uid = auth.currentUser?.uid ?: return
    val profiles = db.collection(PROFILES_COLLECTION_PATH)
    val usernames = db.collection(USERNAMES_COLLECTION_PATH)

    runCatching { await(profiles.document(uid).delete()) }

    val mine =
        runCatching { Tasks.await(usernames.whereEqualTo("uid", uid).get()) }.getOrNull() ?: return
    for (doc in mine.documents) {
      runCatching { await(usernames.document(doc.id).delete()) }
    }
  }

  private fun cleanupAllProfilesAndUsernames() = runBlocking {
    val profiles = db.collection(PROFILES_COLLECTION_PATH)
    val usernames = db.collection(USERNAMES_COLLECTION_PATH)

    profiles.get().await().documents.forEach { profiles.document(it.id).delete().await() }

    usernames.get().await().documents.forEach { usernames.document(it.id).delete().await() }
  }

  private fun getProfileDoc() =
      Tasks.await(db.collection(PROFILES_COLLECTION_PATH).document(currentUid()).get())

  @Test
  fun ensureProfileCreatesWithDefaultsForRegisteredUser() = runBlocking {
    auth.signOut()
    createUniqueUser("defaults-test")
    val created = repo.ensureProfile(suggestedUsernameBase = "user", name = null)

    assertEquals(currentUid(), created.uid)
    assertTrue(created.username.startsWith("user"))
    assertEquals(created.username, created.name)
    assertEquals("Hello! New NepTune user here!", created.bio)
    assertFalse("A registered user should not be anonymous", created.isAnonymous)

    val snap = getProfileDoc()
    assertTrue(snap.exists())
    assertEquals(created.username, snap.getString("username"))
    assertFalse("isAnonymous flag should be false in DB", snap.getBoolean("isAnonymous") ?: true)
  }

  @Test
  fun ensureProfileCreatesWithDefaultsForAnonymousUser() = runBlocking {
    val created = repo.ensureProfile(suggestedUsernameBase = "user", name = null)

    assertEquals(currentUid(), created.uid)
    assertEquals("anonymous", created.username)
    assertEquals("anonymous", created.name)
    assertEquals("Hello! New NepTune user here!", created.bio)
    assertTrue("A guest user should be anonymous", created.isAnonymous)

    val snap = getProfileDoc()
    assertTrue(snap.exists())
    assertEquals("anonymous", snap.getString("username"))
    assertTrue("isAnonymous flag should be true in DB", snap.getBoolean("isAnonymous") ?: false)
  }

  @Test
  fun ensureProfileIsIdempotent() = runBlocking {
    val first = repo.ensureProfile("one", null)
    val second = repo.ensureProfile("two", "Someone")
    assertEquals(first.uid, second.uid)
    assertEquals(first.username, second.username)
    assertEquals(first.name, second.name)
    assertEquals(first.bio, second.bio)
  }

  @Test
  fun isUsernameAvailableRespectsOwnershipAndConflict() = runBlocking {
    // Fresh user A
    auth.signOut()
    createUniqueUser("conflict-a")
    // Make sure "neptune" starts clean
    val desired = "neptune"
    db.collection(USERNAMES_COLLECTION_PATH).document(desired).delete().await()

    assertTrue(repo.isUsernameAvailable(desired))
    repo.ensureProfile(desired, null)
    assertTrue(repo.isUsernameAvailable(desired)) // Still available to user A

    // Check that new user B cannot take it
    auth.signOut()
    createUniqueUser("conflict-b")
    repo.ensureProfile("x", null) // User B needs a profile first

    val e = runCatching { repo.setUsername(desired) }.exceptionOrNull()
    assertTrue(e is UsernameTakenException)
    assertFalse(repo.isUsernameAvailable(desired)) // Not available to user B
  }

  @Test
  fun setUsernameReleasesOldAndAllowsOthersToClaim() = runBlocking {
    // User A claims a unique name
    auth.signOut()
    createUniqueUser("release-a")
    val base = "abase_12345"
    val aProfile = repo.ensureProfile(base, null)
    val aOld = aProfile.username
    assertTrue(repo.isUsernameAvailable(aOld)) // available to its owner

    // A switches to a new username => old username should be released
    val aNew = "z_${Random.nextInt(100000, 999999)}"
    repo.setUsername(aNew)

    // Old doc should be gone now
    val oldDoc = db.collection(USERNAMES_COLLECTION_PATH).document(aOld).get().await()
    assertFalse("Old username doc should be deleted", oldDoc.exists())

    // User B can claim A's old username
    auth.signOut()
    createUniqueUser("release-b")
    repo.ensureProfile("bbase_12345", null)

    assertTrue(repo.isUsernameAvailable(aOld))
    repo.setUsername(aOld)

    // User C sees it as taken
    auth.signOut()
    createUniqueUser("release-c")
    repo.ensureProfile("cbase_12345", null)
    assertFalse(repo.isUsernameAvailable(aOld))
  }

  @Test
  fun setUsernameClaimsNewAndReleasesOldIfOwned() = runBlocking {
    auth.signOut()
    createUniqueUser("reclaim-test")

    val profile = repo.ensureProfile("tester", null)
    val original = profile.username
    val newDesired = "zz_${Random.nextInt(10000, 99999)}"
    repo.setUsername(newDesired)
    val after = repo.getCurrentProfile()!!
    assertEquals(newDesired, after.username)

    val okToReclaim = runCatching { repo.setUsername(original) }.isSuccess
    assertTrue("Should be able to reclaim original username", okToReclaim)
  }

  @Test
  fun generateDifferentUsernameWhenTaken() = runBlocking {
    val usernames = db.collection(USERNAMES_COLLECTION_PATH)

    // Simulation of user A taking username "rnd"
    auth.signOut()
    val b = auth.signInAnonymously().await().user!!.uid
    val desired = normalizeUsername("rnd")
    // Username "rnd" is taken by user A
    usernames.document(desired).set(mapOf("uid" to b)).await()

    // User B signs in and tries to get "rnd"
    auth.signOut()
    auth.signInAnonymously().await()
    val second = repo.generateRandomFreeUsername("rnd")

    assertNotEquals(desired, second)
  }

  fun normalizeUsername(u: String) = u.trim().lowercase().replace(Regex("[^a-z0-9_]"), "")

  @Test
  fun updateNameAndBioOnlyChangeThoseFields() = runBlocking {
    auth.signOut()
    createUniqueUser("update-test")

    val profile = repo.ensureProfile("bob", null)
    val originalUsername = profile.username
    assertEquals(originalUsername, profile.name)

    repo.updateName("Arianna")
    repo.updateBio("Hi there!")

    val after = repo.getCurrentProfile()!!
    assertEquals("Arianna", after.name)
    assertEquals("Hi there!", after.bio)
    assertEquals(originalUsername, after.username)
  }

  @Test
  fun removeAvatarAndUploadAvatarStubsDoNotThrow() = runBlocking {
    repo.ensureProfile("pic", null)
    runCatching { repo.removeAvatar() }.getOrThrow()
    val p1 = repo.getCurrentProfile()!!
    assertEquals("", p1.avatarUrl)

    val returned = repo.uploadAvatar(Uri.parse("file:///tmp/fake.jpg"))
    assertEquals("", returned)
    val p2 = repo.getCurrentProfile()!!
    assertEquals("", p2.avatarUrl)
  }

  @Test
  fun observeProfileEmitsNullThenProfileThenUpdatedProfile() = runBlocking {
    // Make sure we're clean and authenticated
    cleanupCurrentUserDocs()
    runCatching { auth.signOut() }
    auth.signInAnonymously().await()

    repo.observeCurrentProfile().test {
      // first emission: should be null (no doc)
      assertNull("first emission should be null when doc is missing", awaitItem())

      // create the profile: next emission
      val created = repo.ensureProfile(suggestedUsernameBase = "obstest", name = null)
      val afterCreate = awaitItem()!!
      assertEquals(created.uid, afterCreate.uid)
      assertEquals(created.username, afterCreate.username)

      // update name: next emission
      repo.updateName("New Name")
      val afterUpdate = awaitItem()!!
      assertEquals("New Name", afterUpdate.name)
      assertEquals(created.username, afterUpdate.username)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun ensureProfileUsesNumericSuffixWhenBaseAndFirstSuffixesTaken() = runBlocking {
    auth.signOut()
    createUniqueUser("suffix-test")
    val myUid = auth.currentUser!!.uid

    val base = "loopbase"
    val blocked = buildSet {
      add(base)
      addAll((1000..1003).map { "$base$it" })
    }

    val otherUid = "someone-else-${Random.nextInt(100000)}"
    val usernamesCol = db.collection(USERNAMES_COLLECTION_PATH)
    for (u in blocked) {
      usernamesCol.document(u).set(mapOf("uid" to otherUid)).await()
    }

    val profile = repo.ensureProfile(suggestedUsernameBase = base, name = null)

    assertTrue("Username should start with base", profile.username.startsWith(base))
    assertTrue(
        "Username should be suffixed (base was taken)", profile.username.length > base.length)
    assertFalse(
        "Chosen username should not be one of the blocked ones", blocked.contains(profile.username))

    val chosenDoc = usernamesCol.document(profile.username).get().await()
    assertTrue(chosenDoc.exists())
    assertEquals(myUid, chosenDoc.getString("uid"))
  }

  @Test
  fun addNewTagAddsTagOnlyOnce() = runBlocking {
    val profile = repo.ensureProfile("tagger", null)
    val uid = profile.uid

    // Add a new tag
    repo.addNewTag("rock")

    val snap1 = db.collection(PROFILES_COLLECTION_PATH).document(uid).get().await()
    val tags1 = snap1.get("tags") as? List<*> ?: emptyList<Any>()
    assertTrue("Tag 'rock' should be present", tags1.contains("rock"))

    // Add the same tag again -> should not duplicate
    repo.addNewTag("rock")
    val snap2 = db.collection(PROFILES_COLLECTION_PATH).document(uid).get().await()
    val tags2 = snap2.get("tags") as? List<*> ?: emptyList<Any>()
    assertEquals("Tags should not contain duplicates", 1, tags2.size)
  }

  @Test
  fun removeTagRemovesTagFromList() = runBlocking {
    val profile = repo.ensureProfile("tagger2", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Pre-fill a few tags
    doc.update("tags", listOf("rock", "jazz", "pop")).await()

    repo.removeTag("jazz")

    val updated = doc.get().await()
    val tags = updated.get("tags") as? List<*> ?: emptyList<Any>()
    assertFalse("Tag 'jazz' should be removed", tags.contains("jazz"))
    assertTrue("Other tags should remain", tags.containsAll(listOf("rock", "pop")))
  }

  @Test
  fun addAndRemoveMultipleTagsPersistsChangesCorrectly() = runBlocking {
    val profile = repo.ensureProfile("multiTagger", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Add multiple
    listOf("edm", "indie", "metal").forEach { repo.addNewTag(it) }
    val afterAdd = doc.get().await()
    val tagsAfterAdd = afterAdd.get("tags") as? List<*> ?: emptyList<Any>()
    assertTrue(tagsAfterAdd.containsAll(listOf("edm", "indie", "metal")))

    // Remove one
    repo.removeTag("indie")
    val afterRemove = doc.get().await()
    val tagsAfterRemove = afterRemove.get("tags") as? List<*> ?: emptyList<Any>()
    assertFalse(tagsAfterRemove.contains("indie"))
    assertTrue(tagsAfterRemove.containsAll(listOf("edm", "metal")))
  }

  @Test
  fun recordTagInteractionWithEmptyTagsOrZeroDeltaDoesNothing() = runBlocking {
    val profile = repo.ensureProfile("no-op-tags", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Pre-fill tagsWeight to detect unwanted changes
    doc.update("tagsWeight", mapOf("rock" to 10.0)).await()

    // Case 1: empty tags => should early return
    repo.recordTagInteraction(tags = emptyList(), likeDelta = 5, downloadDelta = 5)

    var snap = doc.get().await()
    var tagsWeight = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()
    assertEquals(10.0, (tagsWeight["rock"] as Number).toDouble(), 1e-6)

    // Case 2: delta == 0 => should early return
    repo.recordTagInteraction(tags = listOf("rock"), likeDelta = 0, downloadDelta = 0)

    snap = doc.get().await()
    tagsWeight = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()
    assertEquals(10.0, (tagsWeight["rock"] as Number).toDouble(), 1e-6)
  }

  @Test
  fun recordTagInteractionUpdatesAndClampsTagWeights() = runBlocking {
    val profile = repo.ensureProfile("tag-weights", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Initially: no tagsWeight field
    var snap = doc.get().await()
    val tagsWeightField = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()
    assertTrue("tagsWeight should start empty", tagsWeightField.isEmpty())

    // First call: tagsWeight map absent -> start from empty
    // Tags intentionally messy to exercise normalization + merging:
    // " Rock", "rock", "EDM " -> should become keys "rock" and "edm"
    repo.recordTagInteraction(
        tags = listOf(" Rock", "rock", "EDM "),
        likeDelta = 1, // contributes 2.0
        downloadDelta = 1 // contributes 1.0
        )
    // delta = 3.0

    snap = doc.get().await()
    var tagsWeight = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()

    // " Rock" and "rock" should merge into a single "rock" key
    assertEquals(6.0, (tagsWeight["rock"] as Number).toDouble(), 1e-6)
    assertEquals(3.0, (tagsWeight["edm"] as Number).toDouble(), 1e-6)

    // Now simulate existing weights and test clamping at TAG_WEIGHT_MAX
    doc.update("tagsWeight", mapOf("rock" to TAG_WEIGHT_MAX - 1.0, "edm" to TAG_WEIGHT_MAX)).await()

    // delta = 2 * 2 + 0 = 4.0
    repo.recordTagInteraction(tags = listOf("rock", "EDM"), likeDelta = 2, downloadDelta = 0)

    snap = doc.get().await()
    tagsWeight = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()

    // rock: (TAG_WEIGHT_MAX - 1) + 4 -> clamped to TAG_WEIGHT_MAX
    assertEquals(TAG_WEIGHT_MAX, (tagsWeight["rock"] as Number).toDouble(), 1e-6)
    // edm: TAG_WEIGHT_MAX + 4 -> stays clamped at TAG_WEIGHT_MAX
    assertEquals(TAG_WEIGHT_MAX, (tagsWeight["edm"] as Number).toDouble(), 1e-6)

    // Lower-bound clamping: weight should not go below 0.0
    doc.update("tagsWeight", mapOf("rock" to 1.0)).await()

    // delta = 2 * (-1) + (-1) = -3.0
    repo.recordTagInteraction(tags = listOf(" ROCK "), likeDelta = -1, downloadDelta = -1)

    snap = doc.get().await()
    tagsWeight = snap.get("tagsWeight") as? Map<*, *> ?: emptyMap<Any, Any>()

    // After applying -3 to 1, clamped at 0.0
    assertEquals(0.0, (tagsWeight["rock"] as Number).toDouble(), 1e-6)
  }

  @Test
  fun toProfileOrNullParsesTagsWeightAndFiltersInvalidEntries() = runBlocking {
    // Ensure a profile exists so we have a document to manipulate
    val profile = repo.ensureProfile("tags-weight-parse", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Write a deliberately messy tagsWeight map:
    // - "rock"     -> valid numeric
    // - "weird"    -> String value (non-numeric, should be dropped)
    // - "flag"     -> Boolean value (non-numeric, should be dropped)
    // - "neg"      -> valid numeric but negative (kept by toProfileOrNull)
    doc.update(
            mapOf(
                "tagsWeight" to
                    mapOf<String, Any>(
                        "rock" to 2.5,
                        "weird" to "oops",
                        "flag" to true,
                        "neg" to -1.0,
                    )))
        .await()

    val loaded = repo.getCurrentProfile()!!
    val tw = loaded.tagsWeight

    // Only (String, Number) entries become Double in tagsWeight
    assertTrue("tagsWeight should contain key 'rock'", tw.containsKey("rock"))
    assertTrue("tagsWeight should contain key 'neg'", tw.containsKey("neg"))

    // Non-numeric values should have been filtered out
    assertFalse("Non-numeric value should be dropped", tw.containsKey("weird"))
    assertFalse("Non-numeric value should be dropped", tw.containsKey("flag"))

    assertEquals(2.5, tw["rock"]!!, 1e-6)
    assertEquals(-1.0, tw["neg"]!!, 1e-6)
  }

  @Test
  fun getCurrentRecoUserProfileUsesExistingWeightsAndFiltersNegative() = runBlocking {
    val profile = repo.ensureProfile("reco-weights", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Write a tagsWeight map that will be parsed by toProfileOrNull
    doc.update(
            mapOf(
                "tagsWeight" to
                    mapOf<Any, Any>(
                        "rock" to 3.0, // valid
                        "neg" to -2.0, // negative
                        "badKey" to "x" // non-numeric
                        )))
        .await()

    val reco = repo.getCurrentRecoUserProfile()!!
    val tw = reco.tagsWeight

    // Only non-negative numeric entries from the Profile.tagsWeight should remain
    assertEquals(mapOf("rock" to 3.0), tw)
  }

  @Test
  fun getCurrentRecoUserProfileFallsBackToTagsWhenNoWeights() = runBlocking {
    val profile = repo.ensureProfile("reco-fallback-tags", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // Explicitly clear tagsWeight and set tags
    doc.update(
            mapOf(
                "tagsWeight" to emptyMap<String, Double>(),
                "tags" to listOf("rock", "jazz", "EDM")))
        .await()

    val reco = repo.getCurrentRecoUserProfile()!!
    val tw = reco.tagsWeight

    // Should give weight 1.0 to each tag
    assertEquals(mapOf("rock" to 1.0, "jazz" to 1.0, "EDM" to 1.0), tw)
  }

  @Test
  fun getCurrentRecoUserProfileReturnsEmptyWeightsWhenProfileMissing() = runBlocking {
    // Make sure we have a user but no profile document
    cleanupCurrentUserDocs()
    runCatching { auth.signOut() }
    auth.signInAnonymously().await()

    val currentUid = auth.currentUser!!.uid

    val reco = repo.getCurrentRecoUserProfile()!!
    assertEquals("UID should still be the current user", currentUid, reco.uid)
    assertTrue("tagsWeight should be empty when there is no profile", reco.tagsWeight.isEmpty())
  }

  @Test
  fun getCurrentRecoUserProfileUsesExistingWeightsAndFiltersNegativeAndNonNumeric() = runBlocking {
    val profile = repo.ensureProfile("reco-weights", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    // First, store a messy tagsWeight map in Firestore
    doc.update(
            mapOf(
                "tagsWeight" to
                    mapOf<String, Any>(
                        "rock" to 3.0, // valid
                        "neg" to -2.0, // negative
                        "bad" to "x" // non-numeric
                        )))
        .await()

    // This goes through toProfileOrNull -> Profile.tagsWeight,
    // then through getCurrentRecoUserProfile's filtering layer.
    val reco = repo.getCurrentRecoUserProfile()!!
    val tw = reco.tagsWeight

    // In getCurrentRecoUserProfile, we only keep weight >= 0
    assertEquals(mapOf("rock" to 3.0), tw)
  }

  // --- Tests for searchUsers ---

  private val testProfiles =
      listOf(
          Profile(uid = "user1", username = "testone", name = "First User", subscribers = 10L),
          Profile(uid = "user2", username = "usertwo", name = "Super Tester", subscribers = 20L),
          Profile(
              uid = "user3", username = "anotheruser", name = "User The Third", subscribers = 5L),
          Profile(
              uid = "anonUser1",
              username = "anon123",
              name = "Anonymous",
              isAnonymous = true,
              subscribers = 100L))

  private suspend fun populateFirestore() {
    testProfiles.forEach { profile ->
      db.collection(PROFILES_COLLECTION_PATH)
          .document(profile.uid)
          .set(profile.toFirestoreMap())
          .await()
      if (profile.username.isNotBlank() && !profile.isAnonymous) {
        db.collection(USERNAMES_COLLECTION_PATH)
            .document(profile.username)
            .set(mapOf("uid" to profile.uid))
            .await()
      }
    }
  }

  private suspend fun clearFirestore() {
    val profiles = db.collection(PROFILES_COLLECTION_PATH).get().await()
    profiles.forEach { it.reference.delete().await() }
    val usernames = db.collection(USERNAMES_COLLECTION_PATH).get().await()
    usernames.forEach { it.reference.delete().await() }
  }

  private fun Profile.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "uid" to uid,
        "username" to username,
        "name" to name,
        "bio" to bio,
        "avatarUrl" to avatarUrl,
        "subscribers" to subscribers,
        "subscriptions" to subscriptions,
        "likes" to likes,
        "posts" to posts,
        "tags" to tags,
        "tagsWeight" to tagsWeight,
        "following" to following,
        "isAnonymous" to isAnonymous)
  }

  @Test
  fun searchUsersWithBlankQueryReturnsAllNonAnonymousUsersSorted() = runBlocking {
    val results = repo.searchUsers("  ")

    assertEquals(3, results.size)

    assertEquals("user2", results[0].uid)
    assertEquals("user1", results[1].uid)
    assertEquals("user3", results[2].uid)
  }

  @Test
  fun searchUsersWithQueryReturnsMatchingUsersSorted() = runBlocking {
    val results = repo.searchUsers("user")
    assertEquals(3, results.size)

    assertEquals("user2", results[0].uid)
    assertEquals("user1", results[1].uid)
    assertEquals("user3", results[2].uid)
  }

  @Test
  fun searchUsersWithNameQueryReturnsMatchingUsers() = runBlocking {
    val results = repo.searchUsers("Super")

    assertEquals(1, results.size)
    assertEquals("user2", results[0].uid)
  }

  @Test
  fun searchUsersMergesUsernameAndNameResultsAndDeDuplicates() = runBlocking {
    val results = repo.searchUsers("test")

    val uids = results.map { it.uid }
    assertTrue(uids.contains("user1"))
    assertTrue(uids.contains("user2"))
    assertEquals(2, results.size)

    assertEquals("user2", results[0].uid)
    assertEquals("user1", results[1].uid)
  }

  @Test
  fun searchUsersEmptyResult() = runBlocking {
    val results = repo.searchUsers("nonexistent")
    assertTrue(results.isEmpty())
  }

  @Test
  fun searchUsersDoesNotReturnAnonymous() = runBlocking {
    val results = repo.searchUsers("anon")
    assertTrue(results.isEmpty())
  }

  @Test
  fun observeAllProfilesUpdatesOnChange() = runBlocking {
    cleanupAllProfilesAndUsernames()
    db.collection(PROFILES_COLLECTION_PATH)
        .document("u1")
        .set(mapOf("uid" to "u1", "username" to "old", "name" to "old"))
        .await()

    repo.observeAllProfiles().test {
      val first = awaitItem()
      assertEquals(1, first.size)
      assertEquals("old", first.first()!!.username)

      // update profile
      db.collection(PROFILES_COLLECTION_PATH).document("u1").update("username", "newName").await()

      val second = awaitItem()
      assertEquals("newName", second.first()!!.username)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun updatePostCountIncrementsAndDecrementsCorrectly() = runBlocking {
    auth.signOut()
    createUniqueUser("post-counter-test")
    val profile = repo.ensureProfile("poster", null)
    val uid = profile.uid
    val doc = db.collection(PROFILES_COLLECTION_PATH).document(uid)

    assertEquals("Initial post count should be 0", 0L, doc.get().await().getLong("posts"))

    repo.updatePostCount(1)
    var snap = doc.get().await()
    assertEquals("Post count should be 1", 1L, snap.getLong("posts"))

    repo.updatePostCount(5)
    snap = doc.get().await()
    assertEquals("Post count should be 6", 6L, snap.getLong("posts"))

    repo.updatePostCount(-2)
    snap = doc.get().await()
    assertEquals("Post count should be 4", 4L, snap.getLong("posts"))
  }

  @Test
  fun updateLikeCountUpdatesTargetUserField() = runBlocking {
    auth.signOut()
    createUniqueUser("target-user")
    val targetProfile = repo.ensureProfile("target", null)
    val targetUid = targetProfile.uid
    val targetDoc = db.collection(PROFILES_COLLECTION_PATH).document(targetUid)

    assertEquals(0L, targetDoc.get().await().getLong("likes"))

    auth.signOut()
    createUniqueUser("liker-user")
    repo.ensureProfile("liker", null)

    val likerUid = currentUid()
    val likerDoc = db.collection(PROFILES_COLLECTION_PATH).document(likerUid)
    assertEquals(0L, likerDoc.get().await().getLong("likes"))

    repo.updateLikeCount(targetUid, 1)

    var targetSnap = targetDoc.get().await()
    assertEquals("Target likes should increase", 1L, targetSnap.getLong("likes"))

    assertEquals("Liker likes should not change", 0L, likerDoc.get().await().getLong("likes"))

    repo.updateLikeCount(targetUid, -1)
    targetSnap = targetDoc.get().await()
    assertEquals("Target likes should decrease", 0L, targetSnap.getLong("likes"))
  }
}
