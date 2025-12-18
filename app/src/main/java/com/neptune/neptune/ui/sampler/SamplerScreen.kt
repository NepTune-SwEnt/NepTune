package com.neptune.neptune.ui.sampler

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.R
import com.neptune.neptune.media.LocalMediaPlayer
import com.neptune.neptune.ui.sampler.SamplerTestTags.CURVE_EDITOR_SCROLL_CONTAINER
import com.neptune.neptune.ui.sampler.SamplerTestTags.FADER_60HZ_TAG
import com.neptune.neptune.ui.sampler.SamplerTestTags.PREVIEW_PLAY_BUTTON
import com.neptune.neptune.ui.settings.ThemeDataStore
import com.neptune.neptune.ui.theme.NepTuneTheme
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

object SamplerTestTags {
  const val SCREEN_CONTAINER = "samplerScreenContainer"
  const val PLAYHEAD_CONTROLS = "playheadControls"
  const val WAVEFORM_DISPLAY = "waveformDisplay"
  const val SAMPLER_TABS = "samplerTabs"
  const val TAB_BASICS_CONTENT = "tabBasicsContent"
  const val KNOB_ATTACK = "knobAttack"
  const val KNOB_DECAY = "knobDecay"
  const val KNOB_SUSTAIN = "knobSustain"
  const val KNOB_RELEASE = "knobRelease"

  const val PITCH_SELECTOR = "pitchSelector"
  const val TEMPO_SELECTOR = "tempoSelector"
  const val TIME_SIGNATURE_SELECTOR = "timeSignatureSelector"

  const val KNOB_REVERB_WET = "knobReverbWet"
  const val KNOB_REVERB_SIZE = "knobReverbSize"
  const val KNOB_REVERB_WIDTH = "knobReverbWidth"
  const val KNOB_REVERB_DEPTH = "knobReverbDepth"
  const val KNOB_REVERB_PREDELAY = "knobReverbPredelay"

  const val KNOB_COMP_THRESHOLD = "knobCompThreshold"
  const val INPUT_COMP_RATIO = "inputFieldCompRatio"
  const val KNOB_COMP_KNEE = "knobCompKnee"
  const val KNOB_COMP_GAIN = "knobCompGain"
  const val KNOB_COMP_ATTACK = "knobCompAttack"
  const val KNOB_COMP_DECAY = "knobCompDecay"
  const val SECTION_ADSR = "sectionAdsrControls"
  const val SECTION_REVERB = "sectionReverbControls"

  const val TIME_DISPLAY = "timeDisplay"
  const val EQ_FADER_BOX_INPUT = "eqFaderBoxInput"
  const val FADER_60HZ_TAG = "fader60Hz"
  const val CURVE_EDITOR_SCROLL_CONTAINER = "curveEditorScrollContainer"
  const val INIT_SETUP_CONTAINER = "initSetupContainer"
  const val INIT_TEMPO_SELECTOR = "initTempoSelector"
  const val INIT_CONFIRM_BUTTON = "initConfirmButton"

  const val TAP_TEMPO_BUTTON = "tapTempoButton"
  const val PREVIEW_PLAY_BUTTON = "previewPlayButton"

  const val SETTINGS_RESET_SAMPLE_BUTTON = "settingsResetSampleButton"

  // Settings-related test tags
  const val SETTINGS_BUTTON = "settingsButton"
  const val SETTINGS_DIALOG = "settingsDialog"
  const val SETTINGS_CONFIRM_BUTTON = "settingsConfirmButton"
  const val SETTINGS_CANCEL_BUTTON = "settingsCancelButton"
  const val SETTINGS_PITCH_SELECTOR = "settingsPitchSelector"

  // Help-related test tags
  const val HELP_BUTTON = "helpButton"
  const val HELP_DIALOG = "helpDialog"
  const val HELP_NAV_LEFT = "helpNavLeft"
  const val HELP_NAV_RIGHT = "helpNavRight"
  const val HELP_PAGE_INDICATOR = "helpPageIndicator"
}

val KnobBackground = Color.Black
val PointColor = Color.Red

const val SampleDurationMillis = 4000

const val ADSR_MAX_TIME = 5.0f
const val ADSR_DEFAULT_VIEW_TIME = 5.0f
const val ADSR_MAX_SUSTAIN = 1.0f

const val REVERB_SIZE_MAX = 10.0f
const val PREDELAY_MAX_MS = 100.0f

val ADSR_GRID_COLOR = Color.Gray.copy(alpha = 0.4f)

private const val EQ_GAIN_MIN = -20.0f
private const val EQ_GAIN_MAX = 20.0f

val spectrogramBackground = Color.Black.copy(alpha = 0.5f)

val HELP_DIALOG_TAB_COUNT = 6

enum class KnobUnit {
  SECONDS,
  PERCENT,
  MILLISECONDS,
  NONE
}

private fun getDecodedZipPath(zipFilePath: String?): String? {
  return if (zipFilePath.isNullOrEmpty()) {
    null
  } else {
    URLDecoder.decode(zipFilePath, StandardCharsets.UTF_8.name())
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplerScreen(
    viewModel: SamplerViewModel = viewModel(),
    zipFilePath: String?,
) {
  val uiState by viewModel.uiState.collectAsState()
    val transposeSemitones =
        remember(
            uiState.inputPitchNote,
            uiState.inputPitchOctave,
            uiState.pitchNote,
            uiState.pitchOctave) {
            viewModel.computeSemitoneShift(
                uiState.inputPitchNote,
                uiState.inputPitchOctave,
                uiState.pitchNote,
                uiState.pitchOctave)
        }

    val transposeText =
        remember(transposeSemitones) {
            val sign = if (transposeSemitones > 0) "+" else ""
            "$sign$transposeSemitones st"
        }

  val decodedZipPath = remember(zipFilePath) { getDecodedZipPath(zipFilePath) }

  // Local state to control visibility of the floating settings dialog
  var showSettingsDialog by remember { mutableStateOf(false) }
  // Local state for the help dialog and selected tab
  var showHelpDialog by remember { mutableStateOf(false) }
  var helpTabIndex by remember { mutableIntStateOf(0) }

  // Collect the disabledHelp state from the data store
  val context = NepTuneApplication.appContext
  val dataStore = remember { ThemeDataStore(context) }
  val disabledHelp by dataStore.disableHelp.collectAsState(initial = false)

  LaunchedEffect(Unit) {
    if (decodedZipPath != null) {
      viewModel.loadProjectData(decodedZipPath)
    }
  }

  if (uiState.showInitialSetupDialog) {
    InitialSetupDialog(viewModel)
  }

  Scaffold(
      containerColor = NepTuneTheme.colors.background,
      modifier = Modifier.testTag(SamplerTestTags.SCREEN_CONTAINER),
      floatingActionButton = {
        FloatingActionButton(
            onClick = { showSettingsDialog = true },
            containerColor = NepTuneTheme.colors.accentPrimary,
            modifier = Modifier.testTag(SamplerTestTags.SETTINGS_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Settings",
                  tint = Color.White)
            }
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            PlaybackAndWaveformControls(
                isPlaying = uiState.isPlaying,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSave = {
                  if (decodedZipPath != null) {
                    viewModel.saveProjectData(decodedZipPath)
                    Log.i("SamplerScreen", "Project saved in $decodedZipPath")
                  } else {
                    Log.w("SamplerScreen", "No project path found for saving")
                  }
                },
                pitch = transposeText,
                tempo = uiState.tempo,
                onPitchChange = viewModel::updatePitch,
                onTempoChange = viewModel::updateTempo,
                playbackPosition = uiState.playbackPosition,
                onPositionChange = viewModel::updatePlaybackPosition,
                onIncreasePitch = viewModel::increasePitch,
                onDecreasePitch = viewModel::decreasePitch,
                uiState = uiState,
                viewModel = viewModel)
            Spacer(modifier = Modifier.height(16.dp))

            var showPiano by remember { mutableStateOf(false) }

            ADSRTestButton(onTogglePiano = { showPiano = !showPiano }, isPianoVisible = showPiano)

            Spacer(modifier = Modifier.height(16.dp))

            if (showPiano) {
              ScrollablePiano(viewModel = viewModel)
            } else {
              SamplerTabs(currentTab = uiState.currentTab, onTabSelected = viewModel::selectTab)
              TabContent(currentTab = uiState.currentTab, uiState = uiState, viewModel = viewModel)
            }
          }

          // Bottom-left Help button
          // Only show the Help button if help is not disabled
          if (!disabledHelp) {
            HelpButton { showHelpDialog = true }
          }
        }
      }

  // Show a simple settings dialog when settings button is pressed
  if (showSettingsDialog) {
    SettingsDialog(
        viewModel = viewModel, zipFilePath = zipFilePath, onClose = { showSettingsDialog = false })
  }

  // Help dialog - multi-tab explanatory dialog
  if (showHelpDialog) {
    HelpDialog(
        selectedTab = helpTabIndex,
        onTabSelected = { helpTabIndex = it },
        onClose = { showHelpDialog = false })
  }
}

@Composable
fun HelpButton(onClick: () -> Unit) {
  Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(56.dp),
        shape = CircleShape,
        color = NepTuneTheme.colors.accentPrimary,
        tonalElevation = 4.dp) {
          IconButton(onClick = onClick, modifier = Modifier.testTag(SamplerTestTags.HELP_BUTTON)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Help",
                tint = Color.White)
          }
        }
  }
}

