package com.neptune.neptune.ui.sampler

import androidx.lifecycle.ViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class SamplerTab {
  BASICS,
  EQ,
  COMP,
}

private val NOTE_ORDER = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private val EQ_FREQUENCIES = listOf(60, 120, 250, 500, 1000, 2500, 5000, 10000)
const val EQ_GAIN_DEFAULT = 0.0f
private const val EQ_GAIN_MIN = -20.0f
private const val EQ_GAIN_MAX = 20.0f

const val COMP_GAIN_MIN = -20.0f
const val COMP_GAIN_MAX = 20.0f
const val COMP_TIME_MAX = 1.0f
const val COMP_THRESHOLD_DEFAULT = -10.0f
const val COMP_KNEE_MAX = 20.0f

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
    val playbackPosition: Float = 0.0f,
    val eqBands: List<Float> = List(EQ_FREQUENCIES.size) { EQ_GAIN_DEFAULT },
    val reverbWet: Float = 0.25f,
    val reverbSize: Float = 3.0f,
    val reverbWidth: Float = 1.0f,
    val reverbDepth: Float = 0.5f,
    val reverbPredelay: Float = 10.0f,
    val compThreshold: Float = COMP_THRESHOLD_DEFAULT,
    val compRatio: Int = 4,
    val compKnee: Float = 0.0f,
    val compGain: Float = 0.0f,
    val compAttack: Float = 0.010f,
    val compDecay: Float = 0.100f
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

  open fun updateEqBand(index: Int, newGain: Float) {
    _uiState.update { currentState ->
      val newBands = currentState.eqBands.toMutableList()
      if (index in newBands.indices) {
        newBands[index] = newGain.coerceIn(EQ_GAIN_MIN, EQ_GAIN_MAX)
      }
      currentState.copy(eqBands = newBands)
    }
  }

  open fun updateReverbWet(value: Float) {
    _uiState.update { it.copy(reverbWet = value.coerceIn(0.0f, 1.0f)) }
  }

  open fun updateReverbSize(value: Float) {
    _uiState.update { it.copy(reverbSize = value.coerceIn(0.1f, 10.0f)) }
  }

  open fun updateReverbWidth(value: Float) {
    _uiState.update { it.copy(reverbWidth = value.coerceIn(0.0f, 1.0f)) }
  }

  open fun updateReverbDepth(value: Float) {
    _uiState.update { it.copy(reverbDepth = value.coerceIn(0.0f, 1.0f)) }
  }

  open fun updateReverbPredelay(value: Float) {
    _uiState.update { it.copy(reverbPredelay = value.coerceIn(0.0f, 100.0f)) }
  }

  open fun updateCompThreshold(value: Float) {
    _uiState.update { it.copy(compThreshold = value.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)) }
  }

  open fun updateCompRatio(value: Float) {
    _uiState.update { it.copy(compRatio = value.roundToInt().coerceIn(1, 20)) }
  }

  open fun updateCompKnee(value: Float) {
    _uiState.update { it.copy(compKnee = value.coerceIn(0.0f, COMP_KNEE_MAX)) }
  }

  open fun updateCompGain(value: Float) {
    _uiState.update { it.copy(compGain = value.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)) }
  }

  open fun updateCompAttack(value: Float) {
    _uiState.update { it.copy(compAttack = value.coerceIn(0.0f, COMP_TIME_MAX)) }
  }

  open fun updateCompDecay(value: Float) {
    _uiState.update { it.copy(compDecay = value.coerceIn(0.0f, COMP_TIME_MAX)) }
  }
}
