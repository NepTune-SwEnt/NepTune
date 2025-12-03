package com.neptune.neptune.ui.sampler

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.PitchShifter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.io.android.AndroidAudioInputStream
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.project.AudioFileMetadata
import com.neptune.neptune.model.project.ParameterMetadata
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.ProjectWriter
import com.neptune.neptune.model.project.SamplerProjectData
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlin.math.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

private val DEFAULT_COMB_DELAYS = listOf(1116, 1188, 1277, 1356)

private const val ALLPASS_DELAY_BASE_1 = 556
private const val ALLPASS_DELAY_BASE_2 = 441

private const val DEFAULT_AUDIO_BASENAME = "default_audio"

const val DEFAULT_SAMPLE_TIME = 4000

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
    val reverbWet: Float = 0.0f,
    val reverbSize: Float = 0.0f,
    val reverbWidth: Float = 0.0f,
    val reverbDepth: Float = 0.0f,
    val reverbPredelay: Float = 10.0f,
    val compThreshold: Float = COMP_THRESHOLD_DEFAULT,
    val compRatio: Int = 4,
    val compKnee: Float = 0.0f,
    val compGain: Float = 0.0f,
    val compAttack: Float = 0.010f,
    val compDecay: Float = 0.100f,
    val originalAudioUri: Uri? = null,
    val currentAudioUri: Uri? = null,
    val audioDurationMillis: Int = 4000,
    val showInitialSetupDialog: Boolean = false,
    val inputTempo: Int = 120,
    val inputPitchNote: String = "C",
    val inputPitchOctave: Int = 4,
    val timeSignature: String = "4/4",
    val previewPlaying: Boolean = false,
    val projectLoadError: String? = null,
    val waveform: List<Float> = emptyList()
) {
  val fullPitch: String
    get() = "$pitchNote$pitchOctave"
}

open class SamplerViewModel() : ViewModel() {

  val context: Context = NepTuneApplication.appContext

  open val mediaPlayer = NeptuneMediaPlayer()

  init {
    mediaPlayer.setOnCompletionListener {
      viewModelScope.launch {
        _uiState.update { currentState ->
          currentState.copy(isPlaying = false, playbackPosition = 0.0f)
        }
      }
    }
  }

  internal interface DispatcherProvider {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
  }

  internal object DefaultDispatcherProvider : DispatcherProvider {
    override val default = Dispatchers.Default
    override val io = Dispatchers.IO
    override val main = Dispatchers.Main
  }

  internal var dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  private var playbackTickerJob: Job? = null

  val _uiState = MutableStateFlow(SamplerUiState())
  val uiState: StateFlow<SamplerUiState> = _uiState

  val maxOctave = 7

  val minOctave = 1

  val extractor = ProjectExtractor()

  open fun selectTab(tab: SamplerTab) {
    _uiState.update { it.copy(currentTab = tab) }
  }

  fun updatePlaybackPosition() {
    val positionMillis = mediaPlayer.getCurrentPosition()
    val durationMillis = _uiState.value.audioDurationMillis

    val newPosition =
        if (durationMillis > 0) {
          if (positionMillis >= durationMillis) {
            0.0f
          } else {
            positionMillis.toFloat() / durationMillis.toFloat()
          }
        } else {
          0.0f
        }

    _uiState.update { it.copy(playbackPosition = newPosition) }
  }

