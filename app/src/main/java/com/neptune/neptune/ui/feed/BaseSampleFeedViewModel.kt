package com.neptune.neptune.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.sample.Comment
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.main.SampleResourceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared feed logic for listing samples (likes, comments, usernames, resource cache). Subclasses
 * provide feed-specific behaviors (e.g., download/like impl, resource loading).
 */
abstract class BaseSampleFeedViewModel(
    protected val sampleRepo: SampleRepository,
    protected val profileRepo: ProfileRepository,
) : ViewModel(), SampleFeedController {

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
}
