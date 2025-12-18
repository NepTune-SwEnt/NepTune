package com.neptune.neptune.ui.sampler

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.domain.usecase.PreviewStoreHelper
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.media.fadeOutAndRelease
import com.neptune.neptune.model.project.AudioFileMetadata
import com.neptune.neptune.model.project.ParameterMetadata
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import com.neptune.neptune.model.project.ProjectWriter
import com.neptune.neptune.model.project.SamplerProjectData
import com.neptune.neptune.ui.sampler.SamplerViewModel.AudioProcessor
import com.neptune.neptune.util.AudioUtils.decodeAudioToPCM
import com.neptune.neptune.util.AudioUtils.encodePCMToWAV
import com.neptune.neptune.util.WaveformExtractor
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val sustain: Float = 1.0f,
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
    val inputTempo: Int = 110,
    val inputPitchNote: String = "C",
    val inputPitchOctave: Int = 4,
    val timeSignature: String = "4/4",
    val previewPlaying: Boolean = false,
    val projectLoadError: String? = null,
    val waveform: List<Float> = emptyList(),
    val isSaving: Boolean = false
) {
  val transposeLabel: String
    get() {
      val semitones =
          SamplerViewModel()
              .computeSemitoneShift(inputPitchNote, inputPitchOctave, pitchNote, pitchOctave)
      val sign = if (semitones > 0) "+" else ""
      return "$sign$semitones st"
    }
}

