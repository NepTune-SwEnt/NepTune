package com.neptune.neptune.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import java.io.File
import java.util.NoSuchElementException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectItemsRepositoryLocalTest {

  private lateinit var repository: ProjectItemsRepositoryLocal
  private lateinit var context: Context
  private lateinit var projectsFile: File

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    projectsFile = File(context.filesDir, "projects.json")
    if (projectsFile.exists()) {
      projectsFile.delete()
    }
    repository = ProjectItemsRepositoryLocal(context)
  }

  @After
  fun teardown() {
    if (projectsFile.exists()) {
      projectsFile.delete()
    }
  }

  @Test
  fun addAndGetProject() = runBlocking {
    val projectId = repository.getNewId()
    val project = ProjectItem(uid = projectId, name = "Test Project")
    repository.addProject(project)

    val retrievedProject = repository.getProject(projectId)
    assertThat(retrievedProject).isEqualTo(project)
  }

  @Test
  fun getAllProjects() = runBlocking {
    val project1 = ProjectItem(uid = repository.getNewId(), name = "Test Project 1")
    val project2 = ProjectItem(uid = repository.getNewId(), name = "Test Project 2")
    repository.addProject(project1)
    repository.addProject(project2)

    val allProjects = repository.getAllProjects()
    assertThat(allProjects).containsAnyIn(listOf(project1, project2))
  }

  @Test
  fun editProject() = runBlocking {
    val projectId = repository.getNewId()
    val originalProject = ProjectItem(uid = projectId, name = "Original Project")
    repository.addProject(originalProject)

    val editedProject = originalProject.copy(name = "Edited Project")
    repository.editProject(projectId, editedProject)

    val retrievedProject = repository.getProject(projectId)
    assertThat(retrievedProject).isEqualTo(editedProject)
  }

  @Test
  fun deleteProject() = runBlocking {
    val projectId = repository.getNewId()
    val project = ProjectItem(uid = projectId, name = "Test Project")
    repository.addProject(project)

    repository.deleteProject(projectId)

    try {
      repository.getProject(projectId)
      Assert.fail("Expected NoSuchElementException")
    } catch (e: NoSuchElementException) {
      // success
    }
  }
}
