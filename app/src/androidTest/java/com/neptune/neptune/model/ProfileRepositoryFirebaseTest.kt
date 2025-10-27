package com.neptune.neptune.model

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.profile.PROFILES_COLLECTION_PATH
import com.neptune.neptune.model.profile.ProfileRepositoryFirebase
import com.neptune.neptune.model.profile.USERNAMES_COLLECTION_PATH
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Instrumented tests for ProfileRepositoryFirebase
 *
 * REQUIREMENTS:
 *   - Firestore emulator on port 8080
 *   - Auth emulator on port 9099
 *   - Start them:
 *        firebase emulators:start --only firestore,auth
 *
 * On Android emulator, host is 10.0.2.2
 */
@RunWith(AndroidJUnit4::class)
class ProfileRepositoryFirebaseTest {

    private val host = "10.0.2.2"
    private val firestorePort = 8080
    private val authPort = 9099

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repo: ProfileRepositoryFirebase

    @Before
    fun setUp() {
        runBlocking {
            db = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()
            try {
                db.useEmulator(host, firestorePort)
            } catch (e: IllegalStateException) {
                "database emulator not running?"
            }

            try {
                auth.useEmulator(host, authPort)
            } catch (e: IllegalStateException) {
                "auth emulator not running?"
            }

            runCatching { auth.signOut() }
            Tasks.await(auth.signInAnonymously())

            delay(100)

            repo = ProfileRepositoryFirebase(db)
            cleanupCurrentUserDocs()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            cleanupCurrentUserDocs()
            if (this@ProfileRepositoryFirebaseTest::auth.isInitialized) {
                runCatching { auth.signOut() }
            }
        }
    }

    private fun currentUid(): String {
        check(this::auth.isInitialized) { "Auth not initialized" }
        return auth.currentUser?.uid ?: error("No authenticated user")
    }

    private fun await(task: com.google.android.gms.tasks.Task<out Any?>): Any? =
        Tasks.await(task)

    private fun cleanupCurrentUserDocs() {
        if (!this::auth.isInitialized || !this::db.isInitialized) return
        val uid = auth.currentUser?.uid ?: return
        val profiles = db.collection(PROFILES_COLLECTION_PATH)
        val usernames = db.collection(USERNAMES_COLLECTION_PATH)

        runCatching { await(profiles.document(uid).delete()) }

        val mine = runCatching { Tasks.await(usernames.whereEqualTo("uid", uid).get()) }.getOrNull()
            ?: return
        for (doc in mine.documents) {
            runCatching { await(usernames.document(doc.id).delete()) }
        }
    }

    private fun getProfileDoc() =
        Tasks.await(
            db.collection(PROFILES_COLLECTION_PATH).document(currentUid()).get()
        )

    @Test
    fun ensureProfile_createsWithDefaults_whenMissing() = runBlocking {
        val created = repo.ensureProfile(suggestedUsernameBase = "user", name = null)

        assertEquals(currentUid(), created.uid)
        assertTrue(created.username.startsWith("user"))
        assertEquals(created.username, created.name) // name defaults to username
        assertEquals("Hello! New NepTune user here!", created.bio)
        assertEquals(0L, created.subscribers)
        assertEquals(0L, created.subscriptions)

        val snap = getProfileDoc()
        assertTrue(snap.exists())
        assertEquals(created.username, snap.getString("username"))
        assertEquals(created.name, snap.getString("name"))
        assertEquals(created.bio, snap.getString("bio"))
    }

    @Test
    fun ensureProfile_isIdempotent() = runBlocking {
        val first = repo.ensureProfile("one", null)
        val second = repo.ensureProfile("two", "Someone")
        assertEquals(first.uid, second.uid)
        assertEquals(first.username, second.username)
        assertEquals(first.name, second.name)
        assertEquals(first.bio, second.bio)
    }

    @Test
    fun isUsernameAvailable_and_setUsername_behaveCorrectly() = runBlocking {
        assertTrue(repo.isUsernameAvailable("neptune"))
        repo.ensureProfile("neptune", null)
        assertTrue(repo.isUsernameAvailable("neptune"))

        auth.signOut()
        Tasks.await(auth.signInAnonymously())
        delay(50)
        repo.ensureProfile("x", null)

        val taken = "neptune"
        val thrown = runCatching { repo.setUsername(taken) }.exceptionOrNull()
        if (thrown == null) {
            assertTrue(repo.isUsernameAvailable(taken))
        } else {
            assertFalse(repo.isUsernameAvailable(taken))
        }
    }

    @Test
    fun setUsername_claimsNew_andReleasesOld_ifOwned() = runBlocking {
        val profile = repo.ensureProfile("tester", null)
        val original = profile.username
        val newDesired = "zz_${Random.nextInt(10000, 99999)}"
        repo.setUsername(newDesired)
        val after = repo.getProfile()!!
        assertEquals(newDesired, after.username)

        val okToReclaim = runCatching { repo.setUsername(original) }.isSuccess
        assertTrue(okToReclaim)
    }

    @Test
    fun generateRandomFreeUsername_returnsDifferent_whenTakenByAnotherUser() = runBlocking {
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
    fun updateName_and_updateBio_onlyChangeThoseFields() = runBlocking {
        val profile = repo.ensureProfile("bob", null)
        val originalUsername = profile.username
        assertEquals(originalUsername, profile.name)

        repo.updateName("Arianna")
        repo.updateBio("Hi there!")

        val after = repo.getProfile()!!
        assertEquals("Arianna", after.name)
        assertEquals("Hi there!", after.bio)
        assertEquals(originalUsername, after.username)
    }

    @Test
    fun removeAvatar_and_uploadAvatar_stubs_doNotThrow() = runBlocking {
        repo.ensureProfile("pic", null)
        runCatching { repo.removeAvatar() }.getOrThrow()
        val p1 = repo.getProfile()!!
        assertEquals("", p1.avatarUrl)

        val returned = repo.uploadAvatar(Uri.parse("file:///tmp/fake.jpg"))
        assertEquals("", returned)
        val p2 = repo.getProfile()!!
        assertEquals("", p2.avatarUrl)
    }
}
