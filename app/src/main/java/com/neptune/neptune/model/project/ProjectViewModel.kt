package com.neptune.neptune.model.project

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.storage.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Represents the state of the project screen */
data class ProjectUiState(
    val project: ProjectItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel to manage the logic of a project screen, including project loading and image uploading.
 *
 * This class was created by using AI assistance.
 */
class ProjectViewModel(
    private val projectRepo: ProjectItemsRepository = ProjectItemsRepositoryProvider.repository,
    private val storageService: StorageService = StorageService(FirebaseStorage.getInstance())
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProjectUiState())
  val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

  // State for a the image that has just been selected by the user
  private val _localImageUri = MutableStateFlow<Uri?>(null)
  val localImageUri: StateFlow<Uri?> = _localImageUri.asStateFlow()

  // Contains the local URI of the project's .zip file
  private val _localProjectFileUri = MutableStateFlow<Uri?>(null)
  val localProjectFileUri: StateFlow<Uri?> = _localProjectFileUri.asStateFlow()

  // Contains the local URI of the preview .mp3 file
  private val _localPreviewFileUri = MutableStateFlow<Uri?>(null)
  val localPreviewFileUri: StateFlow<Uri?> = _localPreviewFileUri.asStateFlow()

  /**
   * Function to retrieve a project from Firestore. This loads the project, including the existing
   * 'imageUrl' field.
   */
  fun loadProject(projectId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, error = null) }
      try {
        val project = projectRepo.getProject(projectId)
        _uiState.update { it.copy(isLoading = false, project = project) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = e.message) }
      }
    }
  }

  /**
   * Called by the UI (image picker) when a new image is selected/cropped. Updates the local URI so
   * the UI can display a preview.
   */
  fun onImageSelected(uri: Uri?) {
    _localImageUri.value = uri
  }

  /**
   * Function to save the image to 'sample_image/' and update the project in Firestore with the new
   * URL.
   */
  fun saveImageToStorage() {
    val project = _uiState.value.project
    val localUri = _localImageUri.value

    if (project == null || localUri == null) {
      _uiState.update { it.copy(error = "No projects loaded or images selected") }
      return
    }

    // Define the path in Firebase Storage
    val storagePath = "sample_image/${project.id}.jpg"

    _uiState.update { it.copy(isLoading = true) }

    viewModelScope.launch {
      try {
        // Save the picture in storage
        val downloadUrl = storageService.uploadFileAndGetUrl(localUri, storagePath)

        val updatedProject = project.copy(imageUrl = downloadUrl)

        projectRepo.editProject(project.id, updatedProject)

        _uiState.update { it.copy(isLoading = false, project = updatedProject, error = null) }
        _localImageUri.value = null
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
      }
    }
  }

  /** Called by the UI when a .zip file is chosen. */
  fun onProjectFileSelected(uri: Uri?) {
    _localProjectFileUri.value = uri
  }

  /**
   * Function to save the project's .zip file to 'samples/' and update the project in Firestore with
   * the new 'filePath' URL.
   */
  fun saveProjectFileToStorage() {
    val project = _uiState.value.project
    val localUri = _localProjectFileUri.value

    if (project == null || localUri == null) {
      _uiState.update { it.copy(error = "No projects loaded or no project files selected") }
      return
    }

    val storagePath = "samples/${project.id}.zip"

    _uiState.update { it.copy(isLoading = true) }

    viewModelScope.launch {
      try {

        val downloadUrl = storageService.uploadFileAndGetUrl(localUri, storagePath)

        val updatedProject = project.copy(filePath = downloadUrl)

        projectRepo.editProject(project.id, updatedProject)

        _uiState.update { it.copy(isLoading = false, project = updatedProject, error = null) }
        _localProjectFileUri.value = null
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
      }
    }
  }

  // Called by the UI when an .mp3 file is chosen.
  fun onPreviewFileSelected(uri: Uri?) {
    _localPreviewFileUri.value = uri
  }

  /**
   * Function to save the preview .mp3 file to 'sample_previews/' and update the project in
   * Firestore with the new URL 'previewUrl'.
   */
  fun savePreviewFileToStorage() {
    val project = _uiState.value.project
    val localUri = _localPreviewFileUri.value

    if (project == null || localUri == null) {
      _uiState.update { it.copy(error = "No project loaded or preview file selected") }
      return
    }

    val storagePath = "sample_previews/${project.id}.mp3"

    _uiState.update { it.copy(isLoading = true) }

    viewModelScope.launch {
      try {
        val downloadUrl = storageService.uploadFileAndGetUrl(localUri, storagePath)

        val updatedProject = project.copy(previewUrl = downloadUrl)

        projectRepo.editProject(project.id, updatedProject)

        _uiState.update { it.copy(isLoading = false, project = updatedProject, error = null) }
        _localPreviewFileUri.value = null
      } catch (e: Exception) {
        _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${e.message}") }
      }
    }
  }
}
