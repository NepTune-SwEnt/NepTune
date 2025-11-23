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
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.project.AudioFileMetadata
import com.neptune.neptune.model.project.ParameterMetadata
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.ProjectWriter
import com.neptune.neptune.model.project.SamplerProjectData
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val originalAudioUri: Uri? = null,
    val currentAudioUri: Uri? = null,
    val audioDurationMillis: Int = 4000,
    val showInitialSetupDialog: Boolean = false,
    val inputTempo: Int = 120,
    val inputPitchNote: String = "C",
    val inputPitchOctave: Int = 4,
    val timeSignature: String = "4/4",
    val previewPlaying: Boolean = false,
    val projectLoadError: String? = null
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

  open fun extractWaveform(uri: Uri, sampleRate: Int = 100): List<Float> {
    val extractor = MediaExtractor()
    extractor.setDataSource(context, uri, null)

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

    if (trackIndex == -1) throw IllegalArgumentException("No audio track")

    val format = extractor.getTrackFormat(trackIndex)
    val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
    codec.configure(format, null, null, 0)
    codec.start()

    val waveform = mutableListOf<Float>()
    val bufferInfo = MediaCodec.BufferInfo()
    var isEOS = false

    while (!isEOS) {
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

      var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
      while (outputIndex >= 0) {
        val outputBuffer = codec.getOutputBuffer(outputIndex)!!
        val shortBuffer = outputBuffer.asShortBuffer()
        val chunk = ShortArray(shortBuffer.remaining())
        shortBuffer.get(chunk)

        if (chunk.isNotEmpty()) {
          val avgAmplitude = chunk.map { abs(it.toFloat()) }.average().toFloat()
          val normalized = (avgAmplitude / Short.MAX_VALUE).coerceIn(0f, 1f)
          if (!normalized.isNaN()) {
            waveform.add(normalized)
          }
        }

        codec.releaseOutputBuffer(outputIndex, false)
        outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
      }
    }

    codec.stop()
    codec.release()
    extractor.release()

    Log.d(
        "WaveformDisplay",
        "Waveform extracted: ${waveform.size} samples, min=${waveform.minOrNull()}, max=${waveform.maxOrNull()}")
    return waveform
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
      saveProjectDataSync(zipFilePath)
      audioBuilding()
    }
  }

  fun saveProjectDataSync(zipFilePath: String) {
    try {
      val state = _uiState.value
      val audioUri = state.currentAudioUri

      if (audioUri == null) {
        Log.e("SamplerViewModel", "No audio saved, action canceled.")
        return
      }

      val audioFile = File(audioUri.path ?: "")
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

  fun audioBuilding() {
    Log.d("SamplerViewModel", "Audio building")
    val originalUri = _uiState.value.originalAudioUri
    val eqBands = _uiState.value.eqBands

    viewModelScope.launch(Dispatchers.Default) {
      try {
        equalizeAudio(originalUri, eqBands)
      } catch (e: Exception) {
        Log.e("SamplerViewModel", "audioBuilding failed: ${e.message}", e)
      }
    }
  }

  fun equalizeAudio(audioUri: Uri?, eqBands: List<Float>) {
    Log.d("SamplerViewModel", "Equalize audio called, eqBands=$eqBands")

    if (audioUri == null) {
      Log.e("SamplerViewModel", "No audio to equalize")
      return
    }

    try {
      // Decode audio to PCM samples
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
      val equalizedSamples = applyEQFilters(samples, sampleRate, eqBands)

      // Determine output file name and path in app cache directory
      val originalName = audioUri.lastPathSegment ?: "sample_audio"
      val dotIndex = originalName.lastIndexOf('.')
      val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
      val outFile = File(context.cacheDir, "${baseName}_equalized.wav")

      // Encode equalized samples back to WAV file
      encodePCMToWAV(equalizedSamples, sampleRate, channelCount, outFile)

      // Update UI state with the new audio Uri
      val newUri = Uri.fromFile(outFile)
      _uiState.update { current -> current.copy(currentAudioUri = newUri) }

      Log.i("SamplerViewModel", "Equalized audio written to ${outFile.absolutePath}")
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Failed to equalize audio: ${e.message}", e)
    }
  }

  internal fun decodeAudioToPCM(uri: Uri): Triple<FloatArray, Int, Int>? {
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(context, uri, null)

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

      val format = extractor.getTrackFormat(trackIndex)
      val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val mime = format.getString(MediaFormat.KEY_MIME)!!

      val codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val allSamples = mutableListOf<Float>()
      val bufferInfo = MediaCodec.BufferInfo()
      var isEOS = false

      while (!isEOS) {
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

        var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputIndex >= 0) {
          val outputBuffer = codec.getOutputBuffer(outputIndex)!!
          val shortBuffer = outputBuffer.asShortBuffer()
          val chunk = ShortArray(shortBuffer.remaining())
          shortBuffer.get(chunk)

          // Convert PCM16 to float (-1.0 to 1.0)
          for (sample in chunk) {
            allSamples.add(sample.toFloat() / Short.MAX_VALUE)
          }

          codec.releaseOutputBuffer(outputIndex, false)
          outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
      }

      codec.stop()
      codec.release()
      extractor.release()

      return Triple(allSamples.toFloatArray(), sampleRate, channelCount)
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error decoding audio: ${e.message}", e)
      extractor.release()
      return null
    }
  }

  internal fun applyEQFilters(
      samples: FloatArray,
      sampleRate: Int,
      eqBands: List<Float>
  ): FloatArray {
    var processedSamples = samples.copyOf()

    // Apply a parametric EQ filter for each band
    EQ_FREQUENCIES.forEachIndexed { index, frequency ->
      val gainDB = eqBands.getOrElse(index) { EQ_GAIN_DEFAULT }

      // Skip if gain is near zero (no effect)
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
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a0: Double
    private val a1: Double
    private val a2: Double

    init {
      val a = 10.0.pow(gainDB / 40.0)
      val omega = 2.0 * Math.PI * centerFreq / sampleRate
      val sinOmega = sin(omega)
      val cosOmega = cos(omega)
      val alpha = sinOmega / (2.0 * q)

      b0 = 1.0 + alpha * a
      b1 = -2.0 * cosOmega
      b2 = 1.0 - alpha * a
      a0 = 1.0 + alpha / a
      a1 = -2.0 * cosOmega
      a2 = 1.0 - alpha / a
    }

    fun process(input: FloatArray): FloatArray {
      val output = FloatArray(input.size)
      var x1 = 0.0
      var x2 = 0.0
      var y1 = 0.0
      var y2 = 0.0

      for (i in input.indices) {
        val x0 = input[i].toDouble()
        val y0 = (b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0

        output[i] = y0.toFloat()

        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
      }

      return output
    }
  }

  internal fun encodePCMToWAV(
      samples: FloatArray,
      sampleRate: Int,
      channelCount: Int,
      outputFile: File
  ) {
    try {
      // Convert float samples back to PCM16
      val pcmData = ByteArray(samples.size * 2) // 16-bit = 2 bytes per sample

      for (i in samples.indices) {
        val sample =
            (samples[i] * Short.MAX_VALUE)
                .coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
                .toInt()
                .toShort()
        pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
        pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
      }

      // Write WAV file with header
      outputFile.outputStream().use { out ->
        // WAV header
        val bitsPerSample = 16
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = channelCount * bitsPerSample / 8
        val dataSize = pcmData.size

        // RIFF chunk
        out.write("RIFF".toByteArray())
        out.write(intToBytes(36 + dataSize)) // ChunkSize
        out.write("WAVE".toByteArray())

        // fmt chunk
        out.write("fmt ".toByteArray())
        out.write(intToBytes(16)) // Subchunk1Size (16 for PCM)
        out.write(shortToBytes(1)) // AudioFormat (1 = PCM)
        out.write(shortToBytes(channelCount.toShort()))
        out.write(intToBytes(sampleRate))
        out.write(intToBytes(byteRate))
        out.write(shortToBytes(blockAlign.toShort()))
        out.write(shortToBytes(bitsPerSample.toShort()))

        // data chunk
        out.write("data".toByteArray())
        out.write(intToBytes(dataSize))
        out.write(pcmData)
      }

      Log.d("SamplerViewModel", "WAV file written: ${outputFile.absolutePath}")
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
}
