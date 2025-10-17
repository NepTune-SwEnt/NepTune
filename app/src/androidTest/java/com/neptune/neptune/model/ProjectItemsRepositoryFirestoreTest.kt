package com.neptune.neptune.model

import android.util.Log
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectItemsRepositoryFirestoreTest {

  private lateinit var repository: ProjectItemsRepositoryFirestore

  @Before
  fun setUp() = runTest {
    // Ensure we have a signed-in user in the Auth emulator
    FirebaseEmulator.auth.signInAnonymously().await()

    repository = ProjectItemsRepositoryFirestore(db = FirebaseEmulator.firestore)
    // Clear any leftover data before running tests
    FirebaseEmulator.clearFirestoreEmulator()
  }

  @After
  fun tearDown() = runTest {
    FirebaseEmulator.clearFirestoreEmulator()
    FirebaseEmulator.clearAuthEmulator()
  }

  private fun buildProject(name: String, uid: String, ownerId: String?) =
    ProjectItem(
      uid = uid,
      name = name,
      description = "desc-$name",
      isStoredInCloud = true,
      isFavorite = false,
      tags = listOf("tag1"),
      previewPath = null,
      filePath = null,
      previewUrl = null,
      fileUrl = null,
      lastUpdated = Timestamp.now(),
      ownerId = ownerId,
      collaborators = emptyList()
    )

  @Test
  fun canAddProjectsToRepository() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val uid = repository.getNewId()
    val project = buildProject("Project A", uid, owner)
    repository.addProject(project)

    val projects = repository.getAllProjects()
    assertEquals(1, projects.size)
    assertEquals(project, projects.first())
  }

  @Test
  fun addProjectWithTheCorrectID() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val uid = repository.getNewId()
    val project = buildProject("Project B", uid, owner)
    repository.addProject(project)

    val stored = repository.getProject(uid)
    assertEquals(project, stored)
  }

  @Test
  fun canAddMultipleProjectsToRepository() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val p1 = buildProject("P1", repository.getNewId(), owner)
    val p2 = buildProject("P2", repository.getNewId(), owner)
    val p3 = buildProject("P3", repository.getNewId(), owner)

    repository.addProject(p1)
    repository.addProject(p2)
    repository.addProject(p3)

    val projects = repository.getAllProjects()
    assertEquals(3, projects.size)
    val expected = setOf(p1, p2, p3)
    assertEquals(expected, projects.toSet())
  }

  @Test
  fun uidAreUniqueInTheCollection() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val uid = "duplicate-uid"
    val p1 = buildProject("A", uid, owner)
    val p2 = buildProject("B", uid, owner)

    // depending on implementation, adding duplicate uid may override or fail;
    // this test ensures only one document exists for that uid after attempts
    runCatching {
      repository.addProject(p1)
      repository.addProject(p2)
    }

    val projects = repository.getAllProjects()
    assertEquals(1, projects.size)
    assertEquals(uid, projects.first().uid)
  }

  @Test
  fun getNewIdReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val ids = (0 until numberIDs).map { repository.getNewId() }.toSet()
    assertEquals(numberIDs, ids.size)
  }

  @Test
  fun canRetrieveAProjectByID() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val p1 = buildProject("X", repository.getNewId(), owner)
    val p2 = buildProject("Y", repository.getNewId(), owner)
    repository.addProject(p1)
    repository.addProject(p2)

    val retrieved = repository.getProject(p2.uid)
    Log.i(
      "canRetrieveAProjectByIDTest",
      "Retrieved project: ${retrieved}, \nexpected: $p2"
    )
    assertEquals(p2, retrieved)
  }

  @Test
  fun canDeleteAProjectByID() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val p1 = buildProject("One", repository.getNewId(), owner)
    val p2 = buildProject("Two", repository.getNewId(), owner)
    val p3 = buildProject("Three", repository.getNewId(), owner)

    repository.addProject(p1)
    repository.addProject(p2)
    repository.addProject(p3)

    repository.deleteProject(p2.uid)

    val projects = repository.getAllProjects()
    assertEquals(2, projects.size)
    val expected = setOf(p1, p3)
    assertEquals(expected, projects.toSet())
  }

  @Test
  fun canEditAProjectByID() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val p = buildProject("Orig", repository.getNewId(), owner)
    repository.addProject(p)

    val modified = p.copy(name = "Modified Name", description = "Updated", isFavorite = true)
    repository.editProject(p.uid, modified)

    val projectsAfter = repository.getAllProjects()
    assertEquals(1, projectsAfter.size)
    assertEquals(modified, projectsAfter.first())
  }

  @Test
  fun canEditTheCorrectProjectByID() = runTest {
    val owner = FirebaseEmulator.auth.currentUser?.uid
    val p1 = buildProject("A", repository.getNewId(), owner)
    val p2 = buildProject("B", repository.getNewId(), owner)
    val p3 = buildProject("C", repository.getNewId(), owner)

    repository.addProject(p1)
    repository.addProject(p2)
    repository.addProject(p3)

    val modified = p1.copy(name = "A-modified", isFavorite = true)
    repository.editProject(p1.uid, modified)

    val all = repository.getAllProjects()
    assertEquals(3, all.size)
    val expected = setOf(modified, p2, p3)
    Log.i("canEditTheCorrectProjectByIDTest", "All expected projects:   $expected")
    Log.i("canEditTheCorrectProjectByIDTest", "All projects after edit: $all")
    assertEquals(expected, all.toSet())
  }
}
