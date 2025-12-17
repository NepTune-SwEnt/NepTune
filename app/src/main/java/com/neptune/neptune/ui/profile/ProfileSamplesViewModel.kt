package com.neptune.neptune.ui.profile

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.feed.BaseSampleFeedViewModel
import com.neptune.neptune.ui.feed.SampleFeedController
import com.neptune.neptune.ui.main.SampleUiActions
import com.neptune.neptune.util.DownloadDirectoryProvider
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the list of samples displayed on a profile.
 *
 * Observes items owned by [ownerId], exposes like state, and brokers download/like actions to the
 * shared feed infrastructure.
 */
class ProfileSamplesViewModel(
    private val ownerId: String,
    sampleRepo: SampleRepository = SampleRepositoryProvider.repository,
    profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    explicitStorageService: StorageService? = null,
    explicitDownloadsFolder: File? = null,
    explicitIoDispatcher: CoroutineDispatcher? = null,
    auth: FirebaseAuth? = FirebaseAuth.getInstance(),
    private val enableActions: Boolean = true,
) :
    BaseSampleFeedViewModel(
        sampleRepo = sampleRepo,
        profileRepo = profileRepo,
        auth = auth,
        storageService = explicitStorageService),
    SampleFeedController {

  private val _samples = MutableStateFlow<List<Sample>>(emptyList())
  val samples: StateFlow<List<Sample>> = _samples.asStateFlow()
  val downloadProgress = MutableStateFlow<Int?>(null)

  private val _currentUserFlow = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUserFlow.asStateFlow()

  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples.asStateFlow()

  private val downloadDispatcher: CoroutineDispatcher = explicitIoDispatcher ?: Dispatchers.IO

  override val actions: SampleUiActions? =
      if (!enableActions) {
        null
      } else {
        val storageService =
            explicitStorageService
                ?: StorageService(
                    FirebaseStorage.getInstance(
                        NepTuneApplication.appContext.getString(R.string.storage_path)))
        val downloadsFolder =
            DownloadDirectoryProvider.resolveDownloadsDir(
                NepTuneApplication.appContext, explicitDownloadsFolder)

        SampleUiActions(
            repo = sampleRepo,
            storageService = storageService,
            profileRepo = profileRepo,
            downloadsFolder = downloadsFolder,
            context = NepTuneApplication.appContext,
            ioDispatcher = downloadDispatcher,
            downloadProgress = downloadProgress)
      }

  init {
    observeOwnerSamples()
  }

  private fun observeOwnerSamples() {
    viewModelScope.launch {
      this@ProfileSamplesViewModel.sampleRepo.observeSamples().collectLatest { samples ->
        val filtered =
            samples
              .filter { sample ->
              sample.ownerId == ownerId &&
                  (sample.storageProcessedSamplePath.isNotBlank() ||
                      sample.storagePreviewSamplePath.isNotBlank())
            }
              .sortedByDescending { it.creationTime }
        _samples.value = filtered
        refreshLikeStates(filtered, _likedSamples)
      }
    }
  }

  override fun onDownloadZippedSample(sample: Sample) {
    val safeActions = actions ?: return
    viewModelScope.launch {
      try {
        withContext(downloadDispatcher) { safeActions.onDownloadZippedClicked(sample) }
      } catch (e: Exception) {
        Log.e("ProfileSamplesViewModel", "Error downloading sample: ${e.message}")
      }
    }
  }

  override fun onDownloadProcessedSample(sample: Sample) {
    val safeActions = actions ?: return
    viewModelScope.launch {
      try {
        withContext(downloadDispatcher) { safeActions.onDownloadProcessedClicked(sample) }
      } catch (e: Exception) {
        Log.e("ProfileSamplesViewModel", "Error downloading sample: ${e.message}")
      }
    }
  }

  override fun onLikeClick(sample: Sample, isLiked: Boolean) {
    viewModelScope.launch {
      this@ProfileSamplesViewModel.sampleRepo.toggleLike(sample.id, isLiked)
      val delta = if (isLiked) 1 else -1
      profileRepo.updateLikeCount(sample.ownerId, delta)
      _samples.update { list ->
        list.map { current ->
          if (current.id == sample.id) current.copy(likes = current.likes + delta) else current
        }
      }
      _likedSamples.update { it + (sample.id to isLiked) }
    }
  }
}
