package com.neptune.neptune.model.project

interface ProjectItemsRepository {
  fun getNewId(): String

  suspend fun getAllProjects(): List<ProjectItem>

  suspend fun getProject(projectID: String): ProjectItem

  suspend fun addProject(project: ProjectItem)

  suspend fun editProject(projectID: String, newValue: ProjectItem)

  suspend fun deleteProject(projectID: String)

  suspend fun getProjectDuration(projectID: String): Int
}