@Composable
fun ADSRTestButton(
    onTogglePiano: () -> Unit,
    isPianoVisible: Boolean,
    modifier: Modifier = Modifier
) {

  val color =
      if (isPianoVisible) NepTuneTheme.colors.accentPrimary else NepTuneTheme.colors.soundWave
  val icon = if (isPianoVisible) Icons.Default.Close else Icons.Default.Piano
  val text = if (isPianoVisible) "Close Keyboard" else "Open Keyboard"

  Surface(
      modifier =
          modifier.testTag("ADSR_TEST_BUTTON").fillMaxWidth().height(40.dp).clickable {
            onTogglePiano()
          },
      color = color,
      shape = RoundedCornerShape(12.dp),
      tonalElevation = 2.dp,
      shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
              Icon(imageVector = icon, contentDescription = text, tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = text, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
      }
}

@Composable
fun ScrollablePiano(
    viewModel: SamplerViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState() // par défaut
) {
  val uiState by viewModel.uiState.collectAsState()
  val baseOctave = uiState.inputPitchOctave
  val startOctave = (baseOctave - 1).coerceAtLeast(1)
  val endOctave = (baseOctave + 1).coerceAtMost(7)
  val octaves = (startOctave..endOctave).toList()

  LaunchedEffect(Unit) { listState.scrollToItem(1) }

  LazyRow(state = listState, modifier = modifier.fillMaxWidth().height(200.dp)) {
    items(octaves) { octave ->
      PianoOctave(
          viewModel = viewModel,
          octave = octave,
          modifier = Modifier.width(56.dp * 7).testTag("PianoOctave") // important pour test
          )
    }
  }
}

private val BlackKeyLayout =
    listOf(
        BlackKey(semitone = 1, afterWhiteIndex = 0),
        BlackKey(semitone = 3, afterWhiteIndex = 1),
        BlackKey(semitone = 6, afterWhiteIndex = 3),
        BlackKey(semitone = 8, afterWhiteIndex = 4),
        BlackKey(semitone = 10, afterWhiteIndex = 5))

data class BlackKey(val semitone: Int, val afterWhiteIndex: Int)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PianoOctave(viewModel: SamplerViewModel, octave: Int, modifier: Modifier = Modifier) {
  val whiteKeys = listOf(0, 2, 4, 5, 7, 9, 11)

  BoxWithConstraints(modifier = modifier.height(200.dp).testTag("PianoOctave")) {
    val width = maxWidth
    val whiteKeyWidth = width / whiteKeys.size
    val blackKeyWidth = whiteKeyWidth * 0.6f

    Row(Modifier.fillMaxSize()) {
      whiteKeys.forEach { semitone ->
        val globalSemitone = semitone + (octave - 4) * 12

        PianoKey(
            semitone = globalSemitone,
            viewModel = viewModel,
            isBlack = false,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            label = if (semitone == 0) "C$octave" else null)
      }
    }

    BlackKeyLayout.forEach { key ->
      val globalSemitone = key.semitone + (octave - 4) * 12

      PianoKey(
          semitone = globalSemitone,
          viewModel = viewModel,
          isBlack = true,
          modifier =
              Modifier.width(blackKeyWidth)
                  .fillMaxHeight(0.65f)
                  .offset(x = whiteKeyWidth * (key.afterWhiteIndex + 1) - blackKeyWidth / 2))
    }
  }
}

@Composable
fun PianoKey(
    semitone: Int,
    viewModel: SamplerViewModel,
    isBlack: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null
) {
  Surface(
      modifier =
          modifier.pointerInput(semitone) {
            detectTapGestures(
                onPress = {
                  viewModel.startADSRSampleWithPitch(semitone)
                  tryAwaitRelease()
                  viewModel.stopADSRSample()
                })
          },
      color = if (isBlack) Color.Black else Color.White,
      border = if (!isBlack) BorderStroke(1.dp, Color.Black) else null,
      shape =
          RoundedCornerShape(
              bottomStart = if (isBlack) 4.dp else 8.dp, bottomEnd = if (isBlack) 4.dp else 8.dp),
      shadowElevation = if (isBlack) 6.dp else 2.dp) {
        if (label != null) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 6.dp))
          }
        }
      }
}

@Composable
fun PlaybackAndWaveformControls(
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSave: () -> Unit,
    pitch: String,
    tempo: Int,
    onPitchChange: (String) -> Unit,
    onTempoChange: (Int) -> Unit,
    playbackPosition: Float,
    onPositionChange: (Float) -> Unit,
    onIncreasePitch: () -> Unit,
    onDecreasePitch: () -> Unit,
    uiState: SamplerUiState,
    viewModel: SamplerViewModel
) {

  val mediaPlayer = LocalMediaPlayer.current
  val currentUri = uiState.currentAudioUri

  val playbackPosition = uiState.playbackPosition
  val audioDurationMillis = uiState.audioDurationMillis
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(MaterialTheme.shapes.small)
              .background(NepTuneTheme.colors.background)
              .testTag(SamplerTestTags.PLAYHEAD_CONTROLS)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = NepTuneTheme.colors.accentPrimary,
                    modifier = Modifier.size(32.dp))
              }
              IconButton(onClick = onSave, enabled = !uiState.isSaving) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = if (uiState.isSaving) Color.Gray else NepTuneTheme.colors.accentPrimary,
                    modifier = Modifier.size(32.dp))
              }
              Spacer(modifier = Modifier.weight(1f))

              PitchTempoSelector(
                  value = pitch,
                  onIncrease = onIncreasePitch,
                  onDecrease = onDecreasePitch,
                  modifier =
                      Modifier.border(
                              2.dp, NepTuneTheme.colors.accentPrimary, MaterialTheme.shapes.small)
                          .testTag(SamplerTestTags.PITCH_SELECTOR))
              Spacer(modifier = Modifier.width(8.dp))

              PitchTempoSelector(
                  label = tempo.toString(),
                  value = "",
                  onIncrease = { onTempoChange(tempo + 1) },
                  onDecrease = { onTempoChange(tempo - 1) },
                  modifier =
                      Modifier.border(
                              2.dp, NepTuneTheme.colors.accentPrimary, MaterialTheme.shapes.small)
                          .testTag(SamplerTestTags.TEMPO_SELECTOR))
              Spacer(modifier = Modifier.width(8.dp))

              // Time signature dropdown placed next to tempo
              TimeSignatureSelector(
                  selected = uiState.timeSignature,
                  onSelect = { viewModel.updateTimeSignature(it) },
                  modifier =
                      Modifier.border(
                              2.dp, NepTuneTheme.colors.accentPrimary, MaterialTheme.shapes.small)
                          .testTag(SamplerTestTags.TIME_SIGNATURE_SELECTOR))
            }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
          WaveformDisplay(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(120.dp)
                      .border(2.dp, NepTuneTheme.colors.accentPrimary, MaterialTheme.shapes.small)
                      .testTag(SamplerTestTags.WAVEFORM_DISPLAY),
              isPlaying = isPlaying,
              playbackPosition = playbackPosition,
              onPositionChange = onPositionChange,
              audioDurationMillis = uiState.audioDurationMillis.toLong(),
              uiState = uiState,
              viewModel = viewModel)
          TimeDisplay(
              playbackPosition = playbackPosition,
              audioDurationMillis = audioDurationMillis,
              modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp, end = 8.dp))
        }
      }
}

