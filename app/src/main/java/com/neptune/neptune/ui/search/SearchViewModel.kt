package com.neptune.neptune.ui.search

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.data.ImageStorageRepository
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import com.neptune.neptune.ui.main.SampleUiActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.plus

/*
Search ViewModel
Holds the list of samples and performs search filtering
Uses: MutableStateFlow to hold the list of samples
Provides: search function to filter samples based on query
Written with assistance from ChatGPT
 */

const val NATURE_TAG = "#nature"
val storage = FirebaseStorage.getInstance("gs://neptune-e2728.firebasestorage.app")
val downloadsFolder: File =
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
val storageService: StorageService = StorageService(storage)

open class SearchViewModel(private val repo: SampleRepository = SampleRepositoryProvider.repository,
                           context: Context, private val useMockData: Boolean = false, private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
                           private val storageService: StorageService = StorageService(storage),
                           private val downloadsFolder: File =
                               Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                          auth: FirebaseAuth = FirebaseAuth.getInstance()) : ViewModel() {
  private val _samples = MutableStateFlow<List<Sample>>(emptyList())
  val samples: StateFlow<List<Sample>> = _samples
  val actions = SampleUiActions(repo, storageService, downloadsFolder, viewModelScope, context)
    private val imageRepo = ImageStorageRepository()
    private val _userAvatar = MutableStateFlow<Any?>(null)
    val userAvatar: StateFlow<Any?> = _userAvatar.asStateFlow()

    private val _currentUserFlow = MutableStateFlow(auth.currentUser)
    private val authListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
            _currentUserFlow.value = firebaseAuth.currentUser
        }

    private var observeUserJob: Job? = null // Proposed by gemini

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    private val _likedSamples = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val likedSamples: StateFlow<Map<Int, Boolean>> = _likedSamples

  // TO DO : Load data from real source
  private fun loadData() {
    _samples.value =
        listOf(
            Sample(
                1,
                "Sample 1",
                "This is a sample description 1",
                21,
                listOf(NATURE_TAG),
                21,
                21,
                21),
            Sample(2, "Sample 2", "This is a sample description 2", 42, listOf("#sea"), 42, 42, 42),
            Sample(3, "Sample 3", "sea", 12, listOf("#relax", NATURE_TAG), 12, 12, 12),
            Sample(
                4, "nature", "This is a sample description 4", 2, listOf("#takeItEasy"), 1, 2, 1),
            Sample(
                5,
                "Sample 5",
                "This is a sample description 5",
                75,
                listOf(NATURE_TAG, "#forest"),
                210,
                210,
                210))
  }
  fun onDownloadSample(sample: Sample) {
    viewModelScope.launch {
        try{
            actions.onDownloadClicked(sample)
        } catch (e: Exception){
            // Handle exception if needed
        }
    }
  }
  fun onLikeClick(sample: Sample, isLiked: Boolean) {
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
            val allSamples = _samples.value

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
            repo.addComment(sampleId, username, text.trim())
        }
    }

  open fun search(query: String) {
    if (useMockData)
        loadData()
    else{
        loadSamplesFromFirebase()
    }
    val normalizedQuery = normalize(query)
    if (normalizedQuery.isEmpty()) {
      return
    }
    _samples.value =
        _samples.value.filter {
          normalize(it.name).contains(normalizedQuery, ignoreCase = true) ||
              normalize(it.description).contains(normalizedQuery, ignoreCase = true) ||
              it.tags.any { tag -> normalize(tag).contains(normalizedQuery, ignoreCase = true) }
        }
  }
  // Normalizes text by converting it to lowercase and removing non-alphanumeric characters.
  fun normalize(text: String): String {
    // FYI .toRegex allows to detect patterns in strings
    return text.lowercase().replace("\\p{M}".toRegex(), "").replace(Regex("[^a-z0-9]"), "").trim()
  }
    fun loadSamplesFromFirebase() {
        viewModelScope.launch{
            val profile = profileRepo.getProfile()
            val following = profile?.following.orEmpty()
            _samples.value = repo.getSamples()
            refreshLikeStates()
        }
    }
}
