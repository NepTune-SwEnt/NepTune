/**
 * This file was coded with the help of LLMs.
 *
 * @author Uri Jaquet
 */
package com.neptune.neptune.model.project

/** Interface for a repository that manages both local and cloud project items. */
interface TotalProjectItemsRepository {
  /** Generates a new unique ID for a local project. */
  fun getNewIdLocal(): String

  /** Generates a new unique ID for a cloud project. */
  fun getNewIdCloud(): String

  /** Retrieves all projects, from both local and cloud sources. */
  suspend fun getAllProjects(): List<ProjectItem>

  /**
   * Retrieves a specific project by its ID.
   *
   * @param projectID The ID of the project to retrieve.
   */
  suspend fun getProject(projectID: String): ProjectItem

  /**
   * Adds a new project to the repository.
   *
   * @param project The project item to add.
   */
  suspend fun addProject(project: ProjectItem)

  /**
   * Edits an existing project.
   *
   * @param projectID The ID of the project to edit.
   * @param newValue The new project item data.
   */
  suspend fun editProject(projectID: String, newValue: ProjectItem)

  /**
   * Deletes a project from the repository.
   *
   * @param projectID The ID of the project to delete.
   */
  suspend fun deleteProject(projectID: String)

  /** Retrieves all projects stored locally. */
  suspend fun getAllLocalProjects(): List<ProjectItem>

  /** Retrieves all projects stored in the cloud. */
  suspend fun getAllCloudProjects(): List<ProjectItem>

  /**
   * Removes a project from the cloud storage.
   *
   * @param projectID The ID of the project to remove from the cloud.
   */
  suspend fun removeProjectFromCloud(projectID: String)

  /**
   * Adds a project to the cloud storage.
   *
   * @param projectID The ID of the project to add to the cloud.
   */
  suspend fun addProjectToCloud(projectID: String)

  /**
   * Adds a project to the local storage.
   *
   * @param project The project item to add locally.
   */
  suspend fun addProjectToLocalStorage(project: ProjectItem)

  /**
   * Removes a project from the local storage.
   *
   * @param projectID The ID of the project to remove from local storage.
   */
  suspend fun removeProjectFromLocalStorage(projectID: String)
}
