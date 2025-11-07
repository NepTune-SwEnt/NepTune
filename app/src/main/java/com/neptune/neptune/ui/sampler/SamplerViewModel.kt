package com.neptune.neptune.ui.sampler

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.SamplerProjectMetadata
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.neptune.neptune.media.NeptuneMediaPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

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
    val compDecay: Float = 0.100f,
    val currentAudioUri: Uri? = null,
    val audioDurationMillis: Int = 4000
) {
  val fullPitch: String
    get() = "$pitchNote$pitchOctave"
}

open class SamplerViewModel(application: Application) : AndroidViewModel(application){

    private val context: Context = application.applicationContext


    private val mediaPlayer = NeptuneMediaPlayer(context)

    init {
        mediaPlayer.setOnCompletionListener {
            viewModelScope.launch {
                _uiState.update { currentState ->
                    currentState.copy(
                        isPlaying = false,
                        playbackPosition = 0.0f
                    )
                }
            }
        }
    }

    private var playbackTickerJob: Job? = null

  val _uiState = MutableStateFlow(SamplerUiState())
  val uiState: StateFlow<SamplerUiState> = _uiState

  val maxOctave = 7

  val minOctave = 1

  val extractor = ProjectExtractor()

  open fun selectTab(tab: SamplerTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

    private fun updatePlaybackPosition() {
        val positionMillis = mediaPlayer.getCurrentPosition()
        val durationMillis = _uiState.value.audioDurationMillis

        val newPosition = if (durationMillis > 0) {
            positionMillis.toFloat() / durationMillis.toFloat()
        } else {
            0.0f
        }

        _uiState.update { it.copy(playbackPosition = newPosition) }
    }

    private fun startPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = viewModelScope.launch {
            while (mediaPlayer.isPlaying()) {
                updatePlaybackPosition()
                delay(100L)
            }
            updatePlaybackPosition()
        }
    }

    private fun stopPlaybackTicker() {
        playbackTickerJob?.cancel()
        playbackTickerJob = null
    }

    open fun togglePlayPause() {
        val currentUri = _uiState.value.currentAudioUri

        val shouldReset = _uiState.value.playbackPosition >= 0.99f
        val isCurrentlyPlaying = mediaPlayer.isPlaying()

        if (currentUri != null) {
            if (isCurrentlyPlaying) {
                mediaPlayer.togglePlay(currentUri)
            } else {
                val currentUIPositionNorm = _uiState.value.playbackPosition
                val durationMillis = _uiState.value.audioDurationMillis
                val seekPositionMillis = (currentUIPositionNorm * durationMillis).roundToInt()
                if (shouldReset) {
                    mediaPlayer.goTo(0)
                } else if (seekPositionMillis > 0) {
                    mediaPlayer.goTo(seekPositionMillis)
                }
                mediaPlayer.togglePlay(currentUri)
            }
        }

        _uiState.update { currentState ->
            val newIsPlaying = mediaPlayer.isPlaying()
            val realDuration = mediaPlayer.getDuration()
            val newDuration = if (realDuration > 0) realDuration else currentState.audioDurationMillis
            val newPosition = if (newIsPlaying && shouldReset) 0.0f else currentState.playbackPosition
            if (newIsPlaying) startPlaybackTicker() else stopPlaybackTicker()
            currentState.copy(isPlaying = newIsPlaying, playbackPosition = newPosition, audioDurationMillis = newDuration)
        }

    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.stop()
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

  fun loadProjectData(zipFilePath: String, context: Context) {
    viewModelScope.launch {
      try {
          val zipFile = File(zipFilePath)
          val metadata: SamplerProjectMetadata = extractor.extractMetadata(zipFile)
          val audioFileName = metadata.audioFiles.firstOrNull()?.name
          val audioUri = if (audioFileName != null) {
              extractor.extractAudioFile(zipFile, context, audioFileName)
          } else {
              null
          }
          val duration = mediaPlayer.getDuration()
          Log.d("SamplerViewModel", "Audio URI chargée: $audioUri")
          val paramMap = metadata.parameters.associate { it.type to it.value }
          _uiState.update { current ->
              val newEqBands = current.eqBands.toMutableList()
              EQ_FREQUENCIES.forEachIndexed { index, _ ->
                paramMap["eq_band_$index"]?.let { gain ->
                  newEqBands[index] = gain.coerceIn(EQ_GAIN_MIN, EQ_GAIN_MAX)
                }
              }

              current.copy(
                  attack = paramMap["attack"]?.coerceIn(0f, ADSR_MAX_TIME) ?: current.attack,
                  decay = paramMap["decay"]?.coerceIn(0f, ADSR_MAX_TIME) ?: current.decay,
                  sustain = paramMap["sustain"]?.coerceIn(0f, ADSR_MAX_SUSTAIN) ?: current.sustain,
                  release = paramMap["release"]?.coerceIn(0f, ADSR_MAX_TIME) ?: current.release,
                  reverbWet = paramMap["reverbWet"]?.coerceIn(0f, 1f) ?: current.reverbWet,
                  reverbSize =
                      paramMap["reverbSize"]?.coerceIn(0.1f, REVERB_SIZE_MAX) ?: current.reverbSize,
                  reverbWidth = paramMap["reverbWidth"]?.coerceIn(0f, 1f) ?: current.reverbWidth,
                  reverbDepth = paramMap["reverbDepth"]?.coerceIn(0f, 1f) ?: current.reverbDepth,
                  reverbPredelay =
                      paramMap["reverbPredelay"]?.coerceIn(0f, PREDELAY_MAX_MS)
                          ?: current.reverbPredelay,
                  compThreshold =
                      paramMap["compThreshold"]?.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)
                          ?: current.compThreshold,
                  compRatio =
                      paramMap["compRatio"]?.let { ratioFloat ->
                        ratioFloat.roundToInt().coerceIn(1, 20)
                      } ?: current.compRatio,
                  compKnee = paramMap["compKnee"]?.coerceIn(0f, COMP_KNEE_MAX) ?: current.compKnee,
                  compGain =
                      paramMap["compGain"]?.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX) ?: current.compGain,
                  compAttack =
                      paramMap["compAttack"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compAttack,
                  compDecay = paramMap["compDecay"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compDecay,
                  eqBands = newEqBands.toList(),
                  currentAudioUri = audioUri,
                  audioDurationMillis = if (duration > 0) duration else 4000
              )
        }
      } catch (e: Exception) {
        Log.e("SamplerViewModel", "Échec du chargement du projet ZIP: ${e.message}", e)
      }
    }
  }
}
