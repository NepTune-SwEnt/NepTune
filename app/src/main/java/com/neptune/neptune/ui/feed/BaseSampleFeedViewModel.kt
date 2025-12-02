package com.neptune.neptune.ui.feed

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.main.SampleResourceState
import com.neptune.neptune.ui.main.SampleUiActions
import com.neptune.neptune.util.WaveformExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared feed logic for listing samples (likes, comments, usernames, resource cache). Subclasses
 * provide feed-specific behaviors (download/like impl, resource loading).
 */
abstract class BaseSampleFeedViewModel(
    protected val sampleRepo: SampleRepository,
    protected val profileRepo: ProfileRepository,
    protected val auth: FirebaseAuth? = null,
  protected val context: Context
) : ViewModel(), SampleFeedController {
  protected open val actions: SampleUiActions? = null
  protected val defaultName = "anonymous"
  protected val _comments = MutableStateFlow<List<Comment>>(emptyList())
  val comments: StateFlow<List<Comment>> = _comments.asStateFlow()
  protected val _activeCommentSampleId = MutableStateFlow<String?>(null)
  val activeCommentSampleId: StateFlow<String?> = _activeCommentSampleId.asStateFlow()
  protected val _usernames = MutableStateFlow<Map<String, String>>(emptyMap())
  override val usernames: StateFlow<Map<String, String>> = _usernames.asStateFlow()
  protected val _sampleResources = MutableStateFlow<Map<String, SampleResourceState>>(emptyMap())
  override val sampleResources: StateFlow<Map<String, SampleResourceState>> =
      _sampleResources.asStateFlow()
  private val avatarCache = mutableMapOf<String, String?>()
  private val userNameCache = mutableMapOf<String, String>()
  private val coverImageCache = mutableMapOf<String, String?>()
  private val audioUrlCache = mutableMapOf<String, String?>()
  private val waveformCache = mutableMapOf<String, List<Float>>()

  override fun onCommentClicked(sample: Sample) {
    observeCommentsForSample(sample.id)
    _activeCommentSampleId.value = sample.id
  }

  override fun resetCommentSampleId() {
    _activeCommentSampleId.value = null
  }

  override fun onAddComment(sampleId: String, text: String) {
    viewModelScope.launch {
      val profile = profileRepo.getCurrentProfile()
      val authorId = profile?.uid ?: "unknown"
      val authorName = profile?.username ?: defaultName
      sampleRepo.addComment(sampleId, authorId, authorName, text.trim())
      observeCommentsForSample(sampleId)
    }
  }

  protected open fun observeCommentsForSample(sampleId: String) {
    viewModelScope.launch {
      sampleRepo.observeComments(sampleId).collectLatest { list ->
        list.forEach { comment -> loadUsername(comment.authorId) }
        _comments.value = list
      }
    }
  }

  protected open fun loadUsername(userId: String) {
    viewModelScope.launch {
      val cached = _usernames.value[userId]
      if (cached != null && cached != defaultName) return@launch

      val userName = profileRepo.getUserNameByUserId(userId) ?: defaultName
      _usernames.update { it + (userId to userName) }
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

  /** True when [ownerId] refers to the currently signed-in Firebase user. */
  override fun isCurrentUser(ownerId: String): Boolean {
    val currentUserId = auth?.currentUser?.uid ?: return false
    return ownerId.isNotBlank() && ownerId == currentUserId
  }

  // ===================== HELPERS ========================
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
      Log.e("BaseSampleFeedViewModel", "Waveform error: ${e.message}")
      emptyList()
    }
  }

  // ================= TEST HELPERS ===================

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun onAddCommentPublic(sampleId: String, text: String) {
    onAddComment(sampleId, text)
  }
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun observeCommentsForSamplePublic(sampleId: String) {
    observeCommentsForSample(sampleId)
  }
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun loadUsernamePublic(userId: String) {
    loadUsername(userId)
  }
}
