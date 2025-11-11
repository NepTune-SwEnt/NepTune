package com.neptune.neptune.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the state and operations related to the samples. This has been written
 * with the help of LLMs.
 *
 * @param context The application context.
 * @property SampleRepository Repository for accessing and manipulating samples.
 * @property ProfileRepository Repository for accessing and manipulating profile.
 * @property useMockData false by default; true if we want to test with some MockData
 * @author Angéline Bignens
 */
class MainViewModel(
    context: Context,
    private val repo: SampleRepository = SampleRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val useMockData: Boolean = false,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples

  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: StateFlow<List<Sample>> = _followedSamples

  private val imageRepo = ImageStorageRepository(context)
  private val _userAvatar = MutableStateFlow<Any?>(null)
  val userAvatar: StateFlow<Any?> = _userAvatar.asStateFlow()

  private val _currentUserFlow = MutableStateFlow(auth.currentUser)
  private val authListener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUserFlow.value = firebaseAuth.currentUser
      }

  private var observeUserJob: Job? = null // Proposed by gemini

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: MutableStateFlow<List<Comment>> = _comments

  private val _likedSamples = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
  val likedSamples: MutableStateFlow<Map<Int, Boolean>> = _likedSamples

  init {
    if (useMockData) {
      // If we are testing we load mock data
      loadData()
    } else {
      loadSamplesFromFirebase()
    }
    auth.addAuthStateListener(authListener)
    observeUser()
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

  fun onResume() {
    observeUser()
  }

  override fun onCleared() {
    super.onCleared()
    auth.removeAuthStateListener(authListener)
  }

  /**
   * View the profile and verify the local avatar. Give priority to the local (modified) avatar over
   * the remote avatar. This function was made using AI assistance
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeUser() {
    observeUserJob?.cancel()
    observeUserJob =
        viewModelScope.launch {
          _currentUserFlow
              .flatMapLatest { firebaseUser ->
                if (firebaseUser == null) {
                  flowOf(null)
                } else {
                  profileRepo.observeProfile().map { profile ->
                    val dynamicFileName = "avatar_${firebaseUser.uid}.jpg"
                    val localUri = imageRepo.getImageUri(dynamicFileName)
                    if (localUri != null) {
                      localUri
                          .buildUpon()
                          .appendQueryParameter("t", System.currentTimeMillis().toString())
                          .build()
                    } else if (profile?.avatarUrl != null) {
                      profile.avatarUrl
                    } else {
                      null
                    }
                  }
                }
              }
              .collectLatest { avatar -> _userAvatar.value = avatar }
        }
  }

  fun onLikeClicked(sample: Sample, isLiked: Boolean) {
    viewModelScope.launch {

      // Check if already Liked
      val alreadyLiked = repo.hasUserLiked(sample.id)

      if (!alreadyLiked && isLiked) {
        repo.toggleLike(sample.id, true)
        _likedSamples.value = _likedSamples.value + (sample.id to true)
      } else if (alreadyLiked && !isLiked) {
        repo.toggleLike(sample.id, false)
        _likedSamples.value = _likedSamples.value + (sample.id to false)
      }
    }
  }

  fun refreshLikeStates() {
    viewModelScope.launch {
      val allSamples = _discoverSamples.value + _followedSamples.value

      val updatedStates = mutableMapOf<Int, Boolean>()
      for (sample in allSamples) {
        val liked = repo.hasUserLiked(sample.id)
        updatedStates[sample.id] = liked
      }
      _likedSamples.value = updatedStates
    }
  }

  fun observeCommentsForSample(sampleId: Int) {
    viewModelScope.launch { repo.observeComments(sampleId).collectLatest { _comments.value = it } }
  }

  fun addComment(sampleId: Int, text: String) {
    viewModelScope.launch {
      val profile = profileRepo.getProfile()
      val username = profile?.username ?: "Anonymous"
      repo.addComments(sampleId, username, text.trim())
    }
  }
  // Mock Data
  private fun loadData() {
    _discoverSamples.value =
        listOf(
            Sample(
                1, "Sample 1", "This is a sample description 1", 21, listOf("#nature"), 21, 21, 21),
            Sample(2, "Sample 2", "This is a sample description 2", 42, listOf("#sea"), 42, 42, 42),
            Sample(
                3, "Sample 3", "This is a sample description 3", 12, listOf("#relax"), 12, 12, 12),
            Sample(
                4, "Sample 4", "This is a sample description 4", 2, listOf("#takeItEasy"), 1, 2, 1),
        )
    _followedSamples.value =
        listOf(
            Sample(
                5,
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf("#nature", "#forest"),
                210,
                210,
                210),
            Sample(
                6,
                "Sample 6",
                "This is a sample description 6",
                80,
                listOf("#nature"),
                420,
                420,
                420),
        )
  }
}