open class SamplerViewModel(
    private val previewStoreHelper: PreviewStoreHelper = PreviewStoreHelper()
) : ViewModel() {

  val context: Context = NepTuneApplication.appContext

  open val mediaPlayer = NeptuneMediaPlayer()

  private var previewJob: Job? = null
  private var previewUri: Uri? = null

  private val audioProcessor: AudioProcessor = NativeAudioProcessor()

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
          _uiState.update { state -> state.copy(isPlaying = true, playbackPosition = 0f) }
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

      val didReset = shouldResetFromEnd || isNearZero
      val newPosition = if (newIsPlaying && didReset) 0.0f else state.playbackPosition

      state.copy(isPlaying = newIsPlaying, playbackPosition = newPosition)
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

  var previewPlayer: MediaPlayer? = null

  private val tapTimes = mutableListOf<Long>()

  fun playPreview(context: Context, semitoneOffset: Int = 0) {
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
    _uiState.update { it.copy(reverbWet = value.coerceIn(0.0f, 1f)) }
  }

  open fun updateReverbSize(value: Float) {
    _uiState.update { it.copy(reverbSize = value.coerceIn(0.1f, 10.0f)) }
  }

  open fun updateReverbWidth(value: Float) {
    _uiState.update { it.copy(reverbWidth = value.coerceIn(0.0f, 1f)) }
  }

  open fun updateReverbDepth(value: Float) {
    _uiState.update { it.copy(reverbDepth = value.coerceIn(0.0f, 1f)) }
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
    _uiState.update { it.copy(compKnee = value.coerceIn(0f, COMP_KNEE_MAX)) }
  }

  open fun updateCompGain(value: Float) {
    _uiState.update { it.copy(compGain = value.coerceIn(COMP_GAIN_MIN, COMP_GAIN_MAX)) }
  }

  open fun updateCompAttack(value: Float) {
    _uiState.update { it.copy(compAttack = value.coerceIn(0f, COMP_TIME_MAX)) }
  }

  open fun updateCompDecay(value: Float) {
    _uiState.update { it.copy(compDecay = value.coerceIn(0f, COMP_TIME_MAX)) }
  }

  fun loadWaveform(uri: Uri) {
    viewModelScope.launch {
      val wf = extractWaveformInternal(uri)
      _uiState.update { it.copy(waveform = wf) }
    }
  }

  fun resetSampleAndSave(zipFilePath: String) {
    viewModelScope.launch {
      _uiState.update { state ->
        state.copy(
            // Playback
            isPlaying = false,
            previewPlaying = false,
            playbackPosition = 0f,

            // Tempo
            tempo = state.inputTempo,
            pitchNote = state.inputPitchNote,
            pitchOctave = state.inputPitchOctave,

            // ADSR
            attack = 0.0f,
            decay = 0.0f,
            sustain = 1.0f,
            release = 0.0f,

            // EQ
            eqBands = List(EQ_FREQUENCIES.size) { EQ_GAIN_DEFAULT },

            // Reverb
            reverbWet = 0.0f,
            reverbSize = 0.0f,
            reverbWidth = 0.0f,
            reverbDepth = 0.0f,
            reverbPredelay = 10.0f,

            // Compressor
            compThreshold = COMP_THRESHOLD_DEFAULT,
            compRatio = 4,
            compKnee = 0.0f,
            compGain = 0.0f,
            compAttack = 0.010f,
            compDecay = 0.100f,

            // Audio
            currentAudioUri = state.originalAudioUri,
            audioDurationMillis = state.audioDurationMillis,

            // UI
            currentTab = SamplerTab.BASICS,
            projectLoadError = null)
      }

      stopPreview()

      // Reprocess audio from original source
      val processingJob = audioBuilding()
      processingJob?.join()

      // Save immediately
      saveProjectData(zipFilePath)
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

  var adsrPlaying = false

  open fun startADSRSampleWithPitch(semitoneOffset: Int) {
    val baseUri = _uiState.value.originalAudioUri ?: return
    if (adsrPlaying) return

    adsrPlaying = true

    previewJob =
        viewModelScope.launch {
          val previewUri =
              withContext(dispatcherProvider.default) {
                processAudio(
                    currentAudioUri = baseUri,
                    eqBands = _uiState.value.eqBands,
                    reverbWet = _uiState.value.reverbWet,
                    reverbSize = _uiState.value.reverbSize,
                    reverbWidth = _uiState.value.reverbWidth,
                    reverbDepth = _uiState.value.reverbDepth,
                    reverbPredelay = _uiState.value.reverbPredelay,
                    semitones = semitoneOffset,
                    tempoRatio = 1.0,
                    audioProcessor = audioProcessor,
                    attack = _uiState.value.attack,
                    decay = _uiState.value.decay,
                    sustain = _uiState.value.sustain,
                    release = _uiState.value.release,
                    mode = AudioProcessMode.PREVIEW,
                    outputNameSuffix = "note_$semitoneOffset")
              }

          withContext(Dispatchers.Main) {
            if (previewUri != null) {
              previewPlayer?.release()
              previewPlayer =
                  MediaPlayer().apply {
                    setDataSource(context, previewUri)
                    prepare()
                    start()
                    setOnCompletionListener { stopADSRSample() }
                  }
              _uiState.update { it.copy(previewPlaying = true) }
            }
          }
        }
  }

  open fun stopADSRSample() {
    if (!adsrPlaying) return
    adsrPlaying = false

    previewJob?.cancel()
    previewJob = null

    val player = previewPlayer
    previewPlayer = null

    if (player != null) {
      val releaseMillis = (_uiState.value.release * 1000f).toLong()

      fadeOutAndRelease(mediaPlayer = player, releaseMillis = releaseMillis)
    }

    _uiState.update { it.copy(previewPlaying = false) }
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

        val sampleDuration = extractDurationFromUri(audioUri)

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
                inputTempo = current.inputTempo,
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
            // pitchValue stores NOTE_INDEX + (octave * NOTE_ORDER.size)
            // Reconstruct note index and octave from the stored semitone-like value.
            val pitchInt = pitchValue.roundToInt()
            val loadedPitchOctave = (pitchInt / NOTE_ORDER.size).coerceIn(minOctave, maxOctave)

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
      _uiState.update { it.copy(isSaving = true) }

      try {
        val processingJob = audioBuilding()
        processingJob?.join()

        val newAudioUri: Uri? = _uiState.value.currentAudioUri

        if (newAudioUri != null) {
          _uiState.update { it.copy(currentAudioUri = newAudioUri) }
          val duration = extractDurationFromUri(newAudioUri)

          if (duration > 0) {
            _uiState.update { it.copy(audioDurationMillis = duration) }
          }
        }

        saveProjectDataSync(zipFilePath)

        audioBuilding()

        val projectsJsonRepo = ProjectItemsRepositoryLocal(context)
        val projectId = projectsJsonRepo.findProjectWithProjectFile(zipFilePath).uid

        val state = _uiState.value
        val previewUri = state.currentAudioUri ?: return@launch

        val previewLocalPath =
            previewStoreHelper.saveTempPreviewToPreviewsDir(projectId, previewUri)

        val project = projectsJsonRepo.findProjectWithProjectFile(zipFilePath)
        val updated = project.copy(audioPreviewLocalPath = previewLocalPath)
        projectsJsonRepo.editProject(project.uid, updated)
      } catch (e: Exception) {
        Log.w("SamplerViewModel", "Failed to save preview for project", e)
      } finally {
        _uiState.update { it.copy(isSaving = false) }
      }
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

      val originalAudioFile = File(state.originalAudioUri?.path)

      val projectDataFixed =
          projectData.copy(
              audioFiles =
                  listOf(
                      AudioFileMetadata(
                          name = originalAudioFile.name,
                          volume = 1.0f,
                          durationSeconds = state.audioDurationMillis / 1000f)))

      val zipFile = File(zipFilePath)
      ProjectWriter()
          .writeProject(
              zipFile = zipFile,
              metadata = projectDataFixed,
              audioFiles = listOf(originalAudioFile))
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Failed to save ZIP file: ${e.message}", e)
    }
  }

  private fun extractDurationFromUri(uri: Uri): Int {
    val extractor = MediaExtractor()
    return try {
      extractor.setDataSource(context, uri, null)

      for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)

        if (mime?.startsWith("audio/") == true) {
          val durationUs = format.getLong(MediaFormat.KEY_DURATION)
          return (durationUs / 1000).toInt()
        }
      }
      -1
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Duration extract failed", e)
      -1
    } finally {
      extractor.release()
    }
  }

  open fun audioBuilding(): Job? {
    val state = _uiState.value
    val originalUri =
        state.originalAudioUri ?: return null // Guards against missing original file URI

    var tempoRatio: Double

    if (state.inputTempo > 0) {
      tempoRatio = state.tempo.toDouble() / state.inputTempo.toDouble()
    } else {
      tempoRatio = 1.0
    }
    // Compute semitone shift
    val semitones =
        computeSemitoneShift(
            state.inputPitchNote, state.inputPitchOctave, state.pitchNote, state.pitchOctave)

    val job =
        viewModelScope.launch {
          try {
            // Execute the synchronous DSP pipeline on the default/background dispatcher
            val newUri =
                withContext(dispatcherProvider.default) {
                  processAudio(
                      currentAudioUri = originalUri,
                      eqBands = state.eqBands,
                      reverbWet = state.reverbWet,
                      reverbSize = state.reverbSize,
                      reverbWidth = state.reverbWidth,
                      reverbDepth = state.reverbDepth,
                      reverbPredelay = state.reverbPredelay,
                      semitones = semitones,
                      tempoRatio = tempoRatio,
                      audioProcessor = audioProcessor,
                      attack = state.attack,
                      decay = state.decay,
                      sustain = state.sustain,
                      release = state.release,
                      mode = AudioProcessMode.PROJECT)
                }

            if (newUri != null) {
              _uiState.update { it.copy(currentAudioUri = newUri) }
            }
          } catch (e: Exception) {
            Log.e("SamplerViewModel", "audioBuilding failed: ${e.message}", e)
          }
        }

    return job
  }

  enum class AudioProcessMode {
    PROJECT,
    PREVIEW
  }

  internal open fun processAudio(
      currentAudioUri: Uri?,
      eqBands: List<Float>,
      reverbWet: Float,
      reverbSize: Float,
      reverbWidth: Float,
      reverbDepth: Float,
      reverbPredelay: Float,
      audioProcessor: AudioProcessor,
      semitones: Int = 0,
      tempoRatio: Double = 1.0,
      attack: Float = 0f,
      decay: Float = 0f,
      sustain: Float = 1f,
      release: Float = 0f,
      mode: AudioProcessMode = AudioProcessMode.PROJECT,
      outputNameSuffix: String = "processed"
  ): Uri? {
    if (currentAudioUri == null) return null

    // 1. Decode Audio: Convert source URI (MP3/WAV) to raw PCM samples (FloatArray)
    val audioData = decodeAudio(currentAudioUri) ?: return null
    var (samples, sampleRate, channelCount) = audioData

    // 2. Apply EQ: Parametric equalization is applied non-destructively to the samples
    samples = applyEQFilters(samples, sampleRate, eqBands)

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
    samples = compressor.process(samples)

    if (tempoRatio != 1.0) {
      samples = audioProcessor.timeStretch(samples, tempoRatio.toFloat())
    }

    if (semitones != 0) {
      samples = audioProcessor.pitchShift(samples, semitones)
    }
    samples = applyADSR(samples, sampleRate, attack, decay, sustain, release)

    // 3. Apply Reverb: Reverb is applied on top of the EQ'd signal
    samples =
        applyReverb(
            samples, sampleRate, reverbWet, reverbSize, reverbWidth, reverbDepth, reverbPredelay)

    // 4. Encode and Save: Prepare the output file path
    val originalName = currentAudioUri.lastPathSegment ?: DEFAULT_AUDIO_BASENAME
    val base = originalName.substringBeforeLast(".")

    val outFile =
        when (mode) {
          AudioProcessMode.PROJECT -> File(context.cacheDir, "${base}_processed.wav")
          AudioProcessMode.PREVIEW ->
              File(
                  context.cacheDir,
                  "${base}_preview_${outputNameSuffix}_${System.currentTimeMillis()}.wav")
        }

    encodeAudio(samples, sampleRate, channelCount, outFile)

    return Uri.fromFile(outFile) // Return the URI of the newly processed file
  }

  interface AudioProcessor {
    fun pitchShift(samples: FloatArray, semitones: Int): FloatArray

    fun timeStretch(samples: FloatArray, tempoRatio: Float): FloatArray
  }

  fun equalizeAudio(audioUri: Uri?, eqBands: List<Float>) {

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
      encodeAudio(processedSamples, sampleRate, channelCount, outFile)

      // Update UI state with the new audio Uri
      val newUri = Uri.fromFile(outFile)
      _uiState.update { current -> current.copy(currentAudioUri = newUri) }
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Failed to equalize audio: ${e.message}", e)
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

  /**
   * Apply an ADSR envelope to the audio samples.
   *
   * @param samples Mono or interleaved stereo PCM samples in [-1.0, 1.0]
   * @param sampleRate Sample rate in Hz
   * @param attack Attack time in seconds
   * @param decay Decay time in seconds
   * @param sustain Sustain level in [0, 1]
   * @param release Release time in seconds
   */
  open fun applyADSR(
      samples: FloatArray,
      sampleRate: Int,
      attack: Float,
      decay: Float,
      sustain: Float,
      release: Float
  ): FloatArray {
    val totalFrames = samples.size
    val attackFrames = (attack * sampleRate).toInt()
    val decayFrames = (decay * sampleRate).toInt()
    val releaseFrames = (release * sampleRate).toInt()
    val sustainFrames = totalFrames - attackFrames - decayFrames - releaseFrames

    val envelope = FloatArray(totalFrames)

    for (i in 0 until totalFrames) {
      envelope[i] =
          when {
            i < attackFrames -> (i.toFloat() / attackFrames)
            i < attackFrames + decayFrames -> {
              val t = (i - attackFrames).toFloat() / decayFrames
              1f + t * (sustain - 1f)
            }
            i < attackFrames + decayFrames + sustainFrames -> sustain
            else -> {
              val t = (i - (attackFrames + decayFrames + sustainFrames)).toFloat() / releaseFrames
              sustain * (1f - t)
            }
          }
    }
    return samples.mapIndexed { index, sample -> sample * envelope[index] }.toFloatArray()
  }

  @org.jetbrains.annotations.VisibleForTesting
  open fun decodeAudio(uri: Uri): Triple<FloatArray, Int, Int>? {
    return decodeAudioToPCM(uri)
  }

  @org.jetbrains.annotations.VisibleForTesting
  open fun encodeAudio(samples: FloatArray, sampleRate: Int, channelCount: Int, file: File) {
    encodePCMToWAV(samples, sampleRate, channelCount, file)
  }
}

class NativeAudioProcessor : AudioProcessor {
  private external fun pitchShiftNative(samples: FloatArray, semitones: Int): FloatArray

  private external fun timeStretchNative(samples: FloatArray, tempoRatio: Float): FloatArray

  companion object {
    init {
      try {
        System.loadLibrary("sampler_jni")
      } catch (e: UnsatisfiedLinkError) {
        Log.w(
            "SamplerViewModel", "Lib native not loaded (JVM test or environnement outside Android)")
      }
    }
  }

  override fun pitchShift(samples: FloatArray, semitones: Int): FloatArray {
    return pitchShiftNative(samples, semitones)
  }

  override fun timeStretch(samples: FloatArray, tempoRatio: Float): FloatArray {
    return timeStretchNative(samples, tempoRatio)
  }
}
