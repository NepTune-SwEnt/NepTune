package com.neptune.neptune.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val useMockData: Boolean = false
) : ViewModel() {
  private val _discoverSamples = MutableStateFlow<List<Sample>>(emptyList())
  val discoverSamples: MutableStateFlow<List<Sample>> = _discoverSamples

  private val _followedSamples = MutableStateFlow<List<Sample>>(emptyList())
  val followedSamples: MutableStateFlow<List<Sample>> = _followedSamples

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
      repo.addComments(sampleId, username, text)
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
