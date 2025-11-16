package com.neptune.neptune.ui.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and operations related to the post screen. This has been written
 * with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class PostViewModel(
    private val projectRepository: TotalProjectItemsRepository =
        TotalProjectItemsRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(PostUiState())
  val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

  private val _localImageUri = MutableStateFlow<Uri?>(null)
  val localImageUri: StateFlow<Uri?> = _localImageUri.asStateFlow()

  private val imageRepo = ImageStorageRepository()

  /** Loads a project by its ID and converts it into a Sample. */
  fun loadProject(projectId: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)

        val sample =
            Sample(
                id = project.uid,
                name = project.name,
                description = project.description,
                tags = project.tags,
                durationSeconds = 0,
                uriString = project.projectFileUri ?: "",
                likes = 0,
                usersLike = emptyList(),
                comments = 0,
                downloads = 0) // isPublic button

        _uiState.update { it.copy(sample = sample) }
      } catch (e: Exception) {
        Log.e("PostViewModel", "Error when downloading the project", e)
      }
    }
  }
  /**
   * Loads a sample
   *
   * @param sample The sample to load in the post screen
   */
  fun loadSample(sample: Sample) {
    _uiState.update { it.copy(sample = sample) }
  }

  /**
   * Updates the title of the currently loaded sample
   *
   * @param title The new title to set
   */
  fun updateTitle(title: String) {
    _uiState.update { it.copy(sample = it.sample.copy(name = title)) }
  }

  /**
   * Updates the description of the currently loaded sample
   *
   * @param desc The new description to set
   */
  fun updateDescription(desc: String) {
    _uiState.update { it.copy(sample = it.sample.copy(description = desc)) }
  }

  /**
   * Updates the tags of the currently loaded sample
   *
   * @param tags The new tags to set
   */
  fun updateTags(tags: String) {
    val tagList = tags.split(" ").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
    _uiState.update { it.copy(sample = it.sample.copy(tags = tagList)) }
  }

  /**
   * Callback for when a new image is selected and cropped.
   *
   * @param uri The URI of the new image, or null if the process was canceled.
   */
  fun onImageChanged(uri: Uri?) {
    if (uri == null) {
      // The user cancels
      return
    }
    viewModelScope.launch {
      val sampleId = uiState.value.sample.id
      val fileName = "post_image_for_sample_$sampleId.jpg"

      imageRepo.saveImageFromUri(uri, fileName)

      // Get the new URI from our internal storage and update the UI
      _localImageUri.value =
          imageRepo
              .getImageUri(fileName)
              ?.buildUpon()
              ?.appendQueryParameter(
                  "t", System.currentTimeMillis().toString() // Cache buster
                  )
              ?.build()
    }
  }

  /** Submits the post */
  fun submitPost() {
    // TODO: Post logic
  }
}

/**
 * Represents the UI state of the Post screen
 *
 * @property sample The sample being posted
 * @property audience The selected audience for the post
 */
data class PostUiState(
    val sample: Sample =
        Sample(
            id = "0",
            name = "",
            description = "",
            durationSeconds = 0,
            tags = emptyList(),
            likes = 0,
            usersLike = emptyList(),
            comments = 0,
            downloads = 0,
            uriString = ""),
    val audience: String = "Followers"
)