@Composable
fun PitchTempoSelector(
    label: String = "",
    value: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text = label, color = NepTuneTheme.colors.smallText, fontSize = 16.sp)
        if (value.isNotEmpty()) {
          Text(
              text = value,
              color = NepTuneTheme.colors.smallText,
              fontSize = 16.sp,
              modifier = Modifier.padding(start = 4.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              imageVector = Icons.Default.KeyboardArrowUp,
              contentDescription = "Increase",
              tint = NepTuneTheme.colors.accentPrimary,
              modifier = Modifier.size(24.dp).clickable(onClick = onIncrease))
          Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = "Decrease",
              tint = NepTuneTheme.colors.accentPrimary,
              modifier = Modifier.size(24.dp).clickable(onClick = onDecrease))
        }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSignatureSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  val options = listOf("4/4", "3/4", "2/4", "6/8", "5/4")

  Box(modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 4.dp)) {
          Text(text = "Time", color = NepTuneTheme.colors.smallText, fontSize = 16.sp)
          Text(
              text = selected,
              color = NepTuneTheme.colors.smallText,
              fontSize = 16.sp,
              modifier = Modifier.padding(start = 6.dp))
          Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = "Open",
              tint = NepTuneTheme.colors.accentPrimary,
              modifier = Modifier.size(20.dp))
        }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { opt ->
        DropdownMenuItem(
            text = { Text(opt) },
            onClick = {
              onSelect(opt)
              expanded = false
            })
      }
    }
  }
}

@Composable
fun WaveformDisplay(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    playbackPosition: Float = 0.0f,
    onPositionChange: (Float) -> Unit = {},
    audioDurationMillis: Long,
    uiState: SamplerUiState,
    viewModel: SamplerViewModel
) {
  val soundWaveColor = NepTuneTheme.colors.soundWave
  val localDensity = LocalDensity.current
  val latestOnPositionChange = rememberUpdatedState(onPositionChange)
  val currentUri = uiState.currentAudioUri

  LaunchedEffect(currentUri) {
    if (currentUri != null) {
      viewModel.loadWaveform(currentUri)
    }
  }
  val waveform = uiState.waveform

  val playbackPositionAnimatable = remember { Animatable(playbackPosition) }

  LaunchedEffect(playbackPosition) {
    if (!isPlaying) {
      playbackPositionAnimatable.snapTo(playbackPosition)
    }
  }

  LaunchedEffect(isPlaying) {
    if (isPlaying) {
      if (playbackPositionAnimatable.value < 1.0f) {
        val durationMillis = audioDurationMillis
        playbackPositionAnimatable.animateTo(
            targetValue = 1.0f,
            animationSpec =
                tween(
                    durationMillis =
                        (durationMillis * (1.0f - playbackPositionAnimatable.value)).roundToInt(),
                    easing = LinearEasing))

        latestOnPositionChange.value(1.0f)
      }
    } else {
      playbackPositionAnimatable.stop()
      latestOnPositionChange.value(playbackPositionAnimatable.value)
    }
  }

  val currentAnimPosition = playbackPositionAnimatable.value

  val playheadColor = if (isPlaying) Color.Green else Color.Red

  val paddingPx = localDensity.run { 8.dp.toPx() }
  val playheadStrokeWidth = localDensity.run { 1.5.dp.toPx() }

  // Precompute label color and text size in composable scope to use inside draw lambda
  val timelineLabelColor = NepTuneTheme.colors.smallText.copy(alpha = 0.8f)
  val timelineLabelColorInt = timelineLabelColor.toArgb()
  val timelineTextSizePx = localDensity.run { 12.sp.toPx() }
  // capture theme colors to use inside Canvas (avoid calling composable from non-composable scope)
  val accentPrimaryColor = NepTuneTheme.colors.accentPrimary
  val smallTextColor = NepTuneTheme.colors.smallText

  // Compute beat count and related metrics in composable scope so tests can access it
  val tempoBPM = uiState.tempo.coerceAtLeast(1)
  val tsParts = uiState.timeSignature.split('/')
  val (tsNum, tsDen) =
      if (tsParts.size == 2) {
        val n = tsParts[0].toIntOrNull() ?: 4
        val d = tsParts[1].toIntOrNull() ?: 4
        Pair(n.coerceAtLeast(1), d.coerceAtLeast(1))
      } else {
        Pair(4, 4)
      }

  val secondsPerBeat = 60f / tempoBPM.toFloat() * (4f / tsDen.toFloat())
  val totalDurationSec = audioDurationMillis.toFloat() / 1000f
  val computedBeatCount =
      if (secondsPerBeat > 0f && totalDurationSec > 0f) {
        (totalDurationSec / secondsPerBeat).toInt().coerceAtLeast(1)
      } else {
        0
      }

  Box(
      modifier =
          modifier.background(spectrogramBackground).padding(8.dp).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                  change.consume()

                  val rawX = change.position.x
                  val contentWidth = size.width - (2 * paddingPx)

                  val newPosition = (rawX - paddingPx) / contentWidth

                  latestOnPositionChange.value(newPosition.coerceIn(0f, 1f))
                })
          }) {
        // Invisible beat info for tests
        Text(
            text = "beats:$computedBeatCount",
            color = Color.Transparent,
            modifier = Modifier.testTag("waveform_beat_info").alpha(0f))

        Canvas(modifier = Modifier.fillMaxSize()) {
          val width = size.width
          val height = size.height
          val centerY = height / 2f
          val contentWidth = width - (2 * paddingPx)

          val barWidth = 2.dp.toPx()
          val numBars = 50
          val gapWidth = (contentWidth - (numBars * barWidth)) / (numBars - 1).coerceAtLeast(1)

          // Draw timeline ticks and second labels at the top
          val totalSeconds = (audioDurationMillis / 1000L).coerceAtLeast(1L).toInt()
          val pixelsPerSecond =
              if (totalSeconds > 0) contentWidth / totalSeconds.toFloat() else contentWidth
          // lift the timeline slightly above its previous position
          val tickTop = (-5).dp.toPx()
          val tickBottom = 5.dp.toPx()

          val textPaint =
              Paint().apply {
                isAntiAlias = true
                color = timelineLabelColorInt
                textSize = timelineTextSizePx
                textAlign = Paint.Align.LEFT // we'll position explicitly
              }

          // Draw beat grid (primary and secondary beats) based on tempo and time signature
          // Parse time signature like "4/4". If parsing fails, fallback to 4/4.
          val tempoBPM = uiState.tempo.coerceAtLeast(1)
          val tsParts = uiState.timeSignature.split('/')
          val (tsNum, tsDen) =
              if (tsParts.size == 2) {
                val n = tsParts[0].toIntOrNull() ?: 4
                val d = tsParts[1].toIntOrNull() ?: 4
                Pair(n.coerceAtLeast(1), d.coerceAtLeast(1))
              } else {
                Pair(4, 4)
              }

          // seconds per beat unit where BPM is assumed to reference a quarter-note by default.
          // secondsPerBeat = 60 / BPM * (4 / denominator)
          val secondsPerBeat = 60f / tempoBPM.toFloat() * (4f / tsDen.toFloat())
          val totalDurationSec = audioDurationMillis.toFloat() / 1000f

          // Make beat lines thicker and density-aware
          val primaryBeatStroke = localDensity.run { 3.dp.toPx() }
          val secondaryBeatStroke = localDensity.run { 1.5.dp.toPx() }

          if (secondsPerBeat > 0f && totalDurationSec > 0f) {
            // compute how many beats fit into the audio
            val beatCount = (totalDurationSec / secondsPerBeat).toInt().coerceAtLeast(1)
            for (beatIndex in 0..beatCount) {
              val timeSec = beatIndex * secondsPerBeat
              if (timeSec > totalDurationSec) break

              // Map time (seconds) to canvas x using the full floating totalDurationSec for
              // accuracy
              val xPos = paddingPx + (timeSec / totalDurationSec) * contentWidth

              val beatInMeasure = if (tsNum > 0) beatIndex % tsNum else beatIndex
              val isPrimary = beatInMeasure == 0

              // Primary beats (downbeats) are slightly stronger.
              if (isPrimary) {
                drawLine(
                    color = accentPrimaryColor.copy(alpha = 0.25f),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, height),
                    strokeWidth = primaryBeatStroke)
              } else {
                drawLine(
                    color = smallTextColor.copy(alpha = 0.10f),
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, height),
                    strokeWidth = secondaryBeatStroke)
              }
            }
          }

          // Calculate label step to avoid overlapping labels horizontally
          val minLabelSpacingPx = 24.dp.toPx()
          val labelStep = max(1, ceil(minLabelSpacingPx / pixelsPerSecond).toInt())

          val labelHorizontalPadding = 4.dp.toPx()
          for (sec in 0..totalSeconds) {
            val xPos = paddingPx + sec * pixelsPerSecond
            // small tick for each second
            drawLine(
                color = timelineLabelColor,
                start = Offset(xPos, tickTop),
                end = Offset(xPos, tickBottom),
                strokeWidth = 2f)

            // draw a label only every `labelStep` seconds to avoid overlap
            if (sec % labelStep == 0) {
              val label = "${sec}s"

              // measure label width
              val labelWidth = textPaint.measureText(label)

              // Preferred position: to the right of the tick
              var labelX = xPos + labelHorizontalPadding

              // If it would overflow past the right content bound, draw to the left of the tick
              // instead
              val rightBound = width - paddingPx
              if (labelX + labelWidth > rightBound) {
                // position so label's right edge sits left of the tick
                labelX = xPos - labelHorizontalPadding - labelWidth
                textPaint.textAlign = Paint.Align.LEFT
              } else {
                textPaint.textAlign = Paint.Align.LEFT
              }

              // Compute baseline relative to the tick bottom so vertical offset actually moves the
              // labels.
              // When offset is 0 the baseline sits just below the tickBottom + text height.
              val labelVerticalOffset = 12.dp.toPx() // positive => move label *up*
              val baselineFromTickBottom = tickBottom + timelineTextSizePx
              // allow labels to move higher by lowering the minimum allowed baseline
              val minLabelBaseline = timelineTextSizePx * 0.25f
              val labelBaseline =
                  max(minLabelBaseline, baselineFromTickBottom - labelVerticalOffset)

              drawContext.canvas.nativeCanvas.drawText(label, labelX, labelBaseline, textPaint)
            }
          }

          drawLine(
              color = Color.Gray.copy(alpha = 0.5f),
              start = Offset(paddingPx, centerY),
              end = Offset(width - paddingPx, centerY),
              strokeWidth = 1f)

          if (waveform.isNotEmpty()) {
            val maxAmplitude = waveform.maxOrNull() ?: 1f
            waveform.forEachIndexed { i, amp ->
              val normalized = (amp / maxAmplitude).coerceIn(0f, 1f)
              val barHeight = normalized * (height - 2 * paddingPx) * 0.8f
              val startX = paddingPx + i * (barWidth + gapWidth) + barWidth / 2
              val startY = centerY - barHeight / 2
              val endY = centerY + barHeight / 2

              drawLine(
                  color = soundWaveColor,
                  start = Offset(startX, startY),
                  end = Offset(startX, endY),
                  strokeWidth = barWidth)
            }
          } else {
            for (i in 0 until 50) {
              val simulatedAmplitude = (sin(i * 0.4f) * 0.3f + 0.5f)
              val barHeight = simulatedAmplitude * (height - 2 * paddingPx) * 0.7f
              val startX = paddingPx + i * (barWidth + gapWidth) + barWidth / 2
              val startY = centerY - barHeight / 2
              val endY = centerY + barHeight / 2

              drawLine(
                  color = soundWaveColor.copy(alpha = 0.4f),
                  start = Offset(startX, startY),
                  end = Offset(startX, endY),
                  strokeWidth = barWidth)
            }
          }

          val xPosition = contentWidth * currentAnimPosition + paddingPx

          drawLine(
              brush = SolidColor(playheadColor),
              start = Offset(xPosition, paddingPx),
              end = Offset(xPosition, height - paddingPx),
              strokeWidth = playheadStrokeWidth)
        }
      }
}

