package com.neptune.neptune.model.project

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

open class TotalProjectItemsRepositoryCompose(
    val localRepo: ProjectItemsRepository,
    val cloudRepo: ProjectItemsRepository
) : TotalProjectItemsRepository {

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

    val mergedProjects =
        allProjectIDs.mapNotNull { projectID ->
          val localProject = localProjects[projectID]
          val cloudProject = cloudProjects[projectID]

          when {
            localProject != null && cloudProject != null -> {
              // Both local and cloud versions exist, choose the one with the latest lastEdited
              if (localProject.lastUpdated >= cloudProject.lastUpdated) {
                localProject.copy(isStoredInCloud = true)
              } else {
                cloudProject
              }
            }
            localProject != null -> localProject // Only local version exists
            cloudProject != null -> cloudProject // Only cloud version exists
            else -> null // This case should not happen
          }
        }

    // Return defensive copies of the items and the list itself so callers get new instances
    return mergedProjects.map { it.copy() }.toList()
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    return getAllProjects().find { it.uid == projectID } ?: throw Exception("Project not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    localRepo.addProject(project)
    cloudRepo.addProject(project)
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    try {
      localRepo.editProject(projectID, newValue)
    } catch (e: Exception) {
      Log.e("TotalRepo", "Local edit failed for projectID $projectID: ${e.message}")
    }
    try {
      cloudRepo.editProject(projectID, newValue)
    } catch (e: Exception) {
      Log.e("TotalRepo", "Cloud edit failed for projectID $projectID: ${e.message}")
    }
  }

  override suspend fun deleteProject(projectID: String) {
    var inLocalRepo = false
    var inCloudRepo = false
    try {
      localRepo.getProject(projectID)
      inLocalRepo = true
    } catch (_: Exception) {
      // Nothing
    }
    try {
      cloudRepo.getProject(projectID)
      inCloudRepo = true
    } catch (_: Exception) {
      // Nothing
    }
    if (!inLocalRepo && !inCloudRepo) {
      throw Exception("Project not found")
    }
    if (inLocalRepo) {
      localRepo.deleteProject(projectID)
    }
    if (inCloudRepo) {
      cloudRepo.deleteProject(projectID)
    }
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
    val newUid = cloudRepo.getNewId()
    val newProject =
        project.copy(uid = newUid, isStoredInCloud = true, ownerId = Firebase.auth.currentUser?.uid)
    removeProjectFromLocalStorage(projectID)
    localRepo.addProject(newProject)
    cloudRepo.addProject(newProject)
  }

  override suspend fun addProjectToLocalStorage(project: ProjectItem) {
    localRepo.addProject(project)
  }

  override suspend fun removeProjectFromLocalStorage(projectID: String) {
    localRepo.deleteProject(projectID)
  }
}
