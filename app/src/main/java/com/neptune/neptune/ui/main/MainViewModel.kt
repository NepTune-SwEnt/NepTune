package com.neptune.neptune.ui.main

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.R
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    private val imageRepo: ImageStorageRepository = ImageStorageRepository(),
) : ViewModel() {
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val actions: SampleUiActions? =
      if (useMockData) {
        null
      } else {
        SampleUiActions(repo, storageService, downloadsFolder, context)
      }

  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples
  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: StateFlow<List<Sample>> = _followedSamples

  private val _userAvatar = MutableStateFlow<Any?>(null)
  val userAvatar: StateFlow<Any?> = _userAvatar.asStateFlow()

  private val _currentUserFlow = MutableStateFlow(auth.currentUser)
  private val authListener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUserFlow.value = firebaseAuth.currentUser
      }

  private val avatarFileName: String?
    get() = auth.currentUser?.uid?.let { "avatar_$it.jpg" }

  private val avatarStoragePath: String?
    get() = auth.currentUser?.uid?.let { "profile_pictures/$it.jpg" }

  private val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments

  private val _likedSamples = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  val likedSamples: StateFlow<Map<String, Boolean>> = _likedSamples

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
      val storagePath = avatarStoragePath
      val fileName = avatarFileName

      if (storagePath != null && fileName != null) {
        val downloadUrl = storageService.getDownloadUrl(storagePath)

        if (downloadUrl != null) {
          imageRepo.saveImageFromUrl(downloadUrl, fileName)
        }
      }

      val localUri = if (fileName != null) imageRepo.getImageUri(fileName) else null
      if (localUri != null) {
        _userAvatar.value =
            localUri
                .buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
      } else {
        _userAvatar.value = null
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
      val username = profile?.username ?: "Anonymous"
      repo.addComment(sampleId, username, text.trim())
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