@Composable
fun SamplerTabs(currentTab: SamplerTab, onTabSelected: (SamplerTab) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().testTag(SamplerTestTags.SAMPLER_TABS)) {
    SamplerTab.entries.forEach { tab ->
      val isSelected = tab == currentTab
      Text(
          text = tab.name.uppercase(),
          color =
              if (isSelected) NepTuneTheme.colors.accentPrimary
              else NepTuneTheme.colors.smallText.copy(alpha = 0.6f),
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          modifier =
              Modifier.weight(1f)
                  .clip(MaterialTheme.shapes.small)
                  .clickable { onTabSelected(tab) }
                  .background(
                      if (isSelected) NepTuneTheme.colors.smallText.copy(alpha = 0.1f)
                      else Color.Transparent)
                  .padding(vertical = 8.dp, horizontal = 4.dp)
                  .wrapContentWidth(Alignment.CenterHorizontally)
                  .testTag("${SamplerTestTags.SAMPLER_TABS}_${tab.name.uppercase()}"))
    }
  }
}

@Composable
fun TabContent(currentTab: SamplerTab, uiState: SamplerUiState, viewModel: SamplerViewModel) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .wrapContentHeight()
              .padding(top = 8.dp)
              .border(2.dp, NepTuneTheme.colors.accentPrimary)) {
        when (currentTab) {
          SamplerTab.BASICS -> BasicsTabContent(uiState, viewModel)
          SamplerTab.EQ -> EQTabContent(uiState, viewModel)
          SamplerTab.COMP -> CompTabContent(uiState, viewModel)
        }
      }
}

@Composable
fun BasicsTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
  var isADSrExpanded by remember { mutableStateOf(false) }
  var isReverbExpanded by remember { mutableStateOf(false) }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .wrapContentHeight()
              .verticalScroll(rememberScrollState())
              .testTag(SamplerTestTags.TAB_BASICS_CONTENT)) {
        ExpandableSection(
            title = "ADSR Envelope Controls",
            isExpanded = isADSrExpanded,
            onToggle = { isADSrExpanded = !isADSrExpanded }) {
              Column(modifier = Modifier.fillMaxWidth()) {
                ADSRCurveEditor(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                            .testTag("curveEditor"),
                    attack = uiState.attack,
                    decay = uiState.decay,
                    sustain = uiState.sustain,
                    release = uiState.release,
                    onAttackChange = viewModel::updateAttack,
                    onDecayChange = viewModel::updateDecay,
                    onSustainChange = viewModel::updateSustain,
                    onReleaseChange = viewModel::updateRelease,
                    maxTime = ADSR_MAX_TIME,
                    maxSustain = ADSR_MAX_SUSTAIN)

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 8.dp)
                            .background(NepTuneTheme.colors.background),
                    horizontalArrangement = Arrangement.SpaceAround) {
                      UniversalKnob(
                          label = "Attack",
                          value = uiState.attack,
                          onValueChange = viewModel::updateAttack,
                          minValue = 0.0f,
                          maxValue = ADSR_MAX_TIME,
                          unit = KnobUnit.SECONDS,
                          modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_ATTACK))
                      UniversalKnob(
                          label = "Decay",
                          value = uiState.decay,
                          onValueChange = viewModel::updateDecay,
                          minValue = 0.0f,
                          maxValue = ADSR_MAX_TIME,
                          unit = KnobUnit.SECONDS,
                          modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_DECAY))
                      UniversalKnob(
                          label = "Sustain",
                          value = uiState.sustain,
                          onValueChange = viewModel::updateSustain,
                          minValue = 0.0f,
                          maxValue = ADSR_MAX_SUSTAIN,
                          unit = KnobUnit.PERCENT,
                          modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_SUSTAIN))
                      UniversalKnob(
                          label = "Release",
                          value = uiState.release,
                          onValueChange = viewModel::updateRelease,
                          minValue = 0.0f,
                          maxValue = ADSR_MAX_TIME,
                          unit = KnobUnit.SECONDS,
                          modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_RELEASE))
                    }
              }
            }

        ExpandableSection(
            title = "Reverb Controls",
            isExpanded = isReverbExpanded,
            onToggle = { isReverbExpanded = !isReverbExpanded }) {
              Column(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(vertical = 8.dp)
                          .background(NepTuneTheme.colors.background)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround) {
                          UniversalKnob(
                              label = "Wet",
                              value = uiState.reverbWet,
                              onValueChange = viewModel::updateReverbWet,
                              minValue = 0.0f,
                              maxValue = 1.0f,
                              unit = KnobUnit.PERCENT,
                              modifier =
                                  Modifier.weight(1f).testTag(SamplerTestTags.KNOB_REVERB_WET))
                          UniversalKnob(
                              label = "Size",
                              value = uiState.reverbSize,
                              onValueChange = viewModel::updateReverbSize,
                              minValue = 0.1f,
                              maxValue = REVERB_SIZE_MAX,
                              unit = KnobUnit.SECONDS,
                              modifier =
                                  Modifier.weight(1f).testTag(SamplerTestTags.KNOB_REVERB_SIZE))
                          UniversalKnob(
                              label = "Width",
                              value = uiState.reverbWidth,
                              onValueChange = viewModel::updateReverbWidth,
                              minValue = 0.0f,
                              maxValue = 1.0f,
                              unit = KnobUnit.PERCENT,
                              modifier =
                                  Modifier.weight(1f).testTag(SamplerTestTags.KNOB_REVERB_WIDTH))
                        }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround) {
                          UniversalKnob(
                              label = "Depth",
                              value = uiState.reverbDepth,
                              onValueChange = viewModel::updateReverbDepth,
                              minValue = 0.0f,
                              maxValue = 1.0f,
                              unit = KnobUnit.PERCENT,
                              modifier =
                                  Modifier.weight(1f).testTag(SamplerTestTags.KNOB_REVERB_DEPTH))
                          UniversalKnob(
                              label = "Predelay",
                              value = uiState.reverbPredelay,
                              onValueChange = viewModel::updateReverbPredelay,
                              minValue = 0.0f,
                              maxValue = PREDELAY_MAX_MS,
                              unit = KnobUnit.MILLISECONDS,
                              modifier =
                                  Modifier.weight(1f).testTag(SamplerTestTags.KNOB_REVERB_PREDELAY))
                          Spacer(modifier = Modifier.weight(1f))
                        }
                  }
            }
      }
}

