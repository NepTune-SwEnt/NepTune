package com.neptune.neptune.model

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neptune.neptune.model.project.PROJECT_ITEMS_COLLECTION_PATH
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ProjectItemsRepositoryFirebase
 *
 * REQUIREMENTS:
 * - Firestore emulator on port 8080
 * - Auth emulator on port 9099
 * - Start them: firebase emulators:start --only firestore,auth
 *
 * On Android emulator, host is 10.0.2.2
 *
 * @author Arianna Baur
 * @author Uri Jaquet
 */
@RunWith(AndroidJUnit4::class)
class ProjectItemsRepositoryFirebaseTest {

  private val host = "10.0.2.2"
  private val firestorePort = 8080
  private val authPort = 9099

  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var repo: ProjectItemsRepositoryFirestore

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

      repo = ProjectItemsRepositoryFirestore(db)
    }
  }

  @After
  fun tearDown() = runBlocking {
    // Clear projects collection
    db.collection(PROJECT_ITEMS_COLLECTION_PATH).get().await().documents.forEach {
      it.reference.delete().await()
    }
    // sign out
    auth.signOut()
  }

  @Test
  fun getNewIdReturnsNonEmptyString() {
    val id = repo.getNewId()
    assertTrue(id.isNotEmpty())
  }

  @Test
  fun addAndGetProject() = runBlocking {
    val timestamp = Timestamp.now()
    val ownerId = auth.currentUser!!.uid
    val project =
        ProjectItem(
            uid = repo.getNewId(),
            name = "Test Project",
            ownerId = ownerId,
            isStoredInCloud = true,
            lastUpdated = timestamp)

    repo.addProject(project)

    val retrieved = repo.getProject(project.uid).copy(lastUpdated = timestamp)

    assertEquals(project, retrieved)
  }

  @Test
  fun getProjectWithNonExistentIdThrowsException() = runBlocking {
    try {
      repo.getProject("non-existent-id")
      Log.i(
          "getProjectWithNonExistentIdThrowsException",
          "getProject should have thrown NoSuchElementException")
      fail("getProject should have thrown NoSuchElementException")
    } catch (_: Exception) {
      // success
    }
  }

  @Test
  fun getAllProjects() = runBlocking {
    assertTrue(repo.getAllProjects().isEmpty())

    val timestamp = Timestamp.now()

    val ownerId = auth.currentUser!!.uid
    val project1 =
        ProjectItem(
            uid = repo.getNewId(),
            name = "Project 1",
            ownerId = ownerId,
            lastUpdated = timestamp,
            isStoredInCloud = true)
    val project2 = project1.copy(uid = repo.getNewId(), name = "Project 2")

    repo.addProject(project1)
    repo.addProject(project2)

    val allProjects = repo.getAllProjects().map { it.copy(lastUpdated = timestamp) }
    assertEquals(2, allProjects.size)
    Log.i("getAllProjects", "allProjects: $allProjects")
    assertTrue(allProjects.containsAll(listOf(project1, project2)))
  }

  @Test
  fun editProject() = runBlocking {
    val timestamp = Timestamp.now()
    val ownerId = auth.currentUser!!.uid
    val originalProject =
        ProjectItem(
            uid = repo.getNewId(),
            name = "Original Name",
            ownerId = ownerId,
            isStoredInCloud = true,
            lastUpdated = timestamp)
    repo.addProject(originalProject)

    val updatedProject = originalProject.copy(name = "Updated Name")
    repo.editProject(originalProject.uid, updatedProject)

    val retrieved = repo.getProject(originalProject.uid).copy(lastUpdated = timestamp)
    assertEquals(updatedProject, retrieved)
    assertNotEquals(originalProject, retrieved)
  }

  @Test
  fun deleteProject() = runBlocking {
    val ownerId = auth.currentUser!!.uid
    val project = ProjectItem(uid = repo.getNewId(), name = "To Be Deleted", ownerId = ownerId)
    repo.addProject(project)

    assertEquals(1, repo.getAllProjects().size)

    repo.deleteProject(project.uid)

    assertTrue(repo.getAllProjects().isEmpty())

    try {
      repo.getProject(project.uid)
      fail("getProject should have thrown NoSuchElementException")
    } catch (_: Exception) {
      // success
    }
  }
}
