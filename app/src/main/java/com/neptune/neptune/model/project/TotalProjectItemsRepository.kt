package com.neptune.neptune.model.project

interface TotalProjectItemsRepository {
  fun getNewIdLocal(): String

  fun getNewIdCloud(): String

  suspend fun getAllProjects(): List<ProjectItem>

  suspend fun getProject(projectID: String): ProjectItem

  suspend fun addProject(project: ProjectItem)

  suspend fun editProject(projectID: String, newValue: ProjectItem)

  suspend fun deleteProject(projectID: String)

  suspend fun getProjectDuration(projectID: String): Int

  suspend fun getAllLocalProjects(): List<ProjectItem>

  suspend fun getAllCloudProjects(): List<ProjectItem>

  suspend fun removeProjectFromCloud(projectID: String)

  suspend fun addProjectToCloud(projectID: String)

  suspend fun addProjectToLocalStorage(project: ProjectItem)

  suspend fun removeProjectFromLocalStorage(projectID: String)
}