enum class CurvePoint(val id: Int) {
  P1(1),
  P2(2),
  P3(3)
}

@Composable
fun CurveCanvas(
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
    p1x: Float,
    p2x: Float,
    p2y: Float,
    p3x: Float,
    onPointsChange: (p1x: Float, p2x: Float, p2y: Float, p3x: Float) -> Unit,
    activePoint: CurvePoint?,
    onActivePointChange: (CurvePoint?) -> Unit,
    detectionRadius: Dp,
    maxHorizontalTime: Float,
    pointRadius: Float = 6.0f
) {

  val detectionRadiusPx = LocalDensity.current.run { detectionRadius.toPx() }

  Box(
      modifier =
          modifier
              .background(spectrogramBackground)
              .border(2.dp, NepTuneTheme.colors.accentPrimary)
              .padding(1.dp)
              .pointerInput(p1x, p2x, p2y, p3x) {
                detectDragGestures(
                    onDragStart = { offset ->
                      val widthPx = size.width.toFloat()
                      val heightPx = size.height.toFloat()
                      val rPx = detectionRadius.toPx()

                      val p1Offset = Offset(widthPx * p1x, 0f)
                      val p2Offset = Offset(widthPx * p2x, heightPx * p2y)
                      val p3Offset = Offset(widthPx * p3x, heightPx)

                      val determinedPoint =
                          when {
                            (offset - p1Offset).getDistance() < rPx -> CurvePoint.P1
                            (offset - p2Offset).getDistance() < rPx -> CurvePoint.P2
                            (offset - p3Offset).getDistance() < rPx -> CurvePoint.P3
                            else -> null
                          }
                      onActivePointChange(determinedPoint)
                    },
                    onDragEnd = { onActivePointChange(null) },
                    onDrag = { change, _ ->
                      change.consume()

                      if (activePoint != null) {
                        val widthPx = size.width.toFloat()
                        val heightPx = size.height.toFloat()

                        val currentX = change.position.x.coerceIn(0f, widthPx)
                        val currentY = change.position.y.coerceIn(0f, heightPx)

                        val newNormX = currentX / widthPx
                        val newNormY = currentY / heightPx

                        when (activePoint) {
                          CurvePoint.P1 -> onPointsChange(newNormX.coerceIn(0f, p2x), p2x, p2y, p3x)
                          CurvePoint.P2 ->
                              onPointsChange(p1x, newNormX.coerceIn(p1x, p3x), newNormY, p3x)
                          CurvePoint.P3 -> onPointsChange(p1x, p2x, p2y, newNormX.coerceIn(p2x, 1f))
                        }
                      }
                    })
              }) {
        val colorAccent = NepTuneTheme.colors.accentPrimary
        val lightTextColor = NepTuneTheme.colors.smallText
        Canvas(modifier = Modifier.fillMaxSize()) {
          val width = size.width
          val height = size.height
          val lineStrokeWidth = 2.dp.toPx()

          val totalSeconds = maxHorizontalTime.toInt().coerceAtLeast(1)
          val pixelsPerSecond = width / totalSeconds.toFloat()
          val gridColor = ADSR_GRID_COLOR
          val dashLength = 5.dp.toPx()

          var yPos = 0f
          while (yPos < height) {
            for (sec in 1..totalSeconds) {
              val xPos = sec.toFloat() * pixelsPerSecond

              drawLine(
                  color = gridColor,
                  start = Offset(xPos, yPos),
                  end = Offset(xPos, yPos + dashLength.coerceAtMost(height - yPos)),
                  strokeWidth = 1f)
            }
            yPos += dashLength * 2
          }

          val p1Draw = Offset(width * p1x, 0f)
          val p2Draw = Offset(width * p2x, height * p2y)
          val p3Draw = Offset(width * p3x, height)
          val startDraw = Offset(0f, height)

          drawLine(
              color = colorAccent, start = startDraw, end = p1Draw, strokeWidth = lineStrokeWidth)
          drawLine(color = colorAccent, start = p1Draw, end = p2Draw, strokeWidth = lineStrokeWidth)
          drawLine(color = colorAccent, start = p2Draw, end = p3Draw, strokeWidth = lineStrokeWidth)

          val pointsToDraw = listOf(p1Draw, p2Draw, p3Draw)
          val radiusPx = pointRadius.dp.toPx()
          val contourWidthPx = 2.dp.toPx()

          pointsToDraw.forEach { center ->
            drawCircle(color = lightTextColor, radius = radiusPx, center = center)

            drawCircle(
                color = PointColor,
                radius = radiusPx + contourWidthPx / 2,
                center = center,
                style = Stroke(width = contourWidthPx))
          }
        }
      }
}

@Composable
fun ADSRCurveEditor(
    modifier: Modifier,
    attack: Float,
    decay: Float,
    sustain: Float,
    release: Float,
    onAttackChange: (Float) -> Unit,
    onDecayChange: (Float) -> Unit,
    onSustainChange: (Float) -> Unit,
    onReleaseChange: (Float) -> Unit,
    maxTime: Float = ADSR_MAX_TIME,
    maxSustain: Float = ADSR_MAX_SUSTAIN
) {
  val visibleTimeScale = ADSR_DEFAULT_VIEW_TIME
  val scrollState = rememberScrollState()

  val curveMaxDuration = (attack + decay + release).coerceAtLeast(visibleTimeScale)
  val maxHorizontalTime = curveMaxDuration
  val canvasWidthRatio = (curveMaxDuration / visibleTimeScale).coerceAtLeast(1.0f)

  val p1xNorm = (attack / maxHorizontalTime).coerceIn(0f, 1f)
  val p2xNorm = ((attack + decay) / maxHorizontalTime).coerceIn(p1xNorm, 1f)
  val p2yNorm = 1.0f - (sustain / maxSustain).coerceIn(0f, 1f)
  val p3xNorm = ((attack + decay + release) / maxHorizontalTime).coerceIn(p2xNorm, 1f)

  var activePoint by remember { mutableStateOf<CurvePoint?>(null) }
  val detectionRadius = 25.dp

  val handlePointsChange: (p1x: Float, p2x: Float, p2y: Float, p3x: Float) -> Unit =
      { newP1X, newP2X, newP2Y, newP3X ->
        val newAttack = newP1X * maxHorizontalTime
        onAttackChange(newAttack.coerceIn(0f, maxTime))

        val newSustain = (1.0f - newP2Y) * maxSustain
        onSustainChange(newSustain.coerceIn(0f, maxSustain))

        val newDecayTime = newP2X * maxHorizontalTime - newAttack
        onDecayChange(newDecayTime.coerceIn(0f, maxTime))

        val releaseDuration = newP3X * maxHorizontalTime - (newAttack + newDecayTime)
        onReleaseChange(releaseDuration.coerceIn(0f, maxTime))
      }

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(160.dp)
              .padding(horizontal = 8.dp, vertical = 16.dp)
              .clip(MaterialTheme.shapes.small)
              .testTag(CURVE_EDITOR_SCROLL_CONTAINER)
              .horizontalScroll(scrollState, enabled = activePoint == null)) {
        CurveCanvas(
            modifier =
                Modifier.fillMaxHeight()
                    .width(
                        LocalDensity.current.run {
                          (LocalConfiguration.current.screenWidthDp.dp - 16.dp) * canvasWidthRatio
                        }),
            p1x = p1xNorm,
            p2x = p2xNorm,
            p2y = p2yNorm,
            p3x = p3xNorm,
            onPointsChange = handlePointsChange,
            activePoint = activePoint,
            onActivePointChange = { activePoint = it },
            detectionRadius = detectionRadius,
            maxHorizontalTime = maxHorizontalTime)
      }
}

