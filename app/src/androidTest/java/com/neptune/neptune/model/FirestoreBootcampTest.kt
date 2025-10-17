package com.neptune.neptune.model

import android.util.Log
import com.github.se.bootcamp.model.todo.TODOS_COLLECTION_PATH
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.github.se.bootcamp.model.todo.ToDosRepositoryFirestore
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.neptune.neptune.model.project.PROJECTITEMS_COLLECTION_PATH
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreBootcampTest : BootcampTest {

  suspend fun getTodosCount(): Int {
    val user = FirebaseEmulator.auth.currentUser ?: return 0
    return FirebaseEmulator.firestore
        .collection(PROJECTITEMS_COLLECTION_PATH)
        .whereEqualTo("ownerId", user.uid)
        .get()
        .await()
        .size()
  }

  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val todos =
        FirebaseEmulator.firestore
            .collection(PROJECTITEMS_COLLECTION_PATH)
            .whereEqualTo("ownerId", user.uid)
            .get()
            .await()

    val batch = FirebaseEmulator.firestore.batch()
    todos.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getTodosCount() == 0) {
      "Test collection is not empty after clearing, count: ${getTodosCount()}"
    }
  }

  override fun createInitializedRepository(): ProjectItemsRepository {
    return ProjectItemsRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  override val project1: ProjectItem
    get() = super.project1.copy(ownerId = currentUser.uid)

  override val todo2: ProjectItem
    get() = super.todo2.copy(ownerId = currentUser.uid)

  override val todo3: ProjectItem
    get() = super.todo3.copy(ownerId = currentUser.uid)

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      val todosCount = getTodosCount()
      if (todosCount > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $todosCount",
        )
        clearTestCollection()
      }
    }
  }

  @After
  override fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
