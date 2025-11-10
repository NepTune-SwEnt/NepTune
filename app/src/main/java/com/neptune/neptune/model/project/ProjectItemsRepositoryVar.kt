package com.neptune.neptune.model.project

import android.util.Log

class ProjectItemsRepositoryVar : ProjectItemsRepository {

  private val projects = mutableListOf<ProjectItem>()
  private var idCounter = 0

  override fun getNewId(): String {
    return (idCounter++).toString()
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    return projects
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    return projects.find { it.uid == projectID }
        ?: throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
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
    throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
  }

  override suspend fun deleteProject(projectID: String) {
    val index = projects.indexOfFirst { it.uid == projectID }
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
