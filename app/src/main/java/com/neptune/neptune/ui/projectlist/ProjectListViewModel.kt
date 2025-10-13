package com.neptune.neptune.ui.projectlist

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepository
import com.neptune.neptune.model.project.ProjectItemsRepositoryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectListViewModel(
    private val projectRepository: ProjectItemsRepository = ProjectItemsRepositoryProvider.repository,
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
                // Handle the error appropriately in a real app
                _uiState.value = _uiState.value.copy(isLoading = false) // Also set loading to false on error
                Log.e("ProjectListViewModel", "Error loading projects", e)
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(projectId)
                getAllTodos() // Reload projects after deletion
            } catch (e: Exception) {
                // Handle error
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
                getAllTodos() // Reload projects after renaming
            } catch (e: Exception) {
                // Handle error
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
                getAllTodos() // Reload projects after changing description
            } catch (e: Exception) {
                // Handle error
                Log.e("ProjectListViewModel", "Error changing project description", e)
            }
        }
    }
}

data class ProjectListUiState(
    val projects: List<ProjectItem>,
    val isLoading: Boolean = false,
)
