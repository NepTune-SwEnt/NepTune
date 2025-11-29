package com.neptune.neptune.ui.search

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.R
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.feed.BaseSampleFeedViewModel
import com.neptune.neptune.ui.feed.SampleFeedController
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.SampleUiActions
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val NATURE_TAG = "#nature"
/**
 * Search ViewModel Handles search logic, data loading, and user interactions for the Search Screen.
 * Uses: SampleRepository to fetch samples and manage likes/comments. Includes: Firebase
 * authentication handling, sample filtering based on search queries,
 *
 * written with assistance from ChatGPT
 */
open class SearchViewModel(
    repo: SampleRepository = SampleRepositoryProvider.repository,
    private val context: Context,
    private val useMockData: Boolean = false,
    profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    explicitStorageService: StorageService? = null,
    explicitDownloadsFolder: File? = null,
    private val auth: FirebaseAuth? = null
) : BaseSampleFeedViewModel(sampleRepo = repo, profileRepo = profileRepo), SampleFeedController {

  // ---------- Firebase auth (disabled in tests when useMockData = true) ----------

  private val firebaseAuth: FirebaseAuth? =
      if (useMockData) {
        null
      } else {
        auth ?: FirebaseAuth.getInstance()
      }

  private val _currentUserFlow = MutableStateFlow(firebaseAuth?.currentUser)

  private val authListener: FirebaseAuth.AuthStateListener? =
      firebaseAuth?.let {
        FirebaseAuth.AuthStateListener { fbAuth -> _currentUserFlow.value = fbAuth.currentUser }
      }

  // ---------- Samples & likes / comments ----------

  private val _samples = MutableStateFlow<List<Sample>>(emptyList())
  val samples: StateFlow<List<Sample>> = _samples

  private var query = ""
  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples
  private val allSamples = MutableStateFlow<List<Sample>>(emptyList())

  // ---------- Actions (download, etc.) – disabled in tests ----------
  val downloadProgress = MutableStateFlow<Int?>(null)

  val actions: SampleUiActions? =
      if (useMockData) {
        null
      } else {
        val storageService =
            explicitStorageService
                ?: run {
                  val storage =
                      FirebaseStorage.getInstance(context.getString(R.string.storage_path))
                  StorageService(storage)
                }

        val downloadsFolder =
            explicitDownloadsFolder
                ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        SampleUiActions(
            repo, storageService, downloadsFolder, context, downloadProgress = downloadProgress)
      }

  private val avatarCache = mutableMapOf<String, String?>()
  private val userNameCache = mutableMapOf<String, String>()
  private val coverImageCache = mutableMapOf<String, String?>()
  private val audioUrlCache = mutableMapOf<String, String?>()
  private val waveformCache = mutableMapOf<String, List<Float>>()

  init {
    if (firebaseAuth != null && authListener != null) {
      firebaseAuth.addAuthStateListener(authListener)
    }
    load(useMockData)
  }

  private suspend fun getSampleOwnerAvatar(userId: String): String? {
    if (avatarCache.containsKey(userId)) return avatarCache[userId]
    val url = profileRepo.getAvatarUrlByUserId(userId)
    avatarCache[userId] = url
    return url
  }

  private suspend fun getUserName(userId: String): String {
    if (userNameCache.containsKey(userId)) return userNameCache[userId] ?: "Anonymous"
    val userName = profileRepo.getUserNameByUserId(userId) ?: "Anonymous"
    userNameCache[userId] = userName
    return userName
  }

  private suspend fun getSampleCoverUrl(storagePath: String): String? {
    if (storagePath.isBlank()) return null
    if (coverImageCache.containsKey(storagePath)) return coverImageCache[storagePath]
    val url = actions?.getDownloadUrl(storagePath) ?: return null
    coverImageCache[storagePath] = url
    return url
  }

  private suspend fun getSampleAudioUrl(sample: Sample): String? {
    val storagePath = sample.storagePreviewSamplePath
    if (storagePath.isBlank()) return null
    if (audioUrlCache.containsKey(storagePath)) return audioUrlCache[storagePath]

    val url = actions?.getDownloadUrl(storagePath) ?: return null
    audioUrlCache[storagePath] = url
    return url
  }

  private suspend fun getSampleWaveform(sample: Sample): List<Float> {
    if (waveformCache.containsKey(sample.id))
        waveformCache[sample.id]?.let {
          return it
        }

    val audioUrl = getSampleAudioUrl(sample) ?: return emptyList()

    return try {
      val waveform =
          WaveformExtractor()
              .extractWaveform(context = context, uri = audioUrl.toUri(), samplesCount = 100)
      if (waveform.isNotEmpty()) {
        waveformCache[sample.id] = waveform
      }
      waveform
    } catch (e: Exception) {
      Log.e("SearchViewModel", "Waveform error: ${e.message}")
      emptyList()
    }
  }

  /** Function to trigger loading */
  override fun loadSampleResources(sample: Sample) {
    val currentResources = _sampleResources.value[sample.id]
    if (currentResources != null &&
        currentResources.loadedSamplePath == sample.storagePreviewSamplePath) {
      return
    }

    viewModelScope.launch {
      _sampleResources.update { current ->
        current +
            (sample.id to
                (current[sample.id]?.copy(isLoading = true)
                    ?: SampleResourceState(isLoading = true)))
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
                    isLoading = false,
                    loadedSamplePath = sample.storagePreviewSamplePath))
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    if (firebaseAuth != null && authListener != null) {
      firebaseAuth.removeAuthStateListener(authListener)
    }
  }

  // ---------- Data loading ----------

  // Mock data for tests (your original loadData)
  private fun loadMockData() {
    val mocks =
        listOf(
            Sample(
                "1",
                "Sample 1",
                "This is a sample description 1",
                21,
                listOf(NATURE_TAG),
                21,
                emptyList(),
                21,
                21),
            Sample(
                "2",
                "Sample 2",
                "This is a sample description 2",
                42,
                listOf("#sea"),
                42,
                emptyList(),
                42,
                42),
            Sample(
                "3", "Sample 3", "sea", 12, listOf("#relax", NATURE_TAG), 12, emptyList(), 12, 12),
            Sample(
                "4",
                "nature",
                "This is a sample description 4",
                2,
                listOf("#takeItEasy"),
                1,
                emptyList(),
                2,
                1),
            Sample(
                "5",
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf(NATURE_TAG, "#forest"),
                210,
                usersLike = emptyList(),
                210,
                210))
    allSamples.value = mocks
    applyFilter(query)
  }

  fun loadSamplesFromFirebase() {
    viewModelScope.launch {
      sampleRepo.observeSamples().collectLatest { samples ->
        val readySamples = samples.filter { it.storagePreviewSamplePath.isNotBlank() }
        allSamples.value = readySamples
        applyFilter(query)
        refreshLikeStates()
      }
    }
  }

  // ---------- Public API used by UI ----------

  override fun onDownloadSample(sample: Sample) {
    val safeActions = actions ?: return // no-op in tests
    viewModelScope.launch {
      try {
        safeActions.onDownloadClicked(sample)
        load(useMockData)
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error downloading sample: ${e.message}")
        // optional: log or expose error
      }
    }
  }

  override fun onLikeClick(sample: Sample, isLikedNow: Boolean) {
    val sampleId = sample.id
    viewModelScope.launch {
      sampleRepo.toggleLike(sample.id, isLikedNow)
      val delta = if (isLikedNow) 1 else -1
      val updatedSamples =
          allSamples.value.map { current ->
            if (current.id == sampleId) {
              current.copy(likes = current.likes + delta)
            } else {
              current
            }
          }
      allSamples.value = updatedSamples
      applyFilter(query)
      _likedSamples.value = _likedSamples.value + (sampleId to isLikedNow)
    }
  }

  fun refreshLikeStates() {
    viewModelScope.launch {
      val allSamples = _samples.value
      val updatedStates = mutableMapOf<String, Boolean>()
      for (sample in allSamples) {
        val liked = sampleRepo.hasUserLiked(sample.id)
        updatedStates[sample.id] = liked
      }
      _likedSamples.value = updatedStates
    }
  }

  open fun search(query: String) {
    this.query = query
    applyFilter(query)
  }

  // Normalizes text by converting it to lowercase and removing non-alphanumeric characters.
  fun normalize(text: String): String {
    return text.lowercase().replace("\\p{M}".toRegex(), "").replace(Regex("[^a-z0-9]"), "").trim()
  }

  private fun applyFilter(query: String) {
    val normalizedQuery = normalize(query)
    val base = allSamples.value
    _samples.value =
        if (normalizedQuery.isEmpty()) {
          base
        } else
            base.filter {
              normalize(it.name).contains(normalizedQuery, ignoreCase = true) ||
                  normalize(it.description).contains(normalizedQuery, ignoreCase = true) ||
                  it.tags.any { tag -> normalize(tag).contains(normalizedQuery, ignoreCase = true) }
            }
  }

  private fun load(useMock: Boolean) {
    if (useMock) {
      loadMockData()
    } else {
      loadSamplesFromFirebase()
    }
  }

  /** True when [ownerId] refers to the currently signed-in Firebase user. */
  override fun isCurrentUser(ownerId: String): Boolean {
    val currentUserId = firebaseAuth?.currentUser?.uid ?: return false
    return !ownerId.isNullOrBlank() && ownerId == currentUserId
  }
}
