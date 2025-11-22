package com.neptune.neptune.ui.main

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.util.AudioWaveformExtractor
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleResourceState(
    val ownerName: String = "",
    val ownerAvatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val audioUrl: String? = null,
    val waveform: List<Float> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel for managing the state and operations related to the samples. This has been written
 * with the help of LLMs.
 *
 * @property SampleRepository Repository for accessing and manipulating samples.
 * @property ProfileRepository Repository for accessing and manipulating profile.
 * @property useMockData false by default; true if we want to test with some MockData
 * @author Angéline Bignens
 */
class MainViewModel(
    private val repo: SampleRepository = SampleRepositoryProvider.repository,
    context: Context,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private var storageService: StorageService =
        StorageService(FirebaseStorage.getInstance(context.getString(R.string.storage_path))),
    private val useMockData: Boolean = false,
    downloadsFolder: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val waveformExtractor: AudioWaveformExtractor = WaveformExtractor()
) : ViewModel() {
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val downloadProgress = MutableStateFlow<Int?>(null)

  private val defaultName = "anonymous"
  val actions: SampleUiActions? =
      if (useMockData) {
        null
      } else {
        SampleUiActions(
            repo, storageService, downloadsFolder, context, downloadProgress = downloadProgress)
      }

  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples
  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: StateFlow<List<Sample>> = _followedSamples

  private val _userAvatar = MutableStateFlow<String?>(null)
  val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

  private val _currentUserFlow = MutableStateFlow(auth.currentUser)
  private val authListener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUserFlow.value = firebaseAuth.currentUser
        loadAvatar()
      }

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments

  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples
  private val avatarCache = mutableMapOf<String, String?>()
  private val userNameCache = mutableMapOf<String, String>()
  private val coverImageCache = mutableMapOf<String, String?>()
  private val audioUrlCache = mutableMapOf<String, String?>()
  private val waveformCache = mutableMapOf<String, List<Float>>()
  private val _sampleResources = MutableStateFlow<Map<String, SampleResourceState>>(emptyMap())
  val sampleResources = _sampleResources.asStateFlow()

  init {
    if (useMockData) {
      // If we are testing we load mock data
      loadData()
    } else {
      loadSamplesFromFirebase()
    }
    auth.addAuthStateListener(authListener)
    loadAvatar()
  }

  private fun loadSamplesFromFirebase() {
    viewModelScope.launch {
      // Get current user's profile
      val profile = profileRepo.getProfile()
      val following = profile?.following.orEmpty()
      repo.observeSamples().collectLatest { samples ->
        _discoverSamples.value = samples.filter { it.ownerId !in following }
        _followedSamples.value = samples.filter { it.ownerId in following }

        refreshLikeStates()
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    auth.removeAuthStateListener(authListener)
  }

  private fun loadAvatar() {
    viewModelScope.launch {
      profileRepo.observeProfile().collectLatest { profile ->
        // Update the user avatar
        val newAvatarUrl = profile?.avatarUrl
        _userAvatar.value = newAvatarUrl
        // Update avatar for samplers owned by current user
        val currentUserId = auth.currentUser?.uid ?: return@collectLatest
        avatarCache[currentUserId] = newAvatarUrl
        _sampleResources.update { currentResources ->
          val updatedResources = currentResources.toMutableMap()
          val allLoadedSamples = _discoverSamples.value + _followedSamples.value
          val mySamples = allLoadedSamples.filter { it.ownerId == currentUserId }
          mySamples.forEach { sample ->
            val currentState = updatedResources[sample.id]
            if (currentState != null) {
              updatedResources[sample.id] = currentState.copy(ownerAvatarUrl = newAvatarUrl)
            }
          }
          updatedResources
        }
      }
    }
  }

  fun onDownloadSample(sample: Sample) {
    viewModelScope.launch {
      try {
        actions?.onDownloadClicked(sample)
      } catch (e: Exception) {
        Log.e("MainViewModel", "Error downloading sample: ${e.message}")
        // Handle exception if needed
      }
    }
  }

  fun onLikeClicked(sample: Sample, isLiked: Boolean) {
    viewModelScope.launch {
      val newState = actions?.onLikeClicked(sample.id, isLiked)
      if (newState != null) {
        _likedSamples.value = _likedSamples.value + (sample.id to newState)
      }
    }
  }

  fun refreshLikeStates() {
    viewModelScope.launch {
      val allSamples = _discoverSamples.value + _followedSamples.value

      val updatedStates = mutableMapOf<String, Boolean>()
      for (sample in allSamples) {
        val liked = repo.hasUserLiked(sample.id)
        updatedStates[sample.id] = liked
      }
      _likedSamples.value = updatedStates
    }
  }

  fun observeCommentsForSample(sampleId: String) {
    viewModelScope.launch { repo.observeComments(sampleId).collectLatest { _comments.value = it } }
  }

  fun addComment(sampleId: String, text: String) {
    viewModelScope.launch {
      val profile = profileRepo.getProfile()
      val username = profile?.username ?: defaultName
      repo.addComment(sampleId, username, text.trim())
    }
  }

  /*
   * function to get the avatar of the sample owner.
   */
  private suspend fun getSampleOwnerAvatar(userId: String): String? {
    if (avatarCache.containsKey(userId)) {
      return avatarCache[userId]
    }
    val url = profileRepo.getAvatarUrlByUserId(userId)
    avatarCache[userId] = url
    return url
  }

  /*
   * Function to get the user name.
   */
  private suspend fun getUserName(userId: String): String {
    if (userNameCache.containsKey(userId)) {
      return userNameCache[userId] ?: defaultName
    }
    var userName = profileRepo.getUserNameByUserId(userId)
    userName = userName ?: defaultName
    userNameCache[userId] = userName
    return userName
  }

  /*
   * Function to get the Download URL from the storage path.
   */
  private suspend fun getSampleCoverUrl(storagePath: String): String? {
    if (storagePath.isBlank()) return null

    if (coverImageCache.containsKey(storagePath)) {
      return coverImageCache[storagePath]
    }
    val url = storageService.getDownloadUrl(storagePath)
    coverImageCache[storagePath] = url
    return url
  }

  /*
   * Function to get the Audio URL from the storage path.
   */
  private suspend fun getSampleAudioUrl(sample: Sample): String? {
    val storagePath = sample.storagePreviewSamplePath
    if (storagePath.isBlank()) return null
    if (audioUrlCache.containsKey(storagePath)) {
      return audioUrlCache[storagePath]
    }
    val url = storageService.getDownloadUrl(storagePath)
    audioUrlCache[storagePath] = url
    return url
  }

  /*
   * Retrieves the waveform for a given Sample.
   */

  private suspend fun getSampleWaveform(sample: Sample): List<Float> {
    if (waveformCache.containsKey(sample.id)) {
      waveformCache[sample.id]?.let {
        return it
      }
    }
    val audioUrl = getSampleAudioUrl(sample) ?: return emptyList()

    return try {
      val waveform =
          waveformExtractor.extractWaveform(
              context = NepTuneApplication.appContext, uri = audioUrl.toUri(), samplesCount = 100)

      if (waveform.isNotEmpty()) {
        waveformCache[sample.id] = waveform
      }
      waveform
    } catch (e: Exception) {
      Log.e("MainViewModel", "Error extracting waveform for list: ${e.message}")
      emptyList()
    }
  }

  /** Function to trigger loading */
  fun loadSampleResources(sample: Sample) {
    if (_sampleResources.value.containsKey(sample.id)) return

    viewModelScope.launch {
      _sampleResources.update { current ->
        current + (sample.id to SampleResourceState(isLoading = true))
      }

      val avatarUrl = getSampleOwnerAvatar(sample.ownerId)
      val userName = getUserName(sample.ownerId)
      val coverUrl =
          if (sample.storageImagePath.isNotBlank()) getSampleCoverUrl(sample.storageImagePath)
          else null
      val audioUrl =
          if (sample.storagePreviewSamplePath.isNotBlank()) getSampleAudioUrl(sample) else null
      val waveform = getSampleWaveform(sample)

      _sampleResources.update { current ->
        current +
            (sample.id to
                SampleResourceState(
                    ownerName = userName,
                    ownerAvatarUrl = avatarUrl,
                    coverImageUrl = coverUrl,
                    audioUrl = audioUrl,
                    waveform = waveform,
                    isLoading = false))
      }
    }
  }

  // Mock Data
  private fun loadData() {
    _discoverSamples.value =
        listOf(
            Sample(
                "1",
                "Sample 1",
                "This is a sample description 1",
                21,
                listOf("#nature"),
                21,
                usersLike = emptyList(),
                21,
                21),
            Sample(
                "2",
                "Sample 2",
                "This is a sample description 2",
                42,
                listOf("#sea"),
                42,
                usersLike = emptyList(),
                42,
                42),
            Sample(
                "3",
                "Sample 3",
                "This is a sample description 3",
                12,
                listOf("#relax"),
                12,
                usersLike = emptyList(),
                12,
                12),
            Sample(
                "4",
                "Sample 4",
                "This is a sample description 4",
                2,
                listOf("#takeItEasy"),
                1,
                usersLike = emptyList(),
                2,
                1),
        )
    _followedSamples.value =
        listOf(
            Sample(
                "5",
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf("#nature", "#forest"),
                210,
                usersLike = emptyList(),
                210,
                210),
            Sample(
                "6",
                "Sample 6",
                "This is a sample description 6",
                80,
                listOf("#nature"),
                420,
                usersLike = emptyList(),
                420,
                420),
        )
  }
}
