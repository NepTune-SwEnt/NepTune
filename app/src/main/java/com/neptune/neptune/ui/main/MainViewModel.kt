package com.neptune.neptune.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.Sample
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
  // No real data for now
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val discoverSamples: StateFlow<List<Sample>> = _discoverSamples

  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: StateFlow<List<Sample>> = _followedSamples

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository
  private val imageRepo = ImageStorageRepository(application.applicationContext)

  /** Generates a user-specific filename for the avatar. */
  private val avatarFileName: String?
    get() = auth.currentUser?.uid?.let { "avatar_$it.jpg" }

  private val _userAvatar = MutableStateFlow<Any?>(null)
  val userAvatar: StateFlow<Any?> = _userAvatar.asStateFlow()

  private val _currentUserFlow = MutableStateFlow(auth.currentUser)
  private val authListener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUserFlow.value = firebaseAuth.currentUser
      }

  private var observeUserJob: Job? = null // Proposed by gemini

  init {
    loadData()
    auth.addAuthStateListener(authListener)
    observeUser()
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

  // Todo: Replace with actual data from the repository
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
