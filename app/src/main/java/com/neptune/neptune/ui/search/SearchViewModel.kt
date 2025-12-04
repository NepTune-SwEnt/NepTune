package com.neptune.neptune.ui.search

import android.content.Context
import android.os.Environment
import android.util.Log
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
import com.neptune.neptune.ui.main.SampleUiActions
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    sampleRepo: SampleRepository = SampleRepositoryProvider.repository,
    context: Context,
    private val useMockData: Boolean = false,
    profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    explicitStorageService: StorageService? = null,
    explicitDownloadsFolder: File? = null,
    auth: FirebaseAuth? = null
) :
    BaseSampleFeedViewModel(
        sampleRepo = sampleRepo,
        profileRepo = profileRepo,
        context = context,
        auth = if (useMockData) null else auth ?: FirebaseAuth.getInstance()),
    SampleFeedController {

  // ---------- Firebase auth (disabled in tests when useMockData = true) ----------

  private val _currentUserFlow = MutableStateFlow(auth?.currentUser)

  private val authListener: FirebaseAuth.AuthStateListener? =
      auth?.let {
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

  override val actions: SampleUiActions? =
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
            explicitDownloadsFolder ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir

          SampleUiActions(
            sampleRepo,
            storageService,
            downloadsFolder,
            context,
            downloadProgress = downloadProgress)
      }

  init {
    if (auth != null && authListener != null) {
      auth.addAuthStateListener(authListener)
    }
    load(useMockData)
  }

  override fun onCleared() {
    super.onCleared()
    if (auth != null && authListener != null) {
      auth.removeAuthStateListener(authListener)
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
      this@SearchViewModel.sampleRepo.observeSamples().collectLatest { samples ->
        val readySamples = samples.filter { it.storagePreviewSamplePath.isNotBlank() }
        allSamples.value = readySamples
        readySamples.forEach { loadSampleResources(it) }
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

  override fun onLikeClick(sample: Sample, isLiked: Boolean) {
    val sampleId = sample.id
    viewModelScope.launch {
      this@SearchViewModel.sampleRepo.toggleLike(sample.id, isLiked)
      val delta = if (isLiked) 1 else -1
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
      _likedSamples.value = _likedSamples.value + (sampleId to isLiked)
    }
  }

  fun refreshLikeStates() {
    refreshLikeStates(_samples.value, _likedSamples)
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
    val currentUserId = auth?.currentUser?.uid ?: return false
    return ownerId.isNotBlank() && ownerId == currentUserId
  }
}