  private fun startPlaybackTicker() {
    playbackTickerJob?.cancel()
    playbackTickerJob =
        viewModelScope.launch {
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
    if (currentUri == null) return

    val currentState = _uiState.value
    val wasPlayingBefore = mediaPlayer.isPlaying()

    val shouldResetFromEnd = currentState.playbackPosition >= 0.99f
    val isNearZero = currentState.playbackPosition < 0.01f
    val isFirstPlay = mediaPlayer.getCurrentUri() != currentUri

    val durationMillis = currentState.audioDurationMillis
    val currentUIPositionNorm = currentState.playbackPosition
    val seekPositionMillis = (currentUIPositionNorm * durationMillis).roundToInt()

    mediaPlayer.togglePlay(currentUri)

    if (wasPlayingBefore) {

      mediaPlayer.pause()
      stopPlaybackTicker()
    } else {
      val targetSeekPosition = if (shouldResetFromEnd || isNearZero) 0 else seekPositionMillis

      if (isFirstPlay) {
        mediaPlayer.setOnPreparedListener {
          val duration = mediaPlayer.getDuration()
          _uiState.update { state ->
            state.copy(
                isPlaying = true,
                playbackPosition = 0f,
                audioDurationMillis = if (duration > 0) duration else state.audioDurationMillis)
          }
          startPlaybackTicker()
        }

        mediaPlayer.play(currentUri)
      } else {
        mediaPlayer.goTo(targetSeekPosition)
        mediaPlayer.resume()
        startPlaybackTicker()
      }

      startPlaybackTicker()
    }

    _uiState.update { state ->
      val isStartingPlay = !wasPlayingBefore && !mediaPlayer.isPlaying()

      val newIsPlaying = if (isStartingPlay) true else mediaPlayer.isPlaying()

      val realDuration = mediaPlayer.getDuration()
      val newDuration = if (realDuration > 0) realDuration else state.audioDurationMillis
      val didReset = shouldResetFromEnd || isNearZero
      val newPosition = if (newIsPlaying && didReset) 0.0f else state.playbackPosition

      state.copy(
          isPlaying = newIsPlaying,
          playbackPosition = newPosition,
          audioDurationMillis = newDuration)
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

  fun updateInputPitch(note: String, octave: Int) {
    _uiState.update {
      it.copy(inputPitchNote = note, inputPitchOctave = octave.coerceIn(minOctave, maxOctave))
    }
  }

  open fun updateTempo(newTempo: Int) {
    _uiState.update { it.copy(tempo = newTempo) }
  }

  // New: update selected time signature
  open fun updateTimeSignature(newSignature: String) {
    _uiState.update { it.copy(timeSignature = newSignature) }
  }

  fun updateInputTempo(value: Int?) {
    _uiState.update { current -> current.copy(inputTempo = value ?: 0) }
  }

  private var previewPlayer: MediaPlayer? = null

  private val tapTimes = mutableListOf<Long>()

  fun playPreview(context: Context) {
    stopPreview()

    val previewUri = uiState.value.currentAudioUri ?: return

    previewPlayer =
        MediaPlayer().apply {
          setDataSource(context, previewUri)
          prepare()
          start()
          setOnCompletionListener { stopPreview() }
        }

    _uiState.update { it.copy(previewPlaying = true) }
  }

  fun stopPreview() {
    previewPlayer?.stop()
    previewPlayer?.release()
    previewPlayer = null

    _uiState.update { it.copy(previewPlaying = false) }
  }

  fun tapTempo() {
    val now = System.currentTimeMillis()

    tapTimes.add(now)
    if (tapTimes.size > 6) tapTimes.removeAt(0)

    if (tapTimes.size >= 2) {
      val diffs = tapTimes.zipWithNext { a, b -> b - a }

      val avg = diffs.average()
      val bpm = (60000.0 / avg).roundToInt()

      _uiState.update { it.copy(inputTempo = bpm) }
    }
  }

  fun confirmInitialSetup() {
    val currentState = _uiState.value
    _uiState.update {
      it.copy(
          tempo = currentState.inputTempo,
          pitchNote = currentState.inputPitchNote,
          pitchOctave = currentState.inputPitchOctave,
          showInitialSetupDialog = false)
    }
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

  fun increaseInputPitch() {
    _uiState.update { currentState ->
      val currentIndex = NOTE_ORDER.indexOf(currentState.inputPitchNote)
      var newIndex = currentIndex + 1
      var newOctave = currentState.inputPitchOctave

      if (currentState.inputPitchNote == "B" && currentState.inputPitchOctave == maxOctave) {
        return@update currentState
      }

      if (newIndex >= NOTE_ORDER.size) {
        newIndex = 0
        newOctave++
      }

      currentState.copy(inputPitchNote = NOTE_ORDER[newIndex], inputPitchOctave = newOctave)
    }
  }

  fun decreaseInputPitch() {
    _uiState.update { currentState ->
      val currentIndex = NOTE_ORDER.indexOf(currentState.inputPitchNote)
      var newIndex = currentIndex - 1
      var newOctave = currentState.inputPitchOctave

      if (currentState.inputPitchNote == "C" && currentState.inputPitchOctave == minOctave) {
        return@update currentState
      }

      if (newIndex < 0) {
        newIndex = NOTE_ORDER.size - 1
        newOctave--
      }

      currentState.copy(inputPitchNote = NOTE_ORDER[newIndex], inputPitchOctave = newOctave)
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

  fun loadWaveform(uri: Uri) {
    viewModelScope.launch {
      val wf = extractWaveformInternal(uri)
      _uiState.update { it.copy(waveform = wf) }
    }
  }

  private suspend fun extractWaveformInternal(uri: Uri): List<Float> {
    return try {
      WaveformExtractor().extractWaveform(context, uri, samplesCount = 100)
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error extracting waveform", e)
      emptyList()
    }
  }

  open fun loadProjectData(zipFilePath: String) {
    viewModelScope.launch {
      try {
        val cleanPath = zipFilePath.removePrefix("file:").removePrefix("file://")
        val zipFile = File(cleanPath)
        if (!zipFile.exists()) {
          Log.e("SamplerViewModel", "ZIP not found: $cleanPath")
          _uiState.update {
            it.copy(
                showInitialSetupDialog = false,
                currentAudioUri = null,
                projectLoadError = "ZIP not found")
          }
          return@launch
        }

        val projectData: SamplerProjectData =
            try {
              extractor.extractMetadata(zipFile)
            } catch (e: Exception) {
              Log.e("SamplerViewModel", "Metadata read error: ${e.message}", e)
              _uiState.update {
                it.copy(
                    showInitialSetupDialog = false,
                    currentAudioUri = null,
                    projectLoadError = "Can't read config.json")
              }
              return@launch
            }
        val audioFileName = projectData.audioFiles.firstOrNull()?.name
        if (audioFileName == null) {
          _uiState.update {
            it.copy(
                currentAudioUri = null,
                showInitialSetupDialog = false,
                projectLoadError = "No audio file in project")
          }
          return@launch
        }

        val audioUri =
            try {
              extractor.extractAudioFile(zipFile, context, audioFileName)
            } catch (e: Exception) {
              Log.e("SamplerViewModel", "Audio extraction error: ${e.message}", e)
              _uiState.update {
                it.copy(
                    currentAudioUri = null,
                    showInitialSetupDialog = false,
                    projectLoadError = "Audio file extraction impossible")
              }
              return@launch
            }

        val sampleDuration = mediaPlayer.getDuration()
        Log.d("SamplerViewModel", "URI audio loaded: $audioUri")

        val paramMap = projectData.parameters.associate { it.type to it.value }

        val tempoValue = paramMap["tempo"]
        val pitchValue = paramMap["pitch"]

        val tempoFound = tempoValue != null
        val pitchFound = pitchValue != null
        val needsSetup = !tempoFound || !pitchFound

        if (needsSetup) {
          _uiState.update { current ->
            val newEqBands = current.eqBands.toMutableList()
            EQ_FREQUENCIES.forEachIndexed { index, _ ->
              paramMap["eq_band_$index"]?.let { gain ->
                newEqBands[index] = gain.coerceIn(EQ_GAIN_MIN, EQ_GAIN_MAX)
              }
            }
            current.copy(
                showInitialSetupDialog = true,
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
                    paramMap["compRatio"]?.roundToInt()?.coerceIn(1, 20) ?: current.compRatio,
                compKnee = paramMap["compKnee"]?.coerceIn(0f, COMP_KNEE_MAX) ?: current.compKnee,
                compGain =
                    paramMap["compGain"]?.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)
                        ?: current.compGain,
                compAttack =
                    paramMap["compAttack"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compAttack,
                compDecay = paramMap["compDecay"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compDecay,
                eqBands = newEqBands.toList(),
                inputTempo = current.tempo,
                inputPitchNote = current.pitchNote,
                inputPitchOctave = current.pitchOctave,
                originalAudioUri = audioUri,
                currentAudioUri = audioUri,
                audioDurationMillis =
                    if (sampleDuration > 0) sampleDuration else DEFAULT_SAMPLE_TIME,
                projectLoadError = null)
          }
        } else {
          _uiState.update { current ->
            val newEqBands = current.eqBands.toMutableList()
            EQ_FREQUENCIES.forEachIndexed { index, _ ->
              paramMap["eq_band_$index"]?.let { gain ->
                newEqBands[index] = gain.coerceIn(EQ_GAIN_MIN, EQ_GAIN_MAX)
              }
            }

            val loadedPitchNote = NOTE_ORDER[pitchValue.roundToInt() % NOTE_ORDER.size]
            val loadedPitchOctave = 4

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
                    paramMap["compRatio"]?.roundToInt()?.coerceIn(1, 20) ?: current.compRatio,
                compKnee = paramMap["compKnee"]?.coerceIn(0f, COMP_KNEE_MAX) ?: current.compKnee,
                compGain =
                    paramMap["compGain"]?.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)
                        ?: current.compGain,
                compAttack =
                    paramMap["compAttack"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compAttack,
                compDecay = paramMap["compDecay"]?.coerceIn(0f, COMP_TIME_MAX) ?: current.compDecay,
                eqBands = newEqBands.toList(),
                tempo = tempoValue.roundToInt().coerceIn(50, 200),
                pitchNote = loadedPitchNote,
                pitchOctave = loadedPitchOctave,
                originalAudioUri = audioUri,
                currentAudioUri = audioUri,
                audioDurationMillis =
                    if (sampleDuration > 0) sampleDuration else DEFAULT_SAMPLE_TIME,
                projectLoadError = null)
          }
        }
        audioBuilding()
      } catch (e: Exception) {
        Log.e("SamplerViewModel", "ZIP project loading has failed: ${e.message}", e)

        _uiState.update { it.copy(showInitialSetupDialog = false, currentAudioUri = null) }
      }
    }
  }

  open fun saveProjectData(zipFilePath: String): Job {
    return viewModelScope.launch {
      val newAudioUri: Uri? = audioBuilding()

      if (newAudioUri != null) {
        _uiState.update { it.copy(currentAudioUri = newAudioUri) }
      }

      saveProjectDataSync(zipFilePath)
      audioBuilding()
    }
  }

  fun saveProjectDataSync(zipFilePath: String) {
    try {
      val state = _uiState.value

      val audioUriWithEffects = state.currentAudioUri

      if (audioUriWithEffects == null) {
        Log.e("SamplerViewModel", "No audio saved, action canceled.")
        return
      }

      val path = audioUriWithEffects.path
      if (path.isNullOrEmpty()) {
        Log.e("SamplerViewModel", "Invalid audio path: $path")
        return
      }

      val audioFile = File(path)
      if (!audioFile.exists()) {
        Log.e("SamplerViewModel", "The audio file doesn't exists: ${audioFile.path}")
        return
      }

      val eqParameters =
          state.eqBands.mapIndexed { index, gain ->
            ParameterMetadata(type = "eq_band_$index", value = gain, targetAudioFile = "global")
          }

      val parametersList =
          mutableListOf<ParameterMetadata>(
              ParameterMetadata("attack", state.attack, audioFile.name),
              ParameterMetadata("decay", state.decay, audioFile.name),
              ParameterMetadata("sustain", state.sustain, audioFile.name),
              ParameterMetadata("release", state.release, audioFile.name),
              ParameterMetadata("compRatio", state.compRatio.toFloat(), audioFile.name),
              ParameterMetadata("reverbWet", state.reverbWet, audioFile.name),
              ParameterMetadata("reverbSize", state.reverbSize, audioFile.name),
              ParameterMetadata("reverbDepth", state.reverbDepth, audioFile.name),
              ParameterMetadata("reverbWidth", state.reverbWidth, audioFile.name),
              ParameterMetadata("compGain", state.compGain, audioFile.name),
              ParameterMetadata("compThreshold", state.compThreshold, audioFile.name),
              ParameterMetadata("tempo", state.tempo.toFloat(), "global"),
              ParameterMetadata(
                  "pitch",
                  NOTE_ORDER.indexOf(state.pitchNote).toFloat() +
                      (state.pitchOctave * NOTE_ORDER.size),
                  "global"),
          )
      parametersList.addAll(eqParameters)

      val projectData =
          SamplerProjectData(
              audioFiles =
                  listOf(
                      AudioFileMetadata(
                          name = audioFile.name,
                          volume = 1.0f,
                          durationSeconds = state.audioDurationMillis / 1000f)),
              parameters = parametersList)

      val zipFile = File(zipFilePath)
      ProjectWriter()
          .writeProject(zipFile = zipFile, metadata = projectData, audioFiles = listOf(audioFile))

      Log.i("SamplerViewModel", "Project saved: ${zipFile.absolutePath}")
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Failed to save ZIP file: ${e.message}", e)
    }
  }

  suspend fun audioBuilding(): Uri? {
    val state = _uiState.value
    val originalUri =
        state.originalAudioUri ?: return null // Guards against missing original file URI

    // CompletableDeferred acts as a Promise/Future to hold the Uri result from the coroutine
    val deferred = CompletableDeferred<Uri?>()

    // Define a Coroutine Context for background work (Dispatchers.Default)
    // and includes an exception handler to complete the Deferred value if the process crashes.
    val context =
        Dispatchers.Default +
            CoroutineExceptionHandler { _, e ->
              Log.e("SamplerViewModel", "audioBuilding failed: ${e.message}", e)
              deferred.complete(null) // Complete the Deferred with null on failure
            }

    val semitones =
        computeSemitoneShift(
            state.inputPitchNote, state.inputPitchOctave, state.pitchNote, state.pitchOctave)

    // Launch the main processing coroutine
    viewModelScope.launch(context) {
      try {
        // Execute the synchronous DSP pipeline on a background thread (Dispatchers.Default)
        val newUri =
            withContext(dispatcherProvider.default) {
              processAudio(
                  currentAudioUri = originalUri, // Source is the original file (non-destructive)
                  eqBands = state.eqBands,
                  reverbWet = state.reverbWet,
                  reverbSize = state.reverbSize,
                  reverbWidth = state.reverbWidth,
                  reverbDepth = state.reverbDepth,
                  reverbPredelay = state.reverbPredelay,
                  semitones = semitones)
            }

        // If processing succeeds, update the UI state with the new processed audio URI
        if (newUri != null) {
          _uiState.update { it.copy(currentAudioUri = newUri) }
        }
        deferred.complete(newUri) // Signal completion to the outer runBlocking caller
      } catch (e: Exception) {
        Log.e("SamplerViewModel", "audioBuilding failed: ${e.message}", e)
        deferred.complete(null)
      }
    }

    // Blocks the caller thread until the processing coroutine completes and deferred has a value.
    return deferred.await()
  }

  private fun processAudio(
      currentAudioUri: Uri?,
      eqBands: List<Float>,
      reverbWet: Float,
      reverbSize: Float,
      reverbWidth: Float,
      reverbDepth: Float,
      reverbPredelay: Float,
      semitones: Int = 0
  ): Uri? {

    if (currentAudioUri == null) return null

    // 1. Decode Audio: Convert source URI (MP3/WAV) to raw PCM samples (FloatArray)
    val audioData = decodeAudioToPCM(currentAudioUri) ?: return null
    var (samples, sampleRate, channelCount) = audioData

    // 2. Apply EQ: Parametric equalization is applied non-destructively to the samples
    samples = applyEQFilters(samples, sampleRate, eqBands)

    // 3. Apply pitch shift
    if (semitones != 0) {
      Log.d(
          "SamplerViewModel",
          "processAudio: semitones=$semitones sampleRate=$sampleRate inSamples=${samples.size} durationSec=${samples.size.toDouble()/sampleRate}")
      samples = pitchShift(samples, semitones)
      Log.d("SamplerViewModel", "after pitchShift: samples=${samples.size}")
    }

    // 3. Apply Reverb: Reverb is applied on top of the EQ'd signal
    samples =
        applyReverb(
            samples, sampleRate, reverbWet, reverbSize, reverbWidth, reverbDepth, reverbPredelay)

    // 4. Encode and Save: Prepare the output file path
    val originalName = currentAudioUri.lastPathSegment ?: DEFAULT_AUDIO_BASENAME
    val base = originalName.substringBeforeLast(".")
    // Output file is saved in the app's cache directory (safe location for temporary/processed
    // files)
    val out = File(context.cacheDir, "${base}_processed.wav")

    Log.d(
        "SamplerViewModel",
        "Writing WAV: frames=${samples.size}, durationSec=${samples.size.toFloat() / sampleRate}")

    // Encode processed FloatArray back into a standard WAV file (PCM16 format)
    encodePCMToWAV(samples, sampleRate, channelCount, out)
    Log.d("SamplerViewModel", "WAV file written: frames=${samples.size / channelCount}")

    return Uri.fromFile(out) // Return the URI of the newly processed file
  }

  fun equalizeAudio(audioUri: Uri?, eqBands: List<Float>) {
    Log.d("SamplerViewModel", "Equalize audio called, eqBands=$eqBands")

    if (audioUri == null) {
      Log.e("SamplerViewModel", "No audio to equalize")
      return
    }

    try {
      // Decode PCM (MediaCodec)
      val audioData = decodeAudioToPCM(audioUri)
      if (audioData == null) {
        Log.e("SamplerViewModel", "Failed to decode audio to PCM")
        return
      }

      val (samples, sampleRate, channelCount) = audioData
      Log.d(
          "SamplerViewModel",
          "Decoded ${samples.size} samples at $sampleRate Hz, $channelCount channel(s)")

      // Apply EQ filters to the samples
      var processedSamples = applyEQFilters(samples, sampleRate, eqBands)

      // Apply Compression
      val state = _uiState.value
      val compressor =
          Compressor(
              sampleRate = sampleRate,
              thresholdDb = state.compThreshold,
              ratio = state.compRatio.toFloat(),
              kneeDb = state.compKnee,
              makeUpDb = state.compGain,
              attackSeconds = state.compAttack,
              releaseSeconds = state.compDecay)
      processedSamples = compressor.process(processedSamples)

      // Determine output file name and path in app cache directory
      val originalName = audioUri.lastPathSegment ?: "sample_audio"
      val dotIndex = originalName.lastIndexOf('.')
      val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
      val outFile = File(context.cacheDir, "${baseName}_equalized.wav")

      // Encode equalized samples back to WAV file
      encodePCMToWAV(processedSamples, sampleRate, channelCount, outFile)

      // Update UI state with the new audio Uri
      val newUri = Uri.fromFile(outFile)
      _uiState.update { current -> current.copy(currentAudioUri = newUri) }

      Log.i("SamplerViewModel", "Equalized audio written to ${outFile.absolutePath}")
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Failed to equalize audio: ${e.message}", e)
    }
  }

  /**
   * Internal function to decode audio files (MP3/WAV) into raw PCM float samples (normalized -1.0
   * to 1.0). Uses Android's MediaCodec and MediaExtractor for low-level decoding.
   */
  internal fun decodeAudioToPCM(uri: Uri): Triple<FloatArray, Int, Int>? {
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(context, uri, null)

      // Find the first audio track
      var trackIndex = -1
      var audioTrackFound = false
      var i = 0
      while (i < extractor.trackCount && !audioTrackFound) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)

        if (mime?.startsWith("audio/") == true) {
          trackIndex = i
          extractor.selectTrack(i)
          audioTrackFound = true
        }
        i++
      }

      if (trackIndex == -1) {
        Log.e("SamplerViewModel", "No audio track found")
        return null
      }

      // Setup MediaCodec decoder
      val format = extractor.getTrackFormat(trackIndex)
      val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val mime = format.getString(MediaFormat.KEY_MIME)!!

      val codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val allSamples = mutableListOf<Float>()
      val bufferInfo = MediaCodec.BufferInfo()
      var isEOS = false // End of Stream flag

      while (!isEOS) {
        // Feed input data to the codec
        val inputIndex = codec.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
          val inputBuffer = codec.getInputBuffer(inputIndex)!!
          val sampleSize = extractor.readSampleData(inputBuffer, 0)
          if (sampleSize < 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            isEOS = true
          } else {
            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
            extractor.advance()
          }
        }

        // Receive output data from the codec (decoded PCM)
        var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputIndex >= 0) {
          val outputBuffer = codec.getOutputBuffer(outputIndex)!!
          val shortBuffer = outputBuffer.asShortBuffer()
          val chunk = ShortArray(shortBuffer.remaining())
          shortBuffer.get(chunk)

          // Convert PCM16 (Short.MAX_VALUE range) to float (-1.0 to 1.0)
          for (sample in chunk) {
            allSamples.add(sample.toFloat() / Short.MAX_VALUE)
          }

          codec.releaseOutputBuffer(outputIndex, false)
          outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
      }

      // Cleanup
      codec.stop()
      codec.release()
      extractor.release()

      return Triple(allSamples.toFloatArray(), sampleRate, channelCount)
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error decoding audio: ${e.message}", e)
      // Ensure extractor is released if error occurs before cleanup block
      extractor.release()
      return null
    }
  }

  /** Applies 8 bands of Biquad Peak Filters (Parametric EQ) to the audio samples. */
  internal fun applyEQFilters(
      samples: FloatArray,
      sampleRate: Int,
      eqBands: List<Float>
  ): FloatArray {
    var processedSamples = samples.copyOf()

    // Apply a parametric EQ filter for each band
    EQ_FREQUENCIES.forEachIndexed { index, frequency ->
      val gainDB = eqBands.getOrElse(index) { EQ_GAIN_DEFAULT }

      // Skip if gain is near zero (optimization: no audible effect)
      if (abs(gainDB) < 0.1f) {
        return@forEachIndexed
      }

      // Create a parametric EQ filter using biquad peak filter
      val filter =
          BiquadPeakFilter(
              frequency.toDouble(),
              sampleRate.toDouble(),
              gainDB.toDouble(),
              1.0 // Q factor (bandwidth)
              )

      // Apply filter to all samples
      processedSamples = filter.process(processedSamples)

      Log.d("SamplerViewModel", "Applied EQ at ${frequency}Hz with gain ${gainDB}dB")
    }

    return processedSamples
  }

  // Biquad peak filter for parametric EQ
  internal class BiquadPeakFilter(
      centerFreq: Double,
      sampleRate: Double,
      gainDB: Double,
      q: Double
  ) {
    // Feedforward coefficients (b0, b1, b2) and feedback coefficients (a0, a1, a2)
    // These define the transfer function of the filter.
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a0: Double
    private val a1: Double
    private val a2: Double

    init {
      // 1. Calculate A (amplitude linear gain from gainDB)
      val a = 10.0.pow(gainDB / 40.0) // A = 10^(gainDB / 40)

      // 2. Calculate Omega (normalized angular frequency in radians)
      val omega = 2.0 * Math.PI * centerFreq / sampleRate
      val sinOmega = sin(omega)
      val cosOmega = cos(omega)

      // 3. Calculate Alpha (related to bandwidth/Q factor and sample rate)
      val alpha = sinOmega / (2.0 * q)

      // Calculate B (feedforward) coefficients
      b0 = 1.0 + alpha * a
      b1 = -2.0 * cosOmega
      b2 = 1.0 - alpha * a

      // Calculate A (feedback) coefficients
      a0 = 1.0 + alpha / a // Note: a0 is often normalized to 1, but calculating here for safety.
      a1 = -2.0 * cosOmega
      a2 = 1.0 - alpha / a
    }

    /**
     * Processes the input array (raw audio samples) using the calculated filter coefficients. This
     * is the standard Direct Form 2 implementation of the Biquad filter.
     * * Difference Equation: y[n] = (b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]) / a0
     */
    fun process(input: FloatArray): FloatArray {
      val output = FloatArray(input.size)

      // Delay variables (past inputs and outputs)
      var x1 = 0.0 // x[n-1] (previous input)
      var x2 = 0.0 // x[n-2] (input two steps ago)
      var y1 = 0.0 // y[n-1] (previous output)
      var y2 = 0.0 // y[n-2] (output two steps ago)

      for (i in input.indices) {
        val x0 = input[i].toDouble() // x[n] (current input)

        // Calculate current output y[n]
        val y0 = (b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0

        output[i] = y0.toFloat()

        // Shift the delay variables for the next iteration (n+1)
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
      }

      return output
    }
  }

  /**
   * Encodes the processed float PCM samples back into a standard WAV file format. This is an
   * implementation of a basic 16-bit PCM WAV writer.
   */
  internal fun encodePCMToWAV(
      samples: FloatArray,
      sampleRate: Int,
      channelCount: Int,
      outputFile: File
  ) {
    try {
      // convert to 16-bit PCM (interleaved samples assumed)
      val pcmData = ByteArray(samples.size * 2)
      for (i in samples.indices) {
        val s = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        pcmData[i * 2] = (s.toInt() and 0xFF).toByte()
        pcmData[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
      }

      outputFile.outputStream().use { out ->
        val bitsPerSample = 16
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = (channelCount * bitsPerSample / 8)
        val dataSize = pcmData.size

        out.write("RIFF".toByteArray())
        out.write(intToBytes(36 + dataSize))
        out.write("WAVE".toByteArray())

        out.write("fmt ".toByteArray())
        out.write(intToBytes(16))
        out.write(shortToBytes(1)) // PCM
        out.write(shortToBytes(channelCount.toShort()))
        out.write(intToBytes(sampleRate))
        out.write(intToBytes(byteRate))
        out.write(shortToBytes(blockAlign.toShort()))
        out.write(shortToBytes(bitsPerSample.toShort()))

        out.write("data".toByteArray())
        out.write(intToBytes(dataSize))
        out.write(pcmData)
      }

      Log.d(
          "SamplerViewModel",
          "WAV file written: ${outputFile.absolutePath} frames=${samples.size / channelCount}")
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error encoding WAV: ${e.message}", e)
      throw e
    }
  }

  internal fun intToBytes(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte())
  }

  internal fun shortToBytes(value: Short): ByteArray {
    return byteArrayOf((value.toInt() and 0xFF).toByte(), ((value.toInt() shr 8) and 0xFF).toByte())
  }

  /**
   * Applies Reverb effects (Comb and Allpass filters) and mixes the wet signal with the dry input.
   */
  open fun applyReverb(
      input: FloatArray,
      sampleRate: Int,
      wet: Float,
      size: Float,
      width: Float,
      depth: Float,
      predelayMs: Float
  ): FloatArray {

    if (wet <= 0.01f) return input // If wet mix is near zero, skip reverb processing

    val predelaySamples = (predelayMs / 1000f * sampleRate).toInt()
    val predelayed = FloatArray(input.size + predelaySamples)

    // 1. Apply predelay (shift the signal, introducing silent start)
    System.arraycopy(input, 0, predelayed, predelaySamples, input.size)

    // 2. Comb filter bank (determines tail decay, uses size & depth)
    val combDelays = DEFAULT_COMB_DELAYS.map { (it * size).toInt().coerceAtLeast(10) }

    var processed = predelayed.copyOf()

    combDelays.forEach { delay ->
      val comb = CombFilter(delay, decay = 0.3 * depth)
      processed = comb.process(processed)
    }

    // 3. Allpass filters (smooth the reverb tail)
    val allpass1 =
        AllpassFilter((ALLPASS_DELAY_BASE_1 * width).toInt().coerceAtLeast(10), gain = 0.7)
    val allpass2 =
        AllpassFilter((ALLPASS_DELAY_BASE_2 * width).toInt().coerceAtLeast(10), gain = 0.7)

    processed = allpass1.process(processed)
    processed = allpass2.process(processed)

    // 4. Mix wet/dry signals
    val output = FloatArray(input.size)

    for (i in output.indices) {
      val wetValue = if (i < processed.size) processed[i] else 0f
      val dryValue = input[i]
      output[i] = dryValue * (1f - wet) + wetValue * wet
    }

    return output
  }

  internal class CombFilter(private val delaySamples: Int, private val decay: Double) {
    // Circular buffer to hold delayed samples. Its size is the delay time.
    private val buffer = DoubleArray(delaySamples)
    private var index = 0 // Current read/write position in the buffer

    /**
     * Processes the input audio samples, applying feedback from the buffer. Y[n] = X[n] + Y[n -
     * delay] * decay (Feedforward + Feedback)
     */
    fun process(input: FloatArray): FloatArray {
      val out = FloatArray(input.size)

      for (i in input.indices) {
        val delayed = buffer[index] // Read the sample from 'delaySamples' ago

        // Calculate the current output: Input signal (X[n]) + (Delayed sample * Decay factor)
        val current = input[i].toDouble() + delayed * decay

        buffer[index] = current // Store the current output sample back into the buffer
        out[i] = current.toFloat()

        // Advance the circular buffer index
        index = (index + 1) % delaySamples
      }

      return out
    }
  }

  internal class AllpassFilter(private val delaySamples: Int, private val gain: Double) {
    // Circular buffer to hold delayed samples.
    private val buffer = DoubleArray(delaySamples)
    private var index = 0

    /**
     * Processes the input audio samples, applying phase shift and diffusion. Y[n] = X[n - delay] +
     * gain * (Y[n - delay] - X[n])
     */
    fun process(input: FloatArray): FloatArray {
      val out = FloatArray(input.size)

      for (i in input.indices) {
        val bufOut = buffer[index] // Y[n - delay] (The delayed output sample)
        val x = input[i].toDouble() // X[n] (The current input sample)

        // Calculate output Y[n]: -X[n] * gain + X[n - delay]
        // This is the feedforward component of the allpass structure
        val y = -x * gain + bufOut

        // Calculate feedback: X[n] + Y[n - delay] * gain
        // This is stored in the buffer for the next input sample
        buffer[index] = x + bufOut * gain

        out[i] = y.toFloat()
        index = (index + 1) % delaySamples
      }

      return out
    }
  }
  fun pitchShift(samples: FloatArray, semitones: Int): FloatArray {
    if (semitones == 0) return samples

    val pitchFactor = 2.0.pow(semitones / 12.0).toFloat()

    val out = FloatArray(samples.size)

    for (i in out.indices) {
      val origIndex = i * pitchFactor
      val idx0 = origIndex.toInt().coerceIn(0, samples.size - 1)
      val idx1 = (idx0 + 1).coerceIn(0, samples.size - 1)
      val frac = origIndex - idx0
      out[i] = samples[idx0] * (1 - frac) + samples[idx1] * frac
    }

    return out
  }


  private val NOTE_TO_SEMITONE =
      mapOf(
          "C" to 0,
          "C#" to 1,
          "Db" to 1,
          "D" to 2,
          "D#" to 3,
          "Eb" to 3,
          "E" to 4,
          "F" to 5,
          "F#" to 6,
          "Gb" to 6,
          "G" to 7,
          "G#" to 8,
          "Ab" to 8,
          "A" to 9,
          "A#" to 10,
          "Bb" to 10,
          "B" to 11)

  /**
   * Convert note + octave into an absolute semitone number (MIDI-like). C4 = 60, A4 = 69, etc.
   * (standard MIDI)
   */
  fun noteToSemitone(note: String, octave: Int): Int {
    val base = NOTE_TO_SEMITONE[note.uppercase()] ?: error("Invalid note name: $note")

    return base + (octave + 1) * 12 // MIDI convention: C4 = 60
  }

  fun semitonesToPitchFactor(semitones: Int): Float {
    return 2.0.pow(semitones / 12.0).toFloat()
  }

  fun computeSemitoneShift(
      inputNote: String,
      inputOctave: Int,
      targetNote: String,
      targetOctave: Int
  ): Int {
    val s1 = noteToSemitone(inputNote, inputOctave)
    val s2 = noteToSemitone(targetNote, targetOctave)
    return s2 - s1
  }
}
