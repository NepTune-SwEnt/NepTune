package com.neptune.neptune.ui.sampler

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class SamplerTab {
  BASICS,
  EQ,
  COMP,
  TEMP
}

private val NOTE_ORDER = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

data class SamplerUiState(
    val isPlaying: Boolean = false,
    val currentTab: SamplerTab = SamplerTab.BASICS,
    val pitch: String = "C",
    val tempo: Int = 110,
    val pitchNote: String = "C",
    val pitchOctave: Int = 4,
    val attack: Float = 0.0f,
    val decay: Float = 0.0f,
    val sustain: Float = 0.0f,
    val release: Float = 0.0f,
    val playbackPosition: Float = 0.0f
) {
  val fullPitch: String
    get() = "$pitchNote$pitchOctave"
}

open class SamplerViewModel : ViewModel() {

  val _uiState = MutableStateFlow(SamplerUiState())
  val uiState: StateFlow<SamplerUiState> = _uiState

  val maxOctave = 7

  val minOctave = 1

  open fun selectTab(tab: SamplerTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  open fun togglePlayPause() {
    _uiState.update { currentState ->
      val newIsPlaying = !currentState.isPlaying
      val newPosition =
          if (newIsPlaying && currentState.playbackPosition >= 1.0f) {
            0.0f
          } else {
            currentState.playbackPosition
          }

      currentState.copy(isPlaying = newIsPlaying, playbackPosition = newPosition)
    }
  }

  open fun updateAttack(value: Float) {
    _uiState.update { it.copy(attack = value) }
  }

  open fun updateDecay(value: Float) {
    _uiState.update { it.copy(decay = value) }
  }

  open fun updateSustain(value: Float) {
    _uiState.update { it.copy(sustain = value) }
  }

  open fun updateRelease(value: Float) {
    _uiState.update { it.copy(release = value) }
  }

  open fun updatePitch(newPitch: String) {
    _uiState.update { it.copy(pitch = newPitch) }
  }

  open fun updateTempo(newTempo: Int) {
    _uiState.update { it.copy(tempo = newTempo) }
  }

  open fun saveSampler() {}

  open fun increasePitch() {
    _uiState.update { currentState ->
      val currentIndex = NOTE_ORDER.indexOf(currentState.pitchNote)
      var newIndex = currentIndex + 1
      var newOctave = currentState.pitchOctave

      if (currentState.pitchNote == "B" && currentState.pitchOctave == maxOctave) {
        return@update currentState
      }

      if (newIndex >= NOTE_ORDER.size) {
        newIndex = 0
        newOctave++
      }
      currentState.copy(pitchNote = NOTE_ORDER[newIndex], pitchOctave = newOctave)
    }
  }

  open fun decreasePitch() {
    _uiState.update { currentState ->
      val currentIndex = NOTE_ORDER.indexOf(currentState.pitchNote)
      var newIndex = currentIndex - 1
      var newOctave = currentState.pitchOctave

      if (currentState.pitchNote == "C" && currentState.pitchOctave == minOctave) {
        return@update currentState
      }

      if (newIndex < 0) {
        newIndex = NOTE_ORDER.size - 1
        newOctave--
      }

      currentState.copy(pitchNote = NOTE_ORDER[newIndex], pitchOctave = newOctave)
    }
  }

  open fun updatePlaybackPosition(position: Float) {
    _uiState.update {
      if (position >= 1.0f && it.isPlaying) {
        it.copy(playbackPosition = 0.0f, isPlaying = false)
      } else {
        it.copy(playbackPosition = position.coerceIn(0f, 1f))
      }
    }
  }
}
