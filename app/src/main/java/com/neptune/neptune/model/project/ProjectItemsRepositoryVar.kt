package com.neptune.neptune.model.project

import android.util.Log
import java.util.UUID

private const val NOT_FOUND = "ProjectItemsRepositoryVar: ProjectItem not found"

class ProjectItemsRepositoryVar : ProjectItemsRepository {

  private val projects = mutableListOf<ProjectItem>()

  override fun getNewId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    return projects.toList()
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    return projects.find { it.uid == projectID }
        ?: throw Exception(NOT_FOUND)
  }

  override suspend fun addProject(project: ProjectItem) {
    Log.i("ProjectItemsRepositoryVar", "Repo state: $projects")
    Log.i("ProjectItemsRepositoryVar", "Adding project: $project")
    if (projects.any { it.uid == project.uid }) {
      Log.e(
          "ProjectItemsRepositoryVar",
          "ProjectItem with the same ID already exists, project: ${getProject(project.uid)}")
      throw Exception("ProjectItemsRepositoryVar: ProjectItem with the same ID already exists")
    }
    projects.add(project)
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    val index = projects.indexOfFirst { it.uid == projectID }
    if (index != -1) {
      projects[index] = newValue.copy(uid = projectID)

      return
    }
    throw Exception(NOT_FOUND)
  }

  override suspend fun deleteProject(projectID: String) {
    val index = projects.indexOfFirst { it.uid == projectID }
    if (index != -1) {
      projects.removeAt(index)
      return
    }
    throw Exception(NOT_FOUND)
  }
}
