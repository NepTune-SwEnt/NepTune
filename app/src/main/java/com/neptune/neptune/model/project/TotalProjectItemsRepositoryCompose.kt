package com.neptune.neptune.model.project

import android.util.Log

class TotalProjectItemsRepositoryCompose(
  val localRepo: ProjectItemsRepository,
  val cloudRepo: ProjectItemsRepository) : TotalProjectItemsRepository {

  override fun getNewIdLocal(): String {
    return localRepo.getNewId()
  }

  override fun getNewIdCloud(): String {
    return cloudRepo.getNewId()
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    val localProjects = localRepo.getAllProjects().map { it.id to it }.toMap()
    val cloudProjects = cloudRepo.getAllProjects().map { it.id to it }.toMap()

    val allProjectIDs = localProjects.keys + cloudProjects.keys

    val mergedProjects = allProjectIDs.mapNotNull { projectID ->
      val localProject = localProjects[projectID]
      val cloudProject = cloudProjects[projectID]

      when {
        localProject != null && cloudProject != null -> {
          // Both local and cloud versions exist, choose the one with the latest lastEdited
          if (localProject.lastEdited >= cloudProject.lastEdited) {
            localProject
          } else {
            cloudProject
          }
        }

        localProject != null -> localProject // Only local version exists
        cloudProject != null -> cloudProject // Only cloud version exists
        else -> null // This case should not happen
      }
    }

    Log.d("TotalRepo", "Merged Projects: $mergedProjects")

    return mergedProjects
  }

  override suspend fun getProject(projectID: String): ProjectItem {
  }

  override suspend fun addProject(project: ProjectItem) {
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
  }

  override suspend fun deleteProject(projectID: String) {
  }

  override suspend fun getProjectDuration(projectID: String): Int {
    TODO("Not yet implemented")
  }

  override suspend fun getAllLocalProjects(): List<ProjectItem> {
    TODO("Not yet implemented")
  }

  override suspend fun getAllCloudProjects(): List<ProjectItem> {
    TODO("Not yet implemented")
  }

  override suspend fun removeProjectFromCloud(projectID: String) {
    TODO("Not yet implemented")
  }

  override suspend fun addProjectToCloud(projectID: String) {
    TODO("Not yet implemented")
  }

  override suspend fun removeProjectFromLocalStorage(projectID: String) {
    TODO("Not yet implemented")
  }
}