private const val MIN_ANGLE = -135f
private const val MAX_ANGLE = 135f
private const val DEAD_ZONE_START = -90f
private const val DEAD_ZONE_END = 90f
private const val ACTIVE_RANGE = DEAD_ZONE_END - DEAD_ZONE_START

@Composable
fun UniversalKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    unit: KnobUnit,
    modifier: Modifier = Modifier
) {
  val accentColor = NepTuneTheme.colors.accentPrimary
  val knobColor = Color.White
  val lightText = NepTuneTheme.colors.smallText

  val displayValue =
      when (unit) {
        KnobUnit.SECONDS -> String.format(Locale.ROOT, "%.2fs", value)
        KnobUnit.PERCENT -> "${(value * 100).roundToInt()}%"
        KnobUnit.MILLISECONDS -> "${value.roundToInt()}ms"
        KnobUnit.NONE -> String.format(Locale.ROOT, "%.2f", value)
      }
  Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = displayValue, color = accentColor, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier =
            Modifier.size(70.dp)
                .clip(CircleShape)
                .background(KnobBackground)
                .border(2.dp, accentColor, CircleShape)
                .pointerInput(Unit) {
                  detectDragGestures(
                      onDrag = { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        var angleRad =
                            atan2(change.position.y - centerY, change.position.x - centerX)
                        angleRad += (PI / 2).toFloat()

                        var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

                        // wrap angle
                        if (angleDeg < MIN_ANGLE) angleDeg += 360f
                        if (angleDeg > MAX_ANGLE) angleDeg -= 360f

                        val newValue =
                            when {
                              angleDeg <= DEAD_ZONE_START -> minValue
                              angleDeg >= DEAD_ZONE_END -> maxValue
                              else -> {
                                val normalized = (angleDeg - DEAD_ZONE_START) / ACTIVE_RANGE
                                minValue + normalized * (maxValue - minValue)
                              }
                            }

                        onValueChange(newValue.coerceIn(minValue, maxValue))
                      })
                },
        contentAlignment = Alignment.Center) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 4.dp.toPx()
            val normalizedValue = (value - minValue) / (maxValue - minValue).coerceAtLeast(0.001f)
            val sweepAngle = 270f * normalizedValue
            val currentRotationDegrees = (normalizedValue * 270f) - 135f
            val finalAngle = currentRotationDegrees - 90f
            val adjustedAngleRad = Math.toRadians(finalAngle.toDouble())
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx()))
            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            val indicatorX = center.x + radius * cos(adjustedAngleRad).toFloat()
            val indicatorY = center.y + radius * sin(adjustedAngleRad).toFloat()
            drawLine(
                color = knobColor,
                start = center,
                end = Offset(indicatorX, indicatorY),
                strokeWidth = 1.5.dp.toPx())
            drawCircle(
                color = knobColor, radius = 3.dp.toPx(), center = Offset(indicatorX, indicatorY))
          }
        }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = label, color = lightText, fontSize = 16.sp)
  }
}

@Composable
fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
  val indicator = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
  val frameBorderColor = NepTuneTheme.colors.accentPrimary

  Column(
      modifier =
          Modifier.fillMaxWidth().border(2.dp, frameBorderColor).clip(MaterialTheme.shapes.small)) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .testTag("${title.replace(" ", "")}ClickableHeader")
                    .clickable(onClick = onToggle)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                  title,
                  color = NepTuneTheme.colors.smallText,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold)
              Icon(
                  imageVector = indicator,
                  contentDescription = if (isExpanded) "Collapse" else "Expand",
                  tint = NepTuneTheme.colors.accentPrimary)
            }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))) {
              content()
            }
      }
}

@Composable
fun EQFader(
    frequency: Int,
    gain: Float,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minGain: Float = EQ_GAIN_MIN,
    maxGain: Float = EQ_GAIN_MAX
) {
  val lightPurpleBlue = NepTuneTheme.colors.accentPrimary
  val white = NepTuneTheme.colors.smallText

  val range = maxGain - minGain

  var currentGain by remember { mutableFloatStateOf(gain) }

  LaunchedEffect(gain) { currentGain = gain }

  val dBValueText = String.format(Locale.ROOT, "%+d", gain.roundToInt())

  Column(
      modifier = modifier.height(200.dp).width(40.dp).padding(horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(dBValueText, color = white, fontSize = 10.sp, maxLines = 1)
          Text("dB", color = white, fontSize = 8.sp, maxLines = 1)
        }
        Box(
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(spectrogramBackground)
                    .testTag(SamplerTestTags.EQ_FADER_BOX_INPUT)
                    .pointerInput(minGain, maxGain) {
                      detectDragGestures(
                          onDragStart = {},
                          onDragEnd = {},
                          onDrag = { change, dragAmount ->
                            change.consume()

                            val heightPx = size.height.toFloat()

                            val gainChange = -dragAmount.y / heightPx * range

                            val newGain = (currentGain + gainChange).coerceIn(minGain, maxGain)
                            currentGain = newGain

                            onGainChange(newGain)
                          })
                    }) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasHeight = size.height

                val currentNormalizedPosition = (currentGain - minGain) / range

                val zeroDbY = canvasHeight / 2f

                val faderTopY = canvasHeight * (1f - currentNormalizedPosition)

                val faderHeightFromCenter = zeroDbY - faderTopY

                drawLine(
                    color = Color.Gray.copy(alpha = 0.7f),
                    start = Offset(0f, zeroDbY),
                    end = Offset(size.width, zeroDbY),
                    strokeWidth = 1.dp.toPx())

                drawRect(
                    color = lightPurpleBlue,
                    topLeft = Offset(x = 0f, y = if (currentGain >= 0) faderTopY else zeroDbY),
                    size = Size(width = size.width, height = abs(faderHeightFromCenter)))
              }
            }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "$frequency Hz",
            color = white,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1)
      }
}

@Composable
fun EQTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
  val eqFrequencies = listOf(60, 120, 250, 500, 1000, 2500, 5000, 10000)
  Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
      horizontalArrangement = Arrangement.SpaceEvenly) {
        eqFrequencies.forEachIndexed { index, frequency ->
          EQFader(
              frequency = frequency,
              gain = uiState.eqBands.getOrElse(index) { EQ_GAIN_DEFAULT },
              onGainChange = { newGain -> viewModel.updateEqBand(index, newGain) },
              modifier = if (index == 0) Modifier.testTag(FADER_60HZ_TAG) else Modifier)
        }
      }
}

