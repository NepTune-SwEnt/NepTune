package com.neptune.neptune.model

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditToDoFirestoreEmulatedTest : FirestoreBootcampTest(BootcampMilestone.B2) {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var initialRepositoryContent: List<ToDo>

  @Before
  override fun setUp() {
    super.setUp()
    initialRepositoryContent = listOf<ToDo>(todo1, todo2)
    runTest {
      repository.addTodo(todo1)
      repository.addTodo(todo2)
    }

    composeTestRule.setContent { BootcampApp() }
  }

  @Test
  fun editingATodoUpdateTheRepositoryContent() {
    val editedTodo1 =
        todo1.copy(
            name = "This is the new name",
            description = "New description",
            assigneeName = "New assignee",
            dueDate = Timestamp.fromDate(2025, Calendar.DECEMBER, 24),
            location = Location(46.5210, 6.5790, "New location"))
    composeTestRule.navigateToEditToDoScreen(todo1)
    composeTestRule.enterEditTodoDetails(editedTodo1)
    composeTestRule.clickOnSaveForEditTodo(waitForRedirection = true)
    runTest {
      val storedTodos = repository.getAllTodos()
      assertEquals(storedTodos.size, initialRepositoryContent.size)
      val storedEditedTodo = storedTodos.find { it.name == editedTodo1.name }
      assertNotNull(storedEditedTodo)
      assertTrue(editedTodo1.b2Equals(storedEditedTodo!!))
    }
  }

  @Test
  fun canDeleteTodo() {
    composeTestRule.navigateToEditToDoScreen(editedToDo = todo1)
    composeTestRule.clickOnDeleteForEditTodo(waitForRedirection = true)
    runTest {
      val storedTodos = repository.getAllTodos()
      assertEquals(storedTodos.size, initialRepositoryContent.size - 1)
      val deletedTodo = storedTodos.find { it.name == todo1.name }
      Assert.assertNull(deletedTodo)
    }
  }
}
