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
  fun getNewIdReturnsUniqueIds() {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()
    assertNotEquals(id1, id2)
  }

  @Test
  fun addProjectAddsProjectSuccessfully() = runBlocking {
    val project = ProjectItem(uid = "1", name = "Project 1")
    repository.addProject(project)
    val projects = repository.getAllProjects()
    assertTrue(projects.contains(project))
  }

  @Test(expected = Exception::class)
  fun addProjectThrowsExceptionWhenIdAlreadyExists() = runBlocking {
    val project = ProjectItem(uid = "1", name = "Project 1")
    repository.addProject(project)
    repository.addProject(project)
  }

  @Test
  fun editProjectUpdatesProjectSuccessfully() = runBlocking {
    val project = ProjectItem(uid = "1", name = "Project 1")
    repository.addProject(project)
    val updatedProject = ProjectItem(uid = "1", name = "Updated Project 1")
    repository.editProject("1", updatedProject)
    val retrievedProject = repository.getProject("1")
    assertEquals("Updated Project 1", retrievedProject.name)
  }

  @Test(expected = Exception::class)
  fun editProjectThrowsExceptionWhenProjectNotFound() = runBlocking {
    repository.editProject("1", ProjectItem(uid = "1", name = "Nonexistent Project"))
  }

  @Test
  fun deleteProjectRemovesProjectSuccessfully() = runBlocking {
    val project = ProjectItem(uid = "1", name = "Project 1")
    repository.addProject(project)
    repository.deleteProject("1")
    val projects = repository.getAllProjects()
    assertFalse(projects.contains(project))
  }

  @Test(expected = Exception::class)
  fun deleteProjectThrowsExceptionWhenProjectNotFound() = runBlocking {
    repository.deleteProject("1")
  }

  @Test
  fun getProjectReturnsCorrectProject() = runBlocking {
    val project = ProjectItem(uid = "1", name = "Project 1")
    repository.addProject(project)
    val retrievedProject = repository.getProject("1")
    assertEquals(project, retrievedProject)
  }

  @Test(expected = Exception::class)
  fun getProjectThrowsExceptionWhenProjectNotFound(): Unit = runBlocking {
    repository.getProject("1")
  }

  @Test
  fun getAllProjectsReturnsAllAddedProjects() = runBlocking {
    val project1 = ProjectItem(uid = "1", name = "Project 1")
    val project2 = ProjectItem(uid = "2", name = "Project 2")
    repository.addProject(project1)
    repository.addProject(project2)
    val projects = repository.getAllProjects()
    assertEquals(2, projects.size)
    assertTrue(projects.contains(project1))
    assertTrue(projects.contains(project2))
  }
}
