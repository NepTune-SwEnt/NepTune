package com.neptune.neptune.model.project

import android.util.Log
import com.google.firebase.Timestamp

class ProjectItemsRepositoryVar : ProjectItemsRepository {

  private val projects = mutableListOf<ProjectItem>(
    ProjectItem(
      id = "1",
      name = "Project 1",
      description = "Description 1",
      isFavorite = false,
      tags = listOf(),
      previewUrl = null,
      fileUrl = null,
      lastUpdated = Timestamp.now(),
      ownerId = null,
      collaborators = listOf(),
    ),
    ProjectItem(
      id = "2",
      name = "Project 2",
      description = "Description 2",
      isFavorite = true,
      tags = listOf(),
      previewUrl = null,
      fileUrl = null,
      lastUpdated = Timestamp.now(),
      ownerId = null,
      collaborators = listOf(),
    )
  )
  private var idCounter = 0

  override fun getNewId(): String {
    return (idCounter++).toString()
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    return projects
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    return projects.find { it.id == projectID }
        ?: throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    Log.i("ProjectItemsRepositoryVar", "Repo state: $projects")
    Log.i("ProjectItemsRepositoryVar", "Adding project: $project")
    if (projects.any { it.id == project.id }) {
      Log.e(
          "ProjectItemsRepositoryVar",
          "ProjectItem with the same ID already exists, project: ${getProject(project.id)}")
      throw Exception("ProjectItemsRepositoryVar: ProjectItem with the same ID already exists")
    }
    projects.add(project)
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    val index = projects.indexOfFirst { it.id == projectID }
    if (index != -1) {
      projects[index] = newValue.copy(id = projectID)

      return
    }
    throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
  }

  override suspend fun deleteProject(projectID: String) {
    val index = projects.indexOfFirst { it.id == projectID }
    if (index != -1) {
      projects.removeAt(index)
      return
    }
    throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
  }

  override suspend fun getProjectDuration(projectID: String): Int {
    TODO("Not yet implemented")
  }
}
