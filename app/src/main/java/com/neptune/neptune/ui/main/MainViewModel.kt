package com.neptune.neptune.ui.main

import android.os.Environment
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
import com.neptune.neptune.model.recommendation.RecommendationEngine
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.feed.BaseSampleFeedViewModel
import com.neptune.neptune.ui.feed.SampleFeedController
import com.neptune.neptune.util.AudioWaveformExtractor
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SampleResourceState(
    val ownerName: String = "",
    val ownerAvatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val audioUrl: String? = null,
    val waveform: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val loadedSamplePath: String? = null
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
open class MainViewModel(
    sampleRepo: SampleRepository = SampleRepositoryProvider.repository,
    profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    storageService: StorageService =
        StorageService(
            FirebaseStorage.getInstance(
                NepTuneApplication.appContext.getString(R.string.storage_path))),
    private val useMockData: Boolean = false,
    downloadsFolder: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    auth: FirebaseAuth? = null,
    waveformExtractor: AudioWaveformExtractor = WaveformExtractor()
) :
    BaseSampleFeedViewModel(
        sampleRepo = sampleRepo,
        profileRepo = profileRepo,
        auth = auth ?: if (useMockData) null else FirebaseAuth.getInstance(),
        storageService = storageService,
        waveformExtractor = waveformExtractor),
    SampleFeedController {
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val downloadProgress = MutableStateFlow<Int?>(null)

  override val actions: SampleUiActions? =
      if (useMockData) {
        null
      } else {
        SampleUiActions(
            sampleRepo,
            storageService,
            downloadsFolder,
            NepTuneApplication.appContext,
            downloadProgress = downloadProgress)
      }

  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples
  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: StateFlow<List<Sample>> = _followedSamples

  private val _userAvatar = MutableStateFlow<String?>(null)
  val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()
  private val firebaseAuth: FirebaseAuth? =
      auth ?: if (useMockData) null else FirebaseAuth.getInstance()

  private val _currentUserFlow = MutableStateFlow(firebaseAuth?.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUserFlow.asStateFlow()
  private val observingSampleIds = mutableSetOf<String>()
  private val authListener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUserFlow.value = firebaseAuth.currentUser
        observeUserProfile()
      }

  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples
  private var allSamplesCache: List<Sample> = emptyList()
  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
  private val _isAnonymous = MutableStateFlow(auth?.currentUser?.isAnonymous ?: true)
  val isAnonymous: StateFlow<Boolean> = _isAnonymous.asStateFlow()
  private val _recommendedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val recommendedSamples: StateFlow<List<Sample>> = _recommendedSamples
  private var latestFollowing: List<String> = emptyList()

  init {
    if (useMockData) {
      // If we are testing we load mock data
      loadData()
    } else {
      loadSamplesFromFirebase()
    }
    firebaseAuth?.addAuthStateListener(authListener)
    observeUserProfile()
  }

  fun loadRecommendations(limit: Int = 50) {
    viewModelScope.launch {
      try {
        Log.d("RecoDebug", "loadRecommendations() START, cacheSize=${allSamplesCache.size}")
        if (auth?.currentUser == null) return@launch
        val recoUser = profileRepo.getCurrentRecoUserProfile()
        if (recoUser == null) {
          // Fallback when no user or profile: just show latest samples
          Log.d("RecoDebug", "No recoUser profile (null) – skipping recommendations")

          _recommendedSamples.value = emptyList()
          return@launch
        }
        val candidates = allSamplesCache
        if (candidates.isEmpty()) {
          Log.d("RecoDebug", "No candidates (cache empty) – skipping ranking")
          _recommendedSamples.value = emptyList()
          return@launch
        }
        val ranked =
            RecommendationEngine.rankSamplesForUser(
                user = recoUser, candidates = candidates, limit = limit)
        ranked.forEachIndexed { index, sample ->
          val score = RecommendationEngine.scoreSample(sample, recoUser, System.currentTimeMillis())
          Log.d(
              "RecoDebug",
              "#$index  id=${sample.id}  name=${sample.name}  score=${"%.4f".format(score)}")
        }
        _recommendedSamples.value = ranked
        _discoverSamples.value = ranked
      } catch (e: Exception) {
        Log.e("MainViewModel", "Error loading recommendations: ${e.message}")
      }
    }
  }

  private fun loadSamplesFromFirebase() {
    if (auth?.currentUser == null) return
    viewModelScope.launch {
      try {
        val uid = auth.currentUser?.uid ?: return@launch
        combine(sampleRepo.observeSamples(), profileRepo.observeFollowingIds(uid)) {
                samples,
                following ->
              samples to following
            }
            .collectLatest { (rawSamples, following) ->
              latestFollowing = following
              val currentUserId = auth.currentUser?.uid
              val visibleSamples =
                  rawSamples.filter { sample ->
                    sample.isPublic ||
                        sample.ownerId == currentUserId ||
                        (sample.ownerId in following)
                  }

              val existingIds = allSamplesCache.map { it.id }.toSet()
              allSamplesCache = visibleSamples

              val readySamples = visibleSamples.filter { it.storagePreviewSamplePath.isNotBlank() }
              updateLists(readySamples)

              val pendingSamples = visibleSamples.filter { it.storagePreviewSamplePath.isBlank() }
              pendingSamples.forEach { pendingSample -> watchPendingSample(pendingSample.id) }

              val newSamples = visibleSamples.filter { it.id !in existingIds }
              newSamples.forEach { loadSampleResources(it) }

              refreshLikeStates()
              if (_isRefreshing.value) {
                _isRefreshing.value = false
              }
              viewModelScope.launch { loadRecommendations() }
            }
      } catch (e: Exception) {
        Log.e("MainViewModel", "Error loading samples", e)
        _isRefreshing.value = false
      }
    }
  }

  private fun watchPendingSample(sampleId: String) {
    if (observingSampleIds.contains(sampleId)) return
    viewModelScope.launch {
      try {
        observingSampleIds.add(sampleId)
        sampleRepo
            .observeSample(sampleId)
            .first { updatedSample ->
              updatedSample != null && updatedSample.storagePreviewSamplePath.isNotBlank()
            }
            ?.let { finishedSample ->
              allSamplesCache =
                  allSamplesCache.map { if (it.id == finishedSample.id) finishedSample else it }
              addSampleToList(finishedSample)
            }
      } catch (e: Exception) {
        Log.w("MainViewModel", "Stop watching sample $sampleId", e)
      } finally {
        observingSampleIds.remove(sampleId)
      }
    }
  }

  private fun updateLists(samples: List<Sample>) {
    _discoverSamples.value = samples.filter { it.ownerId !in latestFollowing }
    _followedSamples.value = samples.filter { it.ownerId in latestFollowing }
  }

  private fun addSampleToList(newSample: Sample) {
    if (newSample.ownerId !in latestFollowing) {
      _discoverSamples.update { currentList ->
        if (currentList.any { it.id == newSample.id }) currentList
        else listOf(newSample) + currentList
      }
    } else {
      _followedSamples.update { currentList ->
        if (currentList.any { it.id == newSample.id }) currentList
        else listOf(newSample) + currentList
      }
    }

    loadSampleResources(newSample)
  }

  override fun onCleared() {
    super.onCleared()
    auth?.removeAuthStateListener(authListener)
  }

  private fun observeUserProfile() {
    val currentUser = auth?.currentUser
    if (currentUser == null) {
      _userAvatar.value = null
      latestFollowing = emptyList()
      updateLists(allSamplesCache.filter { it.storagePreviewSamplePath.isNotBlank() })
      return
    }
    viewModelScope.launch {
      try {
        profileRepo.observeCurrentProfile().collectLatest { profile ->
          // Update the user avatar
          val newAvatarUrl = profile?.avatarUrl
          _userAvatar.value = newAvatarUrl
          _isAnonymous.value = profile?.isAnonymous ?: auth.currentUser?.isAnonymous ?: true

          // Update username
          val newUsername = profile?.username
          if (newUsername != null) {
            val old = _usernames.value[currentUser.uid]
            if (old == null || old != newUsername) {
              _usernames.update { it + (currentUser.uid to newUsername) }
            }
          }

          // Update avatar for samplers owned by current user
          val currentUserId = auth.currentUser?.uid ?: return@collectLatest
          updateAvatarCache(currentUserId, newAvatarUrl)
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
      } catch (e: Exception) {
        Log.e("MainViewModel", "Error observing profile: ${e.message}")
      }
    }
  }

  override fun onDownloadSample(sample: Sample) {
    viewModelScope.launch {
      try {
        actions?.onDownloadClicked(sample)
      } catch (e: Exception) {
        Log.e("MainViewModel", "Error downloading sample: ${e.message}")
        // Handle exception if needed
      }
    }
  }

  override fun onLikeClick(sample: Sample, isLiked: Boolean) {
    if (_isAnonymous.value) return
    viewModelScope.launch {
      try {
        val newState = actions?.onLikeClicked(sample, isLiked)
        if (newState != null) {
          _likedSamples.value = _likedSamples.value + (sample.id to newState)
        }
      } catch (e: Exception) {
        Log.e("MainViewModel", "Failed to toggle like: ${e.message}")
      }
    }
  }

  fun refreshLikeStates() {
    val allSamples = _discoverSamples.value + _followedSamples.value
    super.refreshLikeStates(allSamples, _likedSamples)
  }

  fun addComment(sampleId: String, text: String) {
    if (_isAnonymous.value) return
    viewModelScope.launch {
      val profile = profileRepo.getCurrentProfile()
      val authorId = profile?.uid ?: auth?.currentUser?.uid ?: "unknown"
      val authorName = profile?.username ?: defaultName
      val authorProfilePicUrl = userAvatar.value ?: ""
      sampleRepo.addComment(sampleId, authorId, authorName, text.trim(), authorProfilePicUrl)
    }
  }

  /** Function to be called when a refresh is triggered. */
  fun refresh() {
    _isRefreshing.value = true
    // allSamplesCache = emptyList()
    loadSamplesFromFirebase()
  }

  /** Disconnect the user */
  fun signOut() {
    firebaseAuth?.signOut()
    _currentUserFlow.value = null
  }

  /** Function to open the comment section. */
  fun openCommentSection(sample: Sample) {
    onCommentClicked(sample)
  }
  /** Function to close the comment section. */
  fun closeCommentSection() {
    resetCommentSampleId()
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
