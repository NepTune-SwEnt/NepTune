// Kotlin
package com.neptune.neptune.ui.projectlist

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectRepository: ProjectItemsRepository =
        ProjectItemsRepositoryProvider.repository,
) : ViewModel() {
  private var _uiState = MutableStateFlow(ProjectListUiState(projects = emptyList()))
  val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

  init {
    getAllTodos()
  }

  fun refreshProjects() {
    getAllTodos()
  }

  private fun getAllTodos() {
    _uiState.value = _uiState.value.copy(isLoading = true)
    Log.i("ProjectListViewModel", "Loading projects")
    viewModelScope.launch {
      try {
        val projects = projectRepository.getAllProjects()
        _uiState.value = ProjectListUiState(projects = projects.toList(), isLoading = false)
        Log.i("ProjectListViewModel", "Loaded ${projects.toString()}")
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false)
        Log.e("ProjectListViewModel", "Error loading projects", e)
      }
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      try {
        projectRepository.deleteProject(projectId)
        getAllTodos()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error deleting project", e)
      }
    }
  }

  fun renameProject(projectId: String, newName: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject = project.copy(name = newName)
        projectRepository.editProject(projectId, updatedProject)
        getAllTodos()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error renaming project", e)
      }
    }
  }

  fun changeProjectDescription(projectId: String, newDescription: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject = project.copy(description = newDescription)
        projectRepository.editProject(projectId, updatedProject)
        getAllTodos()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error changing project description", e)
      }
    }
  }

  fun toggleFavorite(projectId: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val updatedProject = project.copy(isFavorite = !project.isFavorite)
        projectRepository.editProject(projectId, updatedProject)
        getAllTodos()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error toggling favorite", e)
      }
    }
  }

  fun selectProject(project: ProjectItem) {
    _uiState.value = _uiState.value.copy(selectedProject = project.id)
  }

  fun getProjectDuration(project: ProjectItem): String {
    if (project.previewUrl.isNullOrEmpty()) return "00:00"
    return "00:59"
  }

  fun storeInCloud(projectId: String) {}
}

data class ProjectListUiState(
    val projects: List<ProjectItem>,
    val isLoading: Boolean = false,
    val selectedProject: String? = null
)
