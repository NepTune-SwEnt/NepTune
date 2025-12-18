package com.neptune.neptune.ui.search

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.Profile
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
import kotlin.collections.forEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val NATURE_TAG = "#nature"

enum class SearchType(val title: String) {
  SAMPLES("Samples"),
  USERS("Users");

  fun toggle(): SearchType {
    return if (this == SAMPLES) USERS else SAMPLES
  }
}
/**
 * Search ViewModel Handles search logic, data loading, and user interactions for the Search Screen.
 * Uses: SampleRepository to fetch samples and manage likes/comments. Includes: Firebase
 * authentication handling, sample filtering based on search queries,
 *
 * written with assistance from ChatGPT
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class SearchViewModel(
    sampleRepo: SampleRepository = SampleRepositoryProvider.repository,
    private val useMockData: Boolean = false,
    profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    explicitStorageService: StorageService? = null,
    explicitDownloadsFolder: File? = null,
    auth: FirebaseAuth? = null,
) :
    BaseSampleFeedViewModel(
        sampleRepo = sampleRepo,
        profileRepo = profileRepo,
        auth = if (useMockData) null else auth ?: FirebaseAuth.getInstance(),
        storageService = explicitStorageService),
    SampleFeedController {

  // ---------- Firebase auth (disabled in tests when useMockData = true) ----------
  private val effectiveAuth = if (useMockData) null else auth ?: FirebaseAuth.getInstance()
  private val _currentUserFlow = MutableStateFlow(auth?.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUserFlow.asStateFlow()

  private val authListener: FirebaseAuth.AuthStateListener? =
      effectiveAuth?.let {
        FirebaseAuth.AuthStateListener { fbAuth ->
          _currentUserFlow.value = fbAuth.currentUser
          if (fbAuth.currentUser != null) {
            load(useMockData)
          }
        }
      }

  // ---------- Samples & likes / comments ----------

  private val _samples = MutableStateFlow<List<Sample>>(emptyList())
  val samples: StateFlow<List<Sample>> = _samples

  private var query = ""
  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples
  private val allSamples = MutableStateFlow<List<Sample>>(emptyList())
  // Search Type State
  private val _searchType = MutableStateFlow(SearchType.SAMPLES)
  val searchType: StateFlow<SearchType> = _searchType.asStateFlow()

  private val _isAnonymous = MutableStateFlow(auth?.currentUser?.isAnonymous ?: true)
  val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()
  // User search results
  private val _userResults = MutableStateFlow<List<Profile>>(emptyList())
  val userResults: StateFlow<List<Profile>> = _userResults.asStateFlow()

  // Current User Profile (metadata needed by the UI)
  // Logic: Observe the current user flow. If a user is logged in, observe their profile.
  // If no user is logged in, emit null.
  @OptIn(ExperimentalCoroutinesApi::class)
  val currentUserProfile: StateFlow<Profile?> =
      _currentUserFlow
          .flatMapLatest { user ->
            if (user == null) {
              flowOf(null)
            } else {
              profileRepo.observeProfile(user.uid)
            }
          }
          .catch { e ->
            Log.e("SearchViewModel", "Error observing profile", e)
            emit(null)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // Local state for following IDs to ensure instant UI updates
  private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
  val followingIds: StateFlow<Set<String>> = _followingIds.asStateFlow()

  init {
    if (effectiveAuth != null && authListener != null) {
      effectiveAuth.addAuthStateListener(authListener)
    }
    viewModelScope.launch {
      try {
        _currentUserFlow
            .flatMapLatest { user ->
              if (user == null) {
                flowOf(emptySet<String>())
              } else {
                profileRepo.observeFollowingIds(user.uid)
              }
            }
            .collectLatest { ids -> _followingIds.value = ids.toSet() }
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error collecting current user profile", e)
      }
    }
    load(useMockData)
  }

  fun toggleSearchType() {
    _searchType.update { it.toggle() }
    // Re-trigger search with the new type
    search(query)
  }

  fun toggleFollow(userId: String, isFollowing: Boolean) {
    // 1. Optimistic update for the Button (following state) to keep UI responsive
    val currentFollowing = _followingIds.value
    val newFollowing =
        if (isFollowing) {
          currentFollowing - userId
        } else {
          currentFollowing + userId
        }
    _followingIds.value = newFollowing

    // 2. Perform network request and update follower count ONLY after processing
    viewModelScope.launch {
      try {
        if (isFollowing) {
          profileRepo.unfollowUser(userId)
        } else {
          profileRepo.followUser(userId)
        }

        // Action processed successfully: Now update the follower count in the list
        _userResults.update { currentList ->
          currentList.map { profile ->
            if (profile.uid == userId) {
              val newCount = if (isFollowing) profile.subscribers - 1 else profile.subscribers + 1
              profile.copy(subscribers = newCount)
            } else {
              profile
            }
          }
        }
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Failed to toggle follow for $userId", e)
        // Revert optimistic button update on failure
        _followingIds.value = currentFollowing
      }
    }
  }
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
                      FirebaseStorage.getInstance(
                          NepTuneApplication.appContext.getString(R.string.storage_path))
                  StorageService(storage)
                }

        val downloadsFolder =
            DownloadDirectoryProvider.resolveDownloadsDir(
                NepTuneApplication.appContext, explicitDownloadsFolder)

        SampleUiActions(
            sampleRepo,
            storageService,
            downloadsFolder,
            downloadProgress = downloadProgress)
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
    if (auth?.currentUser == null) return
    viewModelScope.launch {
      try {
        this@SearchViewModel.sampleRepo.observeSamples().collectLatest { samples ->
          val currentUserId = auth.currentUser?.uid
          val following = _followingIds.value
          val visibleSamples =
              samples.filter { sample ->
                sample.isPublic || sample.ownerId == currentUserId || (sample.ownerId in following)
              }
          val readySamples =
              visibleSamples.filter {
                it.storageProcessedSamplePath.isNotBlank() ||
                    it.storagePreviewSamplePath.isNotBlank()
              }
          allSamples.value = readySamples
          readySamples.forEach { loadSampleResources(it) }
          applyFilter(query)
          refreshLikeStates()
        }
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error loading samples (Offline?)", e)
      }
    }
  }

  // ---------- Public API used by UI ----------

  override fun onDownloadZippedSample(sample: Sample) {
    val safeActions = actions ?: return // no-op in tests
    viewModelScope.launch {
      try {
        safeActions.onDownloadZippedClicked(sample)
        load(useMockData)
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error downloading sample: ${e.message}")
        // optional: log or expose error
      }
    }
  }

  override fun onDownloadProcessedSample(sample: Sample) {
    val safeActions = actions ?: return // no-op in tests
    viewModelScope.launch {
      try {
        safeActions.onDownloadProcessedClicked(sample)
        load(useMockData)
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error downloading sample: ${e.message}")
      }
    }
  }

  override fun onLikeClick(sample: Sample, isLiked: Boolean) {
    val sampleId = sample.id
    viewModelScope.launch {
      try {
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
      } catch (e: Exception) {
        Log.e("SearchViewModel", "Error toggling like: ${e.message}")
      }
    }
  }

  fun refreshLikeStates() {
    refreshLikeStates(_samples.value, _likedSamples)
  }

  fun addComment(sampleId: String, text: String) {
    onAddComment(sampleId, text)
  }

  open fun search(query: String) {
    this.query = query
    viewModelScope.launch {
      if (_searchType.value == SearchType.SAMPLES) {
        applyFilter(query)
      } else {
        // Perform user search via ProfileRepository
        try {
          _userResults.value = profileRepo.searchUsers(query)
        } catch (e: Exception) {
          Log.e("SearchViewModel", "User search failed", e)
          _userResults.value = emptyList()
        }
      }
    }
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
