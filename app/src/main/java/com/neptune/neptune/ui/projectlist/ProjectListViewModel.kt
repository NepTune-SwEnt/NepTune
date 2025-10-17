package com.neptune.neptune.ui.projectlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and operations related to the list of projects.
 * This has been written with the help of LLMs.
 *
 * @property projectRepository Repository for accessing and manipulating project items.
 * @author Uri Jaquet
 */
class ProjectListViewModel(
    private val projectRepository: ProjectItemsRepository =
        ProjectItemsRepositoryProvider.repository,
) : ViewModel() {
  private var _uiState = MutableStateFlow(ProjectListUiState(projects = emptyList()))
  val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

  init {
    getAllProjects()
  }

  /**
   * Refreshes the list of projects by fetching them from the repository.
   */
  fun refreshProjects() {
    getAllProjects()
  }

  /**
   * Fetches all projects from the repository, sorts them by favorite status and last updated time,
   * and updates the UI state accordingly.
   * This has been written with the help of LLMs.
   */
  private fun getAllProjects() {
    _uiState.value = _uiState.value.copy(isLoading = true)
    Log.i("ProjectListViewModel", "Loading projects")
    viewModelScope.launch {
      try {
        val projects = projectRepository.getAllProjects()
        val sortedProjects =
            projects.sortedWith(
                compareByDescending<ProjectItem> { it.isFavorite }
                    .thenByDescending { it.lastUpdated })
        _uiState.value = ProjectListUiState(projects = sortedProjects, isLoading = false)
        Log.i("ProjectListViewModel", "Loaded $sortedProjects")
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        Log.e("ProjectListViewModel", "Error loading projects", e)
      }
    }
  }

  /**
   * Deletes a project by its ID and refreshes the project list.
   * This has been written with the help of LLMs.
   *
   * @param projectId The ID of the project to delete.
   */
  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      try {
        projectRepository.deleteProject(projectId)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error deleting project", e)
      }
    }
  }

  /**
   * Renames a project by its ID and refreshes the project list.
   * This has been written with the help of LLMs.
   *
   * @param projectId The ID of the project to rename.
   * @param newName The new name for the project.
   */
  fun renameProject(projectId: String, newName: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject = project.copy(name = newName, lastUpdated = Timestamp.now())
        projectRepository.editProject(projectId, updatedProject)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error renaming project", e)
      }
    }
  }

  /**
   * Changes the description of a project by its ID and refreshes the project list.
   * This has been written with the help of LLMs.
   *
   * @param projectId The ID of the project to update.
   * @param newDescription The new description for the project.
   */
  fun changeProjectDescription(projectId: String, newDescription: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject =
            project.copy(description = newDescription, lastUpdated = Timestamp.now())
        projectRepository.editProject(projectId, updatedProject)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error changing project description", e)
      }
    }
  }

  /**
   * Toggles the favorite status of a project by its ID and refreshes the project list.
   * This has been written with the help of LLMs.
   *
   * @param projectId The ID of the project to toggle favorite status.
   */
  fun toggleFavorite(projectId: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject = project.copy(isFavorite = !project.isFavorite)
        projectRepository.editProject(projectId, updatedProject)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error toggling favorite", e)
      }
    }
  }

  /**
   * Selects a project and updates the UI state with the selected project's ID.
   *
   * @param project The project to select.
   */
  fun selectProject(project: ProjectItem) {
    _uiState.value = _uiState.value.copy(selectedProject = project.id)
  }

  /**
   * Gets the duration of a project in "MM:SS" format.
   */
  fun getProjectDuration(project: ProjectItem): String {
    TODO("Not yet implemented")
  }
}

/**
 * Data class representing the UI state for the project list.
 *
 * @property projects List of project items to display.
 * @property isLoading Indicates if the project list is currently being loaded.
 * @property selectedProject The ID of the currently selected project, if any.
 * @author Uri Jaquet
 */
data class ProjectListUiState(
    val projects: List<ProjectItem>,
    val isLoading: Boolean = false,
    val selectedProject: String? = null
)
