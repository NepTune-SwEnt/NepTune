package com.neptune.neptune.ui.post

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.util.NetworkConnectivityObserver
import com.neptune.neptune.util.WaveformExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val storageService: StorageService? =
        StorageService(
            FirebaseStorage.getInstance(
                NepTuneApplication.appContext.getString(
                    R.string.storage_path))), // Make nullable or provide a default if needed, but
    // injection is key
    private val imageRepo: ImageStorageRepository = ImageStorageRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow(PostUiState())
  val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

  private val _localImageUri = MutableStateFlow<Uri?>(null)
  val localImageUri: StateFlow<Uri?> = _localImageUri.asStateFlow()
  private val waveformExtractor = WaveformExtractor()

  private val _localZipUri = MutableStateFlow<Uri?>(null)
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  val isAnonymous: Boolean
    get() = auth.currentUser?.isAnonymous ?: true

  private val _processedAudioUri = MutableStateFlow<Uri?>(null)
  private val timeout = 5000L
  val isOnline: StateFlow<Boolean> =
      NetworkConnectivityObserver()
          .isOnline
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(timeout),
              initialValue = true)

  /** Loads a project by its ID and converts it into a Sample. */
  fun loadProject(projectId: String) {
    viewModelScope.launch {
      try {
        val project = projectRepository.getProject(projectId)
        val rawPath = project.projectFileLocalPath
        if (rawPath.isNullOrEmpty()) {
          Log.e("PostViewModel", "The project don't have a file")
          return@launch
        }
        val previewPath = project.audioPreviewLocalPath
        val playbackUri =
            if (!previewPath.isNullOrBlank()) {
              previewPath.toUri()
            } else {
              null
            }
        _localZipUri.value = rawPath.toUri()
        val durationSeconds = storageService?.getProjectDuration(_localZipUri.value) ?: 0
        // processed audio
        project.audioPreviewLocalPath
            ?.takeIf { it.isNotBlank() }
            ?.let { localPathString -> _processedAudioUri.value = localPathString.toUri() }

        val wf =
            if (playbackUri != null) {
              waveformExtractor.extractWaveform(
                  NepTuneApplication.appContext, playbackUri, samplesCount = 100)
            } else emptyList()
        val sample =
            Sample(
                id = project.uid,
                name = project.name,
                description = project.description,
                tags = project.tags,
                durationSeconds = durationSeconds,
                likes = 0,
                usersLike = emptyList(),
                comments = 0,
                downloads = 0,
                ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "")

        _uiState.update { it.copy(sample = sample, playbackUri = playbackUri, waveform = wf) }
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
      try {
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
      } catch (e: Exception) {
        Log.e("PostViewModel", "Failed to change image: ${e.message}")
      }
    }
  }

  fun audioExist(): Boolean {
    val currentZipUri = _localZipUri.value
    return currentZipUri != null
  }

  /** Submits the post */
  fun submitPost() {
    if (isAnonymous || _uiState.value.isUploading) {
      return
    }
    val currentZipUri = _localZipUri.value
    if (currentZipUri == null) {
      Log.e("PostViewModel", "Cannot submit: No zip file loaded")
      return
    }
    _uiState.update { it.copy(isUploading = true) }
    viewModelScope.launch {
      try {
        storageService?.uploadSampleFiles(
            _uiState.value.sample, currentZipUri, localImageUri.value, _processedAudioUri.value)
        profileRepo.updatePostCount(1)
        _uiState.update { it.copy(isUploading = false, postComplete = true) }
      } catch (e: Exception) {
        Log.e("PostViewModel", "error on upload", e)
        _uiState.update { it.copy(isUploading = false) }
      }
    }
  }

  /** Switches the visibility of the post between Public (true) and My followers (false). */
  fun toggleAudience() {
    _uiState.update { currentState ->
      val currentSample = currentState.sample
      currentState.copy(sample = currentSample.copy(isPublic = !currentSample.isPublic))
    }
  }
}

/**
 * Represents the UI state of the Post screen
 *
 * @property sample The sample being posted
 * @property isUploading Indicates if the post is currently being uploaded
 * @property postComplete Indicates if the post has been successfully completed
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
            downloads = 0),
    val isUploading: Boolean = false,
    val postComplete: Boolean = false,
    val playbackUri: Uri? = null,
    val waveform: List<Float> = emptyList()
)
