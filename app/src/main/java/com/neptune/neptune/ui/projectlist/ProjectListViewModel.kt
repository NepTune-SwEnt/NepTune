package com.neptune.neptune.ui.projectlist

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.util.NetworkConnectivityObserver
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProjectListViewModelFactory(
    private val getLibraryUseCase: GetLibraryUseCase,
    private val mediaRepository: MediaRepository,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return ProjectListViewModel(
        projectRepository = TotalProjectItemsRepositoryProvider.repository,
        getLibraryUseCase = getLibraryUseCase,
        mediaRepository = mediaRepository,
        storageService = StorageService(FirebaseStorage.getInstance()),
        auth = FirebaseAuth.getInstance())
        as T
  }
}

/**
 * ViewModel for managing the state and operations related to the list of projects. This has been
 * written with the help of LLMs.
 *
 * @property projectRepository Repository for accessing and manipulating project items.
 * @author Uri Jaquet
 */
class ProjectListViewModel(
    private val projectRepository: TotalProjectItemsRepository =
        TotalProjectItemsRepositoryProvider.repository,
    private val getLibraryUseCase: GetLibraryUseCase? = null,
    private val mediaRepository: MediaRepository? = null,
    private val storageService: StorageService? = null,
    private val auth: FirebaseAuth? = null,
    private val connectivityObserver: NetworkConnectivityObserver = NetworkConnectivityObserver()
) : ViewModel() {
  private var _uiState = MutableStateFlow(ProjectListUiState(projects = emptyList()))
  val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()
  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  val isUserLoggedIn: Boolean
    get() = auth?.currentUser != null

  init {
    viewModelScope.launch {
      try {

        connectivityObserver.isOnline.collectLatest { connected ->
          _isOnline.value = connected
          refreshProjects()
        }
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Network observer error", e)
      }
    }
    viewModelScope.launch {
      try {
        getLibraryUseCase?.invoke()?.collectLatest { refreshProjects() }
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Library observer error", e)
      }
    }
  }

  /** Refreshes the list of projects by fetching them from the repository. */
  fun refreshProjects() {
    getAllProjects()
  }

  /**
   * Fetches all projects from the repository, sorts them by favorite status and last updated time,
   * and updates the UI state accordingly. This has been written with the help of LLMs.
   */
  private fun getAllProjects() {
    _uiState.value = _uiState.value.copy(isLoading = true)

    viewModelScope.launch {
      try {
        val localItems = getLibraryUseCase?.invoke()?.first() ?: emptyList()
        val online = _isOnline.value

        if (!isUserLoggedIn) {
          val localProjects = localItems.map { toLocalProjectItem(it) }
          val sortedProjects = localProjects.sortedByDescending { it.lastUpdated }
          _uiState.value = ProjectListUiState(projects = sortedProjects, isLoading = false)
          return@launch
        }

        // auto sync
        if (online && localItems.isNotEmpty()) {
          localItems.forEach { item -> importProjectInFirebaseSuspend(item.id) }
        }

        val projects =
            if (online) {
              try {
                projectRepository.getAllProjects()
              } catch (_: Exception) {
                Log.w("ProjectListViewModel", "Error on cloud pass in offline mode")
                val remainingLocal = getLibraryUseCase?.invoke()?.first() ?: emptyList()
                remainingLocal.map { toLocalProjectItem(it) }
              }
            } else {
              localItems.map { toLocalProjectItem(it) }
            }

        val sortedProjects =
            projects.sortedWith(
                compareByDescending<ProjectItem> { it.isFavorite }
                    .thenByDescending { it.lastUpdated })

        _uiState.value = ProjectListUiState(projects = sortedProjects, isLoading = false)
      } catch (e: Exception) {
        Log.e("ProjectListVM", "Error downloading", e)
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }

  private fun toLocalProjectItem(mediaItem: MediaItem): ProjectItem {
    val file = File(mediaItem.projectUri)
    val name = file.nameWithoutExtension.ifBlank { "Imported Project" }

    return ProjectItem(
        uid = "local_${mediaItem.id}",
        name = name,
        projectFileLocalPath = mediaItem.projectUri,
    )
  }

  /**
   * Deletes a project by its ID and refreshes the project list. This has been written with the help
   * of LLMs.
   *
   * @param projectId The ID of the project to delete.
   */
  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      try {
        val projectToDelete = uiState.value.projects.find { it.uid == projectId }
        projectRepository.deleteProject(projectId)

        if (projectToDelete?.projectFileLocalPath != null && mediaRepository != null) {
          val path = projectToDelete.projectFileLocalPath
          val localItems = getLibraryUseCase?.invoke()?.first() ?: emptyList()
          val ghostItem = localItems.find { it.projectUri == path }
          if (ghostItem != null) {
            mediaRepository.delete(ghostItem)
          }
          try {
            val file = File(path)
            if (file.exists()) file.delete()
          } catch (_: Exception) {
            // Ignore
          }
        }
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error deleting project", e)
      }
    }
  }

  /**
   * Renames a project by its ID and refreshes the project list. This has been written with the help
   * of LLMs.
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
   * Changes the description of a project by its ID and refreshes the project list. This has been
   * written with the help of LLMs.
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

  private suspend fun importProjectInFirebaseSuspend(rawId: String) {
    try {
      val localItems = getLibraryUseCase?.invoke()?.first() ?: emptyList()
      val mediaItem = localItems.find { it.id == rawId } ?: return
      val file = File(mediaItem.projectUri)

      if (file.exists() && storageService != null && mediaRepository != null) {
        val newCloudId = projectRepository.getNewIdCloud()
        val storagePath = "projects/$newCloudId.zip"

        storageService.uploadFile(Uri.fromFile(file), storagePath)
        val downloadUrl = storageService.getDownloadUrl(storagePath)

        val newProject =
            ProjectItem(
                uid = newCloudId,
                name = file.nameWithoutExtension,
                isStoredInCloud = false,
                projectFileCloudUri = downloadUrl,
                projectFileLocalPath = mediaItem.projectUri,
                lastUpdated = Timestamp.now())
        projectRepository.addProject(newProject)

        mediaRepository.delete(mediaItem)
      }
    } catch (e: Exception) {
      Log.e("ProjectListViewModel", "Error sync item $rawId", e)
    }
  }

  /**
   * Adds a project to the cloud and refreshes the project list.
   *
   * @param projectId The ID of the project to add to the cloud.
   */
  fun addProjectToCloud(projectId: String) {
    if (projectId.startsWith("local_")) return
    viewModelScope.launch {
      try {
        projectRepository.addProjectToCloud(projectId)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error adding project to cloud", e)
      }
    }
  }

  /**
   * Removes a project from the cloud and refreshes the project list.
   *
   * @param projectId The ID of the project to remove from the cloud.
   */
  fun removeProjectFromCloud(projectId: String) {
    viewModelScope.launch {
      try {
        projectRepository.removeProjectFromCloud(projectId)
        refreshProjects()
      } catch (e: Exception) {
        Log.e("ProjectListViewModel", "Error removing project from cloud", e)
      }
    }
  }

  /**
   * Toggles the favorite status of a project by its ID and refreshes the project list. This has
   * been written with the help of LLMs.
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
    _uiState.value = _uiState.value.copy(selectedProject = project.uid)
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
    val selectedProject: String? = null,
)
