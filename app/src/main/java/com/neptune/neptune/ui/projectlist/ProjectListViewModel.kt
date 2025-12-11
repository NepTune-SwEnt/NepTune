package com.neptune.neptune.ui.projectlist

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.google.firebase.Timestamp
import com.neptune.neptune.data.FileImporterImpl
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import java.io.File
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MEDIA_DB = "media.db"

/**
 * ViewModel for managing the state and operations related to the list of projects. This has been
 * written with the help of LLMs.
 *
 * @property projectRepository Repository for accessing and manipulating project items.
 * @author Uri Jaquet and Angéline Bignens
 */
class ProjectListViewModel(
    private val projectRepository: TotalProjectItemsRepository =
        TotalProjectItemsRepositoryProvider.repository,
    private val importMedia: ImportMediaUseCase,
    getLibrary: GetLibraryUseCase
) : ViewModel() {
  private var _uiState = MutableStateFlow(ProjectListUiState(projects = emptyList()))
  val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

  val library: StateFlow<List<MediaItem>> =
      getLibrary().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  init {
    getAllProjects()
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
   * Deletes a project by its ID and refreshes the project list. This has been written with the help
   * of LLMs.
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

  /**
   * Adds a project to the cloud and refreshes the project list.
   *
   * @param projectId The ID of the project to add to the cloud.
   */
  fun addProjectToCloud(projectId: String) {
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

  /**
   * Imports a media file from a given URI string. Supports both SAF/content URIs and file:// URIs.
   *
   * If the URI uses the "file" scheme, it attempts to convert it to a [File] and calls
   * [importMedia]. If any exception occurs during this conversion, it falls back to importing using
   * the original URI string.
   *
   * @param uriString The URI string pointing to the media file to import. Can be a content URI or
   *   file URI.
   */
  fun importFromSaf(uriString: String) =
      viewModelScope.launch {
        val parsed =
            try {
              URI(uriString)
            } catch (_: Exception) {
              null
            }
        if (parsed != null && parsed.scheme == "file") {
          // use File overload
          try {
            val f = File(parsed)
            importMedia(f)
          } catch (_: Exception) {
            // fallback to string-based import in case of any issue
            importMedia(uriString)
          }
        } else {
          importMedia(uriString)
        }
      }

  /**
   * Imports a [File] produced by the in-app recorder directly and updates the project list
   * immediately.
   *
   * This is a convenience function for handling files already available as [File] objects.
   *
   * @param file The recorded audio file to import as a project.
   */
  fun importRecordedFile(file: File) =
      viewModelScope.launch {
        importMedia(file)
        getAllProjects()
      }
}

/**
 * Factory for creating [ProjectListViewModel] instances with custom use cases. This has been
 * written with the help of LLMs.
 *
 * @property importUC The [ImportMediaUseCase] to be injected into the ViewModel.
 * @property libraryUC The [GetLibraryUseCase] to be injected into the ViewModel.
 */
class ProjectListVMFactory(
    private val importUC: ImportMediaUseCase,
    private val libraryUC: GetLibraryUseCase,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(ProjectListViewModel::class.java)) {
      return ProjectListViewModel(
          projectRepository = TotalProjectItemsRepositoryProvider.repository, importUC, libraryUC)
          as T
    }
    error("Unknown ViewModel class")
  }
}

/**
 * Provides a singleton instance of the [MediaDb] database.This has been written with the help of
 * LLMs.
 *
 * @param context The Android [Context] used to build the database.
 * @return An instance of [MediaDb].
 */
private fun provideDb(context: Context): MediaDb =
    Room.databaseBuilder(context.applicationContext, MediaDb::class.java, MEDIA_DB)
        .fallbackToDestructiveMigration(false)
        .build()

/**
 * Composable function that sets up singletons and provides the [ProjectListVMFactory] for the
 * app.This has been written with the help of LLMs.
 *
 * This function initializes:
 * - The local database ([MediaDb])
 * - Media repository ([MediaRepositoryImpl])
 * - File importer ([FileImporterImpl])
 * - Packager ([NeptunePackager])
 * - Use cases ([ImportMediaUseCase] and [GetLibraryUseCase])
 *
 * @return A [ProjectListVMFactory] ready to create [ProjectListViewModel] instances.
 */
@Composable
fun importAppRoot(): ProjectListVMFactory {
  val context = LocalContext.current
  val db = remember { provideDb(context) }
  val repo = remember { MediaRepositoryImpl(db.mediaDao()) }
  val paths = remember { StoragePaths(context) }
  val importer = remember { FileImporterImpl(context, context.contentResolver, paths) }
  val packager = remember { NeptunePackager(paths) }
  val importUC = remember { ImportMediaUseCase(importer, repo, packager) }
  val libraryUC = remember { GetLibraryUseCase(repo) }

  return ProjectListVMFactory(importUC, libraryUC)
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
