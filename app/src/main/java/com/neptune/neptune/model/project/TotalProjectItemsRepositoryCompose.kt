package com.neptune.neptune.model.project

import android.util.Log

open class TotalProjectItemsRepositoryCompose(
  val localRepo: ProjectItemsRepository,
  val cloudRepo: ProjectItemsRepository) : TotalProjectItemsRepository {

  override fun getNewIdLocal(): String {
    return localRepo.getNewId()
  }

  override fun getNewIdCloud(): String {
    return cloudRepo.getNewId()
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    val localProjects = localRepo.getAllProjects().associateBy { it.uid }
    val cloudProjects = cloudRepo.getAllProjects().associateBy { it.uid }

    val allProjectIDs = localProjects.keys + cloudProjects.keys

    val mergedProjects = allProjectIDs.mapNotNull { projectID ->
      val localProject = localProjects[projectID]
      val cloudProject = cloudProjects[projectID]

      when {
        localProject != null && cloudProject != null -> {
          // Both local and cloud versions exist, choose the one with the latest lastEdited
          if (localProject.lastUpdated >= cloudProject.lastUpdated) {
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
    return getAllProjects().find { it.uid == projectID } ?: throw Exception("Project not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    localRepo.addProject(project)
    cloudRepo.addProject(project)
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    localRepo.editProject(projectID, newValue)
    cloudRepo.editProject(projectID, newValue)
  }

  override suspend fun deleteProject(projectID: String) {
    localRepo.deleteProject(projectID)
    cloudRepo.deleteProject(projectID)
  }

  override suspend fun getProjectDuration(projectID: String): Int {
    TODO("Not yet implemented")
  }

  override suspend fun getAllLocalProjects(): List<ProjectItem> {
    return localRepo.getAllProjects()
  }

  override suspend fun getAllCloudProjects(): List<ProjectItem> {
    return cloudRepo.getAllProjects()
  }

  override suspend fun removeProjectFromCloud(projectID: String) {
    cloudRepo.deleteProject(projectID)
  }

  override suspend fun addProjectToCloud(projectID: String) {
    val project = localRepo.getProject(projectID)
    cloudRepo.addProject(project)
  }

  override suspend fun removeProjectFromLocalStorage(projectID: String) {
    localRepo.deleteProject(projectID)
  }
}
