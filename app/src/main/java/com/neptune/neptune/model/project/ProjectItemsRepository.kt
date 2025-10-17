package com.neptune.neptune.model.project

/**
 * Interface defining the contract for a repository that manages project items.
 *
 * @author Uri Jaquet
 */
interface ProjectItemsRepository {
  /**
   * Generates and returns a new unique identifier for a project.
   */
  fun getNewId(): String

  /**
   * Retrieves all project items.
   *
   * @return A list of all project items.
   */
  suspend fun getAllProjects(): List<ProjectItem>

  /**
   * Retrieves a specific project item by its unique identifier.
   *
   * @param projectID The unique identifier of the project to retrieve.
   * @return The project item corresponding to the provided identifier.
   */
  suspend fun getProject(projectID: String): ProjectItem

  /**
   * Adds a new project item to the repository.
   *
   * @param project The project item to add.
   */
  suspend fun addProject(project: ProjectItem)

  /**
   * Edits an existing project item in the repository.
   *
   * @param projectID The unique identifier of the project to edit.
   * @param newValue The new value for the project item.
   */
  suspend fun editProject(projectID: String, newValue: ProjectItem)

  /**
   * Deletes a project item from the repository.
   *
   * @param projectID The unique identifier of the project to delete.
   */
  suspend fun deleteProject(projectID: String)

  /**
   * Retrieves the duration of a specific project by its unique identifier.
   *
   * @param projectID The unique identifier of the project.
   * @return The duration of the project in minutes.
   */
  suspend fun getProjectDuration(projectID: String): Int
}
