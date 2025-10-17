package com.neptune.neptune.model

import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryVar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProjectItemsRepositoryVarTest {

  private lateinit var repository: ProjectItemsRepositoryVar

  @Before
  fun setup() {
    repository = ProjectItemsRepositoryVar()
  }

  @Test
  fun getNewId_returnsUniqueIds() {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()
    assertNotEquals(id1, id2)
  }

  @Test
  fun addProject_addsProjectSuccessfully() = runBlocking {
    val project = ProjectItem(id = "1", name = "Project 1")
    repository.addProject(project)
    val projects = repository.getAllProjects()
    assertTrue(projects.contains(project))
  }

  @Test(expected = Exception::class)
  fun addProject_throwsExceptionWhenIdAlreadyExists() = runBlocking {
    val project = ProjectItem(id = "1", name = "Project 1")
    repository.addProject(project)
    repository.addProject(project)
  }

  @Test
  fun editProject_updatesProjectSuccessfully() = runBlocking {
    val project = ProjectItem(id = "1", name = "Project 1")
    repository.addProject(project)
    val updatedProject = ProjectItem(id = "1", name = "Updated Project 1")
    repository.editProject("1", updatedProject)
    val retrievedProject = repository.getProject("1")
    assertEquals("Updated Project 1", retrievedProject.name)
  }

  @Test(expected = Exception::class)
  fun editProject_throwsExceptionWhenProjectNotFound() = runBlocking {
    repository.editProject("1", ProjectItem(id = "1", name = "Nonexistent Project"))
  }

  @Test
  fun deleteProject_removesProjectSuccessfully() = runBlocking {
    val project = ProjectItem(id = "1", name = "Project 1")
    repository.addProject(project)
    repository.deleteProject("1")
    val projects = repository.getAllProjects()
    assertFalse(projects.contains(project))
  }

  @Test(expected = Exception::class)
  fun deleteProject_throwsExceptionWhenProjectNotFound() = runBlocking {
    repository.deleteProject("1")
  }

  @Test
  fun getProject_returnsCorrectProject() = runBlocking {
    val project = ProjectItem(id = "1", name = "Project 1")
    repository.addProject(project)
    val retrievedProject = repository.getProject("1")
    assertEquals(project, retrievedProject)
  }

  @Test(expected = Exception::class)
  fun getProject_throwsExceptionWhenProjectNotFound(): Unit = runBlocking {
    repository.getProject("1")
  }

  @Test
  fun getAllProjects_returnsAllAddedProjects() = runBlocking {
    val project1 = ProjectItem(id = "1", name = "Project 1")
    val project2 = ProjectItem(id = "2", name = "Project 2")
    repository.addProject(project1)
    repository.addProject(project2)
    val projects = repository.getAllProjects()
    assertEquals(2, projects.size)
    assertTrue(projects.contains(project1))
    assertTrue(projects.contains(project2))
  }
}
