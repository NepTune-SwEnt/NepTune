package com.neptune.neptune.ui.post

import androidx.lifecycle.ViewModel
import com.neptune.neptune.model.sample.Sample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing the state and operations related to the post screen. This has been written
 * with the help of LLMs.
 *
 * @author Angéline Bignens
 */
class PostViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(PostUiState())
  val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

  /**
   * Loads a sample
   *
   * @param sample The sample to load in the post screen
   */
  fun loadSample(sample: Sample) {
    _uiState.update { it.copy(sample = sample) }
  }

  /**
   * Updates the title of the currently loaded sample
   *
   * @param title The new title to set
   */
  fun updateTitle(title: String) {
    _uiState.update { it.copy(sample = it.sample.copy(name = title)) }
  }

  /**
   * Updates the description of the currently loaded sample
   *
   * @param desc The new description to set
   */
  fun updateDescription(desc: String) {
    _uiState.update { it.copy(sample = it.sample.copy(description = desc)) }
  }

  /**
   * Updates the tags of the currently loaded sample
   *
   * @param tags The new tags to set
   */
  fun updateTags(tags: String) {
    val tagList = tags.split(" ").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
    _uiState.update { it.copy(sample = it.sample.copy(tags = tagList)) }
  }

  /** Submits the post */
  fun submitPost() {
    // TODO: Post logic
  }
}

/**
 * Represents the UI state of the Post screen
 *
 * @property sample The sample being posted
 * @property audience The selected audience for the post
 */
data class PostUiState(
    val sample: Sample =
        Sample(
            id = 0,
            name = "",
            description = "",
            durationSeconds = 0,
            tags = emptyList(),
            likes = 0,
            comments = 0,
            downloads = 0,
            uriString = ""),
    val audience: String = "Followers"
)
