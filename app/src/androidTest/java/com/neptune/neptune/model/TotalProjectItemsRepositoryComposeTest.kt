package com.neptune.neptune.model

import com.google.common.truth.Truth
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryVar
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryCompose
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TotalProjectItemsRepositoryComposeTest {

  private lateinit var totalRepository: TotalProjectItemsRepositoryCompose
  private lateinit var localRepo: ProjectItemsRepository
  private lateinit var cloudRepo: ProjectItemsRepository
  private val timestamp1 = Timestamp.now()
  private val timestamp2 = Timestamp.now()
  private val project1 = ProjectItem(uid = "1", name = "Test Project 1", lastUpdated = timestamp1)
  private val project2 = ProjectItem(uid = "2", name = "Test Project 2", lastUpdated = timestamp2)

  @Before
  fun setUp() {
    localRepo = ProjectItemsRepositoryVar()
    cloudRepo = ProjectItemsRepositoryVar()
    totalRepository = TotalProjectItemsRepositoryCompose(localRepo, cloudRepo)
    mockkStatic(Firebase::class)
    val firebaseAuth = mockk<FirebaseAuth>()
    val firebaseUser = mockk<FirebaseUser>()
    every { firebaseAuth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns "test_user_id"
  }

  @Test
  fun getNewIdReturnDifferent() {
    val id1 = totalRepository.getNewIdLocal()
    val id2 = totalRepository.getNewIdLocal()
    Truth.assertThat(id1).isNotEqualTo(id2)
    val id3 = totalRepository.getNewIdCloud()
    val id4 = totalRepository.getNewIdCloud()
    Truth.assertThat(id3).isNotEqualTo(id4)
    Truth.assertThat(id3).isNotEqualTo(id1)
    Truth.assertThat(id4).isNotEqualTo(id2)
    Truth.assertThat(id3).isNotEqualTo(id2)
    Truth.assertThat(id4).isNotEqualTo(id1)
  }

  @Test
  fun addProjectAddsToBothRepos() = runTest {
    totalRepository.addProject(project1)
    Truth.assertThat(localRepo.getAllProjects()).contains(project1)
    Truth.assertThat(cloudRepo.getAllProjects()).contains(project1)
  }

  @Test
  fun editProjectEditsBothRepos() = runTest {
    totalRepository.addProject(project1)
    val editedProject = project1.copy(name = "Edited Project")
    totalRepository.editProject("1", editedProject)
    Truth.assertThat(localRepo.getProject("1").name).isEqualTo("Edited Project")
    Truth.assertThat(cloudRepo.getProject("1").name).isEqualTo("Edited Project")
  }

  @Test
  fun deleteProjectDeletesFromBothRepos() = runTest {
    totalRepository.addProject(project1)
    totalRepository.deleteProject("1")
    Truth.assertThat(localRepo.getAllProjects()).isEmpty()
    Truth.assertThat(cloudRepo.getAllProjects()).isEmpty()
  }

  @Test
  fun getAllProjectsMergesLocalAndCloud() = runTest {
    val timestamp3 = Timestamp.now()
    val localProject = project1.copy(lastUpdated = timestamp3)
    val cloudProject = project1.copy(name = "Cloud Project", lastUpdated = timestamp3)
    val cloudOnlyProject = project2.copy(isStoredInCloud = true)

    localRepo.addProject(localProject)
    cloudRepo.addProject(cloudProject)
    cloudRepo.addProject(cloudOnlyProject)

    val mergedProjects = totalRepository.getAllProjects()
    Truth.assertThat(mergedProjects).hasSize(2)
    Truth.assertThat(mergedProjects.find { it.uid == "1" }?.name)
        .isEqualTo("Test Project 1") // local is newer
    Truth.assertThat(mergedProjects.find { it.uid == "1" }?.isStoredInCloud)
        .isTrue() // local is newer
    Truth.assertThat(mergedProjects.find { it.uid == "2" }?.name).isEqualTo("Test Project 2")
  }

  @Test
  fun getProjectReturnsCorrectMergedProject() = runTest {
    val timestamp2 = Timestamp.now()
    val timestamp3 = Timestamp.now()
    val localProject = project1.copy(lastUpdated = timestamp3)
    val cloudProject = project1.copy(name = "Cloud Project", lastUpdated = timestamp2)
    localRepo.addProject(localProject)
    cloudRepo.addProject(cloudProject)

    val project = totalRepository.getProject("1")
    Truth.assertThat(project.name).isEqualTo("Test Project 1")
    Truth.assertThat(project.isStoredInCloud).isTrue()
  }

  @Test
  fun getAllLocalProjectsReturnsOnlyLocalProjects() = runTest {
    localRepo = mockk()
    totalRepository = TotalProjectItemsRepositoryCompose(localRepo, cloudRepo)
    coEvery { localRepo.getAllProjects() } returns listOf(project1, project2)

    val localProjects = totalRepository.getAllLocalProjects()

    Truth.assertThat(localProjects).containsExactly(project1, project2)
  }

  @Test
  fun getAllCloudProjectsReturnsOnlyCloudProjects() = runTest {
    cloudRepo = mockk()
    totalRepository = TotalProjectItemsRepositoryCompose(localRepo, cloudRepo)
    coEvery { cloudRepo.getAllProjects() } returns listOf(project1, project2)

    val cloudProjects = totalRepository.getAllCloudProjects()

    Truth.assertThat(cloudProjects).containsExactly(project1, project2)
  }

  @Test
  fun removeProjectFromCloudDeletesFromCloudRepo() = runTest {
    cloudRepo.addProject(project1)
    totalRepository.removeProjectFromCloud("1")
    Truth.assertThat(cloudRepo.getAllProjects()).isEmpty()
  }

  @Test
  fun addProjectToCloudCopiesFromLocalToCloudAndRemovesFromLocal() = runTest {
    localRepo.addProject(project1)
    cloudRepo = mockk()
    totalRepository = TotalProjectItemsRepositoryCompose(localRepo, cloudRepo)
    coEvery { cloudRepo.getNewId() } returns "new_cloud_id"
    coEvery { cloudRepo.addProject(any()) } returns Unit

    totalRepository.addProjectToCloud("1")

    val projects = localRepo.getAllProjects()
    val project = projects.find { it.uid == "new_cloud_id" }
    Truth.assertThat(project).isNotNull()
    Truth.assertThat(project?.isStoredInCloud).isTrue()
    Truth.assertThat(project?.ownerId).isEqualTo(null)
  }

  @Test
  fun addProjectToLocalStorageAddsToLocalRepo() = runTest {
    totalRepository.addProjectToLocalStorage(project1)
    Truth.assertThat(localRepo.getAllProjects()).contains(project1)
  }

  @Test
  fun removeProjectFromLocalStorageDeletesFromLocalRepo() = runTest {
    localRepo.addProject(project1)
    totalRepository.removeProjectFromLocalStorage("1")
    Truth.assertThat(localRepo.getAllProjects()).isEmpty()
  }
}
