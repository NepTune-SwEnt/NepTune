package com.neptune.neptune.ui.search

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.R
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.SampleUiActions
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val repo: SampleRepository = SampleRepositoryProvider.repository,
    private val context: Context,
    private val useMockData: Boolean = false,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    explicitStorageService: StorageService? = null,
    explicitDownloadsFolder: File? = null,
    private val auth: FirebaseAuth? = null
) : ViewModel() {
  private val defaultName = "anonymous"

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

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments
  private var query = ""
  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples
  private val allSamples = MutableStateFlow<List<Sample>>(emptyList())
  private var allSamplesCache: List<Sample> = emptyList()
  private val _activeCommentSampleId = MutableStateFlow<String?>(null)
  val activeCommentSampleId: StateFlow<String?> = _activeCommentSampleId.asStateFlow()
  private val _sampleResources = MutableStateFlow<Map<String, SampleResourceState>>(emptyMap())
  val sampleResources = _sampleResources.asStateFlow()

  private val _usernames = MutableStateFlow<Map<String, String>>(emptyMap())
  val usernames: StateFlow<Map<String, String>> = _usernames.asStateFlow()

  fun onCommentClicked(sample: Sample) {
    observeCommentsForSample(sample.id)
    _activeCommentSampleId.value = sample.id
  }

  fun onAddComment(sampleId: String, text: String) {
    addComment(sampleId, text)
    observeCommentsForSample(sampleId)
  }
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
  fun loadSampleResources(sample: Sample) {
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
      repo.observeSamples().collectLatest { remoteSamples ->
        if (allSamplesCache.isEmpty()) {
          allSamplesCache = remoteSamples
          val readySamples = remoteSamples.filter { it.storagePreviewSamplePath.isNotBlank() }
          readySamples.forEach { loadSampleResources(it) }
        } else {
          val existingIds = allSamplesCache.map { it.id }.toSet()
          val newSamples = remoteSamples.filter { it.id !in existingIds }
          allSamplesCache = remoteSamples
          newSamples
              .filter { it.storagePreviewSamplePath.isNotBlank() }
              .forEach { loadSampleResources(it) }
        }
        val validSamples = allSamplesCache.filter { it.storagePreviewSamplePath.isNotBlank() }
        allSamples.value = validSamples
        applyFilter(query)
        refreshLikeStates()
      }
    }
  }

  // ---------- Public API used by UI ----------

  fun onDownloadSample(sample: Sample) {
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

  fun onLikeClick(sample: Sample, isLikedNow: Boolean) {
    val sampleId = sample.id
    viewModelScope.launch {
      repo.toggleLike(sample.id, isLikedNow)
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
        val liked = repo.hasUserLiked(sample.id)
        updatedStates[sample.id] = liked
      }
      _likedSamples.value = updatedStates
    }
  }

  fun observeCommentsForSample(sampleId: String) {
    viewModelScope.launch {
      repo.observeComments(sampleId).collectLatest { list ->
        // Ensure usernames are loaded for each author
        list.forEach { comment -> loadUsername(comment.authorId) }
        _comments.value = list
      }
    }
  }

  /*
   * Function to load the user name.
   */
  fun loadUsername(userId: String) {
    viewModelScope.launch {
      // Check cache first
      val cached = _usernames.value[userId]
      if (cached != null && cached != defaultName) return@launch

      // Fetch latest username
      val userName = profileRepo.getUserNameByUserId(userId) ?: defaultName

      _usernames.update { it + (userId to userName) }
    }
  }

  fun resetCommentSampleId() {
    _activeCommentSampleId.value = null
  }

  fun addComment(sampleId: String, text: String) {
    viewModelScope.launch {
      val profile = profileRepo.getCurrentProfile()
      val authorId = profile?.uid ?: auth?.currentUser?.uid ?: "unknown"
      val authorName = profile?.username ?: defaultName
      repo.addComment(sampleId, authorId, authorName, text.trim())
      observeCommentsForSample(sampleId)
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
  open fun isCurrentUser(ownerId: String?): Boolean {
    val currentUserId = firebaseAuth?.currentUser?.uid ?: return false
    return !ownerId.isNullOrBlank() && ownerId == currentUserId
  }
}
