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
    assertThat(allProjects.size).isEqualTo(2)
    assertThat(allProjects).contains(project1)
    assertThat(allProjects).contains(project2)
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
    } catch (_: NoSuchElementException) {
      // success
    }
  }

  @Test
  fun deleteProject_deletesLocalFiles() = runBlocking {
    val projectId = repository.getNewId()

    // Create temp files representing the project zip, audio preview and image preview
    val zipFile = File(context.filesDir, "test_project_$projectId.zip")
    val audioFile = File(context.filesDir, "preview_$projectId.mp3")
    val imageFile = File(context.filesDir, "img_$projectId.png")

    zipFile.writeText("zip")
    audioFile.writeText("audio")
    imageFile.writeText("img")

    // Sanity checks
    assertThat(zipFile.exists()).isTrue()
    assertThat(audioFile.exists()).isTrue()
    assertThat(imageFile.exists()).isTrue()

    // Add project referencing the files using absolute paths
    val project = ProjectItem(
      uid = projectId,
      name = "Project with files",
      projectFileLocalPath = zipFile.absolutePath,
      audioPreviewLocalPath = audioFile.absolutePath,
      imagePreviewLocalPath = imageFile.absolutePath
    )
    repository.addProject(project)

    // Delete project
    repository.deleteProject(projectId)

    // Files should be deleted
    assertThat(zipFile.exists()).isFalse()
    assertThat(audioFile.exists()).isFalse()
    assertThat(imageFile.exists()).isFalse()

    // Project should be removed
    try {
      repository.getProject(projectId)
      Assert.fail("Expected NoSuchElementException after deletion")
    } catch (_: NoSuchElementException) {
      // success
    }
  }
}