@Composable
fun CompTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
  val compRatioFloat = uiState.compRatio.toFloat()

  Column(
      modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            UniversalKnob(
                label = "Threshold",
                value = uiState.compThreshold,
                onValueChange = viewModel::updateCompThreshold,
                minValue = COMP_GAIN_MIN,
                maxValue = COMP_GAIN_MAX,
                unit = KnobUnit.NONE,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_COMP_THRESHOLD))
            RatioInputField(
                label = "Ratio",
                ratio = uiState.compRatio,
                onValueChange = viewModel::updateCompRatio,
                minValue = 1,
                maxValue = 20,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.INPUT_COMP_RATIO))
            UniversalKnob(
                label = "Knee",
                value = uiState.compKnee,
                onValueChange = viewModel::updateCompKnee,
                minValue = 0.0f,
                maxValue = COMP_KNEE_MAX,
                unit = KnobUnit.NONE,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_COMP_KNEE))
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            UniversalKnob(
                label = "Gain",
                value = uiState.compGain,
                onValueChange = viewModel::updateCompGain,
                minValue = COMP_GAIN_MIN,
                maxValue = COMP_GAIN_MAX,
                unit = KnobUnit.NONE,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_COMP_GAIN))

            UniversalKnob(
                label = "Attack",
                value = uiState.compAttack,
                onValueChange = viewModel::updateCompAttack,
                minValue = 0.0f,
                maxValue = COMP_TIME_MAX,
                unit = KnobUnit.SECONDS,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_COMP_ATTACK))

            UniversalKnob(
                label = "Decay",
                value = uiState.compDecay,
                onValueChange = viewModel::updateCompDecay,
                minValue = 0.0f,
                maxValue = COMP_TIME_MAX,
                unit = KnobUnit.SECONDS,
                modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_COMP_DECAY))
          }
        }
        Spacer(modifier = Modifier.height(24.dp))

        CompressorCurve(
            modifier = Modifier.size(200.dp).testTag("compressorCurve"),
            threshold = uiState.compThreshold,
            ratio = compRatioFloat,
            knee = uiState.compKnee,
            compGain = uiState.compGain)
      }
}

@Composable
fun CompressorCurve(
    modifier: Modifier = Modifier,
    threshold: Float,
    ratio: Float,
    knee: Float,
    compGain: Float
) {
  val lightPurpleBlue = NepTuneTheme.colors.accentPrimary
  val minDb = COMP_GAIN_MIN
  val maxDb = COMP_GAIN_MAX
  val totalDbRange = maxDb - minDb

  Canvas(modifier = modifier.border(2.dp, lightPurpleBlue).background(spectrogramBackground)) {
    val width = size.width
    val height = size.height

    fun dbToX(db: Float): Float = ((db - minDb) / totalDbRange) * width
    fun dbToY(db: Float): Float = height - ((db - minDb) / totalDbRange) * height

    val compressionFactor = 1.0f / ratio

    val thresholdX = dbToX(threshold)
    val thresholdY = dbToY(threshold)

    val kneeStartDb = threshold - knee / 2f
    val kneeEndDb = threshold + knee / 2f

    val kneeStartX = dbToX(kneeStartDb)
    val kneeEndX = dbToX(kneeEndDb)

    val linePath = Path()

    drawLine(
        color = Color.Gray,
        start = Offset(dbToX(0f), 0f),
        end = Offset(dbToX(0f), height),
        strokeWidth = 1f)
    drawLine(
        color = Color.Gray,
        start = Offset(0f, dbToY(0f)),
        end = Offset(width, dbToY(0f)),
        strokeWidth = 1f)

    linePath.moveTo(dbToX(minDb), dbToY(minDb))

    for (i in 0..1000) {
      val inputDb = minDb + (i / 1000f) * totalDbRange
      var outputDb = inputDb

      if (inputDb > threshold) {
        outputDb = threshold + (inputDb - threshold) * compressionFactor
      }

      if (knee > 0f) {
        val kneeHalf = knee / 2f
        val kneeStartDb = threshold - kneeHalf
        val kneeEndDb = threshold + kneeHalf

        if (inputDb > kneeStartDb && inputDb < kneeEndDb) {

          val x = (inputDb - kneeStartDb) / knee
          val compressedOutputAtEnd = threshold + (kneeEndDb - threshold) * compressionFactor
          val uncompressedOutputAtStart = kneeStartDb

          outputDb =
              uncompressedOutputAtStart + (compressedOutputAtEnd - uncompressedOutputAtStart) * x
        } else if (inputDb >= kneeEndDb) {
          outputDb = threshold + (inputDb - threshold) * compressionFactor
        }
      }
      linePath.lineTo(dbToX(inputDb), dbToY(outputDb))
    }

    drawPath(linePath, color = lightPurpleBlue, style = Stroke(width = 3.dp.toPx()))

    drawCircle(color = Color.Red, center = Offset(thresholdX, thresholdY), radius = 6.dp.toPx())
  }
}

@Composable
fun TimeDisplay(playbackPosition: Float, audioDurationMillis: Int, modifier: Modifier = Modifier) {
  val currentPositionMillis = (playbackPosition * audioDurationMillis).roundToInt()
  val totalSeconds = audioDurationMillis / 1000
  val elapsedSeconds = currentPositionMillis / 1000
  val totalMilliseconds = (audioDurationMillis % 1000) / 10
  val elapsedMilliseconds = (currentPositionMillis % 1000) / 10

  val timeText =
      String.format(
          Locale.ROOT,
          "%02d.%02d / %02d.%02d s",
          elapsedSeconds,
          elapsedMilliseconds,
          totalSeconds,
          totalMilliseconds)

  Text(
      text = timeText,
      style = MaterialTheme.typography.bodySmall,
      color = NepTuneTheme.colors.smallText,
      modifier = modifier.testTag(SamplerTestTags.TIME_DISPLAY))
}

@Composable
fun InitialSetupDialog(viewModel: SamplerViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val context = NepTuneApplication.appContext

  AlertDialog(
      onDismissRequest = {},
      title = { Text("Setup required") },
      text = {
        Column(
            modifier = Modifier.fillMaxWidth().testTag(SamplerTestTags.INIT_SETUP_CONTAINER),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  "Define pitch & tempo for this sample",
                  style = MaterialTheme.typography.bodyLarge)

              PreviewAudioRow(
                  isPlaying = uiState.previewPlaying,
                  onPlay = { viewModel.playPreview(context) },
                  onStop = { viewModel.stopPreview() },
              )

              TempoRow(
                  tempo = uiState.inputTempo,
                  onTempoChange = viewModel::updateInputTempo,
                  onTapTempo = viewModel::tapTempo)
            }
      },
      confirmButton = {
        Button(
            onClick = viewModel::confirmInitialSetup,
            modifier = Modifier.testTag(SamplerTestTags.INIT_CONFIRM_BUTTON)) {
              Text("Confirm")
            }
      })
}

@Composable
fun PreviewAudioRow(isPlaying: Boolean, onPlay: () -> Unit, onStop: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { if (isPlaying) onStop() else onPlay() },
            modifier = Modifier.testTag(PREVIEW_PLAY_BUTTON)) {
              Text(if (isPlaying) "Stop" else "Play")
            }

        Text("Preview audio", style = MaterialTheme.typography.bodyMedium)
      }
}

@Composable
fun TempoRow(tempo: Int, onTempoChange: (Int) -> Unit, onTapTempo: () -> Unit) {
  var textState by remember(tempo) { mutableStateOf(TextFieldValue(tempo.toString())) }

  var userIsEditing by remember { mutableStateOf(false) }

  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = textState,
            onValueChange = { newValue ->
              textState = newValue
              newValue.text.toIntOrNull()?.let(onTempoChange)
              userIsEditing = true
            },
            label = { Text("Tempo BPM") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier =
                Modifier.weight(1f).testTag(SamplerTestTags.INIT_TEMPO_SELECTOR).onFocusChanged {
                    focusState ->
                  if (focusState.isFocused &&
                      textState.text == tempo.toString() &&
                      !userIsEditing) {
                    textState = TextFieldValue("", TextRange(0))
                  }
                })

        Button(
            onClick = onTapTempo, modifier = Modifier.testTag(SamplerTestTags.TAP_TEMPO_BUTTON)) {
              Text("Tap")
            }
      }
}

@Composable
fun PitchSelectorField(
    pitchNote: String,
    pitchOctave: Int,
    onPitchUp: () -> Unit,
    onPitchDown: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$pitchNote$pitchOctave", modifier = Modifier.weight(1f))
        Button(onClick = onPitchUp, modifier = Modifier.testTag("PITCH_UP_BUTTON")) { Text("↑") }
        Button(onClick = onPitchDown, modifier = Modifier.testTag("PITCH_DOWN_BUTTON")) {
          Text("↓")
        }
      }
}

