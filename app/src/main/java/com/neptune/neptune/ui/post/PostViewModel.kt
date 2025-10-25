package com.neptune.neptune.ui.post

import androidx.lifecycle.ViewModel
import com.neptune.neptune.Sample
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

  fun loadSample(sample: Sample) {
    _uiState.update { it.copy(sample = sample) }
  }

  fun updateTitle(title: String) {
    _uiState.update { it.copy(sample = it.sample.copy(name = title)) }
  }

  fun updateDescription(desc: String) {
    _uiState.update { it.copy(sample = it.sample.copy(description = desc)) }
  }

  fun updateTags(tags: String) {
    val tagList = tags.split(" ").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
    _uiState.update { it.copy(sample = it.sample.copy(tags = tagList)) }
  }

  fun submitPost() {
    val sample = _uiState.value.sample
    // TODO: Post logic
  }
}

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
