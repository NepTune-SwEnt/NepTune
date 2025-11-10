package com.neptune.neptune.model.project

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TotalProjectItemsRepositoryComposeTest {

  private lateinit var totalRepository: TotalProjectItemsRepositoryCompose
  private lateinit var localRepo: ProjectItemsRepository
  private lateinit var cloudRepo: ProjectItemsRepository

  @Before
  fun setUp() {
    localRepo = ProjectItemsRepositoryVar()
    cloudRepo = ProjectItemsRepositoryVar()
    totalRepository = TotalProjectItemsRepositoryCompose(localRepo, cloudRepo)
  }

  @Test
  fun getNewIdLocalReturnsIdFromLocalRepo() {
    assertThat(totalRepository.getNewIdLocal()).isEqualTo("0")
    assertThat(totalRepository.getNewIdLocal()).isEqualTo("1")
  }

  @Test
  fun getNewIdCloudReturnsIdFromCloudRepo() {
    assertThat(totalRepository.getNewIdCloud()).isEqualTo("0")
    assertThat(totalRepository.getNewIdCloud()).isEqualTo("1")
  }

  @Test
  fun addProjectAddsToBothRepos() = runTest {
    val project = ProjectItem(uid = "1", name = "Test Project")
    totalRepository.addProject(project)
    assertThat(localRepo.getAllProjects()).contains(project)
    assertThat(cloudRepo.getAllProjects()).contains(project)
  }

  @Test
  fun editProjectEditsBothRepos() = runTest {
    val project = ProjectItem(uid = "1", name = "Test Project")
    totalRepository.addProject(project)
    val editedProject = project.copy(name = "Edited Project")
    totalRepository.editProject("1", editedProject)
    assertThat(localRepo.getProject("1").name).isEqualTo("Edited Project")
    assertThat(cloudRepo.getProject("1").name).isEqualTo("Edited Project")
  }

  @Test
  fun deleteProjectDeletesFromBothRepos() = runTest {
    val project = ProjectItem(uid = "1", name = "Test Project")
    totalRepository.addProject(project)
    totalRepository.deleteProject("1")
    assertThat(localRepo.getAllProjects()).isEmpty()
    assertThat(cloudRepo.getAllProjects()).isEmpty()
  }
}