@Composable
fun RatioInputField(
    label: String,
    ratio: Int,
    onValueChange: (Float) -> Unit,
    minValue: Int,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
  val accentColor = NepTuneTheme.colors.accentPrimary
  val lightText = NepTuneTheme.colors.smallText

  var text by remember { mutableStateOf(ratio.toString()) }

  LaunchedEffect(ratio) {
    if (text.toIntOrNull() != ratio) {
      text = ratio.toString()
    }
  }

  Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = "$text:1", color = accentColor, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = text,
        onValueChange = { newValue ->
          val filteredValue = newValue.filter { it.isDigit() }
          text = filteredValue
          filteredValue.toIntOrNull()?.let { intValue ->
            val coercedValue = intValue.coerceIn(minValue, maxValue).toFloat()
            onValueChange(coercedValue)
          }
        },
        modifier = Modifier.size(70.dp, 70.dp),
        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp, color = lightText),
        singleLine = true,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = accentColor.copy(alpha = 0.5f),
                cursorColor = accentColor,
                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

    Spacer(modifier = Modifier.height(8.dp))
    Text(text = label, color = lightText, fontSize = 16.sp)
  }
}

@Composable
fun SettingsDialog(viewModel: SamplerViewModel, zipFilePath: String?, onClose: () -> Unit) {
  var bpmText by remember { mutableStateOf("") }
  val uiState by viewModel.uiState.collectAsState()
  val context = NepTuneApplication.appContext

  // Keep bpmText in sync with the viewModel's inputTempo when dialog opens or state changes
  LaunchedEffect(uiState.inputTempo) {
    bpmText = if (uiState.inputTempo == 0) "" else uiState.inputTempo.toString()
  }

  AlertDialog(
      onDismissRequest = onClose,
      title = { Text("Settings") },
      text = {
        Column(modifier = Modifier.fillMaxWidth().testTag(SamplerTestTags.SETTINGS_DIALOG)) {
          Text("Settings placeholder — add your settings here.")
          Spacer(modifier = Modifier.height(8.dp))

          PreviewAudioRow(
              isPlaying = uiState.previewPlaying,
              onPlay = { viewModel.playPreview(context) },
              onStop = { viewModel.stopPreview() })

          Spacer(modifier = Modifier.height(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bpmText,
                onValueChange = { newValue -> bpmText = newValue.filter { it.isDigit() } },
                label = { Text("Initial BPM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(18.dp))

          val canReset = zipFilePath != null

          Button(
              onClick = {
                zipFilePath?.let {
                  viewModel.resetSampleAndSave(it)
                  onClose()
                }
              },
              enabled = canReset,
              modifier =
                  Modifier.fillMaxWidth().testTag(SamplerTestTags.SETTINGS_RESET_SAMPLE_BUTTON)) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset sample",
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset sample")
              }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              // parse and save into viewModel.inputTempo, input pitch, then close
              val newBpm = bpmText.toIntOrNull() ?: uiState.inputTempo
              viewModel.updateInputTempo(newBpm)
              // ensure input pitch state is persisted (updates already occur when user taps
              // up/down)
              viewModel.updateInputPitch(uiState.inputPitchNote, uiState.inputPitchOctave)
              onClose()
            },
            modifier = Modifier.testTag(SamplerTestTags.SETTINGS_CONFIRM_BUTTON)) {
              Text("Save & Close")
            }
      },
      dismissButton = {
        TextButton(
            onClick = onClose,
            modifier = Modifier.testTag(SamplerTestTags.SETTINGS_CANCEL_BUTTON)) {
              Text("Cancel")
            }
      })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(selectedTab: Int, onTabSelected: (Int) -> Unit, onClose: () -> Unit) {
  // swipe threshold in pixels (density-aware)
  val swipeThresholdPx = LocalDensity.current.run { 64.dp.toPx() }
  var dragAccum by remember { mutableFloatStateOf(0f) }
  fun onSwipe() = {
    // negative accumulation = swipe left (go to next)
    if (dragAccum < -swipeThresholdPx && selectedTab < HELP_DIALOG_TAB_COUNT - 1) {
      onTabSelected(selectedTab + 1)
    } else if (dragAccum > swipeThresholdPx && selectedTab > 0) {
      onTabSelected(selectedTab - 1)
    }
    dragAccum = 0f
  }

  Dialog(onDismissRequest = onClose) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NepTuneTheme.colors.background,
        modifier = Modifier.testTag(SamplerTestTags.HELP_DIALOG)) {
          // Entire content area is swipeable horizontally to switch pages
          Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier.padding(16.dp).widthIn(max = 520.dp).pointerInput(selectedTab) {
                      detectDragGestures(
                          onDragStart = { dragAccum = 0f },
                          onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccum += dragAmount.x
                          },
                          onDragEnd = onSwipe(),
                          onDragCancel = { dragAccum = 0f })
                    }) {
                  HelpDialogContent(selectedTab)

                  Spacer(modifier = Modifier.height(20.dp))
                  // Place the page indicator and Close button on the same row so dots are
                  // vertically aligned with the Close button
                  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HelpDialogNavigationArrows(selectedTab, onTabSelected)
                    PageIndicator(
                        currentPage = selectedTab,
                        onPageSelected = { idx -> onTabSelected(idx) },
                        modifier = Modifier.wrapContentWidth())
                  }
                  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = onClose) {
                      Text(stringResource(id = R.string.close))
                    }
                  }
                }
          }
        }
  }
}

@Composable
private fun HelpDialogNavigationArrows(selectedTab: Int, onTabSelected: (Int) -> Unit) {
  // Left navigation button (overlaid)
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val canGoPrev = selectedTab > 0
    IconButton(
        onClick = { if (canGoPrev) onTabSelected(selectedTab - 1) },
        enabled = canGoPrev,
        modifier =
            Modifier.align(Alignment.CenterStart)
                .absoluteOffset((-24).dp)
                .padding(start = 8.dp)
                .size(50.dp)
                .testTag(SamplerTestTags.HELP_NAV_LEFT)) {
          Surface(shape = CircleShape, color = Color.Transparent, tonalElevation = 2.dp) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous help page",
                tint = Color.White,
                modifier = Modifier.padding(6.dp))
          }
        }

    // Right navigation button (overlaid)
    val canGoNext = selectedTab < HELP_DIALOG_TAB_COUNT - 1
    IconButton(
        onClick = { if (canGoNext) onTabSelected(selectedTab + 1) },
        enabled = canGoNext,
        modifier =
            Modifier.align(Alignment.CenterEnd)
                .absoluteOffset(24.dp)
                .padding(end = 8.dp)
                .size(50.dp)
                .testTag(SamplerTestTags.HELP_NAV_RIGHT)) {
          Surface(shape = CircleShape, color = Color.Transparent, tonalElevation = 2.dp) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next help page",
                tint = Color.White,
                modifier = Modifier.padding(6.dp))
          }
        }
  }
}

@Composable
private fun HelpDialogContent(selectedTab: Int) {
  Text(
      stringResource(id = R.string.sampler_help_title),
      style = MaterialTheme.typography.titleLarge,
      color = NepTuneTheme.colors.smallText)
  Spacer(modifier = Modifier.height(12.dp))

  // Content pages (no visible tabs) - swipe left/right to change
  when (selectedTab) {
    0 -> HelpTabText(R.string.tab_overview, R.string.help_overview_text)
    1 -> HelpTabText(R.string.tab_controls, R.string.help_controls_text)
    2 -> HelpTabText(R.string.tab_adsr, R.string.help_adsr_text)
    3 -> HelpTabText(R.string.tab_reverb, R.string.help_reverb_text)
    4 -> HelpTabText(R.string.tab_equalizer, R.string.help_equalizer_text)
    5 -> HelpTabText(R.string.tab_compressor, R.string.help_compressor_text)
    else -> HelpTabText(R.string.tab_overview, R.string.help_overview_text)
  }
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.padding(vertical = 4.dp).testTag(SamplerTestTags.HELP_PAGE_INDICATOR),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically) {
        val dotSize = 8.dp
        val spacing = 8.dp
        for (i in 0 until HELP_DIALOG_TAB_COUNT) {
          val isSelected = i == currentPage
          Box(
              modifier =
                  Modifier.size(dotSize)
                      .clip(CircleShape)
                      .background(
                          if (isSelected) NepTuneTheme.colors.accentPrimary
                          else NepTuneTheme.colors.smallText.copy(alpha = 0.25f))
                      .clickable { onPageSelected(i) })
          if (i < HELP_DIALOG_TAB_COUNT - 1) Spacer(modifier = Modifier.width(spacing))
        }
      }
}

@Composable
private fun HelpTabText(titleStringResource: Int, textStringResource: Int) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        stringResource(id = titleStringResource),
        fontWeight = FontWeight.Bold,
        color = NepTuneTheme.colors.smallText)
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(id = textStringResource), color = NepTuneTheme.colors.smallText)
  }
}
