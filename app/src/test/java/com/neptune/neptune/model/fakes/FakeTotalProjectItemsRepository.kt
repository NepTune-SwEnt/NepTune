package com.neptune.neptune.model.fakes

import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A fake repository for testing ProjectListViewModel and other components. Does not touch Firebase;
 * all data is in-memory.
 */
class FakeTotalProjectItemsRepository : TotalProjectItemsRepository {

  private val projects = mutableListOf<ProjectItem>()
  private val idCounter = AtomicInteger(0)
  private val _projectsFlow = MutableStateFlow<List<ProjectItem>>(emptyList())
  val projectsFlow = _projectsFlow.asStateFlow()

  override fun getNewIdLocal(): String = "local_${idCounter.incrementAndGet()}"

  override fun getNewIdCloud(): String = "cloud_${idCounter.incrementAndGet()}"

  override suspend fun getAllProjects(): List<ProjectItem> = projects.toList()

  override suspend fun getProject(projectID: String): ProjectItem =
      projects.find { it.uid == projectID } ?: throw NoSuchElementException()

  override suspend fun addProject(project: ProjectItem) {
    projects += project
    _projectsFlow.value = projects.toList()
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    val idx = projects.indexOfFirst { it.uid == projectID }
    if (idx >= 0) {
      projects[idx] = newValue
      _projectsFlow.value = projects.toList()
    } else throw NoSuchElementException()
  }

  override suspend fun deleteProject(projectID: String) {
    projects.removeAll { it.uid == projectID }
    _projectsFlow.value = projects.toList()
  }

  override suspend fun getAllLocalProjects(): List<ProjectItem> =
      projects.filter { it.uid.startsWith("local_") }

  override suspend fun getAllCloudProjects(): List<ProjectItem> =
      projects.filter { it.uid.startsWith("cloud_") }

  override suspend fun removeProjectFromCloud(projectID: String) {
    val idx = projects.indexOfFirst { it.uid == projectID }
    if (idx >= 0) projects[idx] = projects[idx].copy(isStoredInCloud = false)
  }

  override suspend fun addProjectToCloud(projectID: String) {
    val idx = projects.indexOfFirst { it.uid == projectID }
    if (idx >= 0) projects[idx] = projects[idx].copy(isStoredInCloud = true)
  }

  override suspend fun addProjectToLocalStorage(project: ProjectItem) = addProject(project)

  override suspend fun removeProjectFromLocalStorage(projectID: String) = deleteProject(projectID)
}
