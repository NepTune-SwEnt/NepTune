package com.neptune.neptune.ui.sampler

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.ui.theme.NepTuneTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
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
}

const val SampleDurationMillis = 4000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplerScreen(viewModel: SamplerViewModel = viewModel(), onBack: () -> Unit = {}) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedItem by remember { mutableIntStateOf(2) }

  Scaffold(
      containerColor = NepTuneTheme.colors.background,
      modifier = Modifier.testTag(SamplerTestTags.SCREEN_CONTAINER),
  ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
      PlaybackAndWaveformControls(
          isPlaying = uiState.isPlaying,
          onTogglePlayPause = viewModel::togglePlayPause,
          onSave = viewModel::saveSampler,
          pitch = uiState.fullPitch,
          tempo = uiState.tempo,
          onPitchChange = viewModel::updatePitch,
          onTempoChange = viewModel::updateTempo,
          playbackPosition = uiState.playbackPosition,
          onPositionChange = viewModel::updatePlaybackPosition,
          onIncreasePitch = viewModel::increasePitch,
          onDecreasePitch = viewModel::decreasePitch)

      Spacer(modifier = Modifier.height(16.dp))

      SamplerTabs(currentTab = uiState.currentTab, onTabSelected = viewModel::selectTab)

      TabContent(currentTab = uiState.currentTab, uiState = uiState, viewModel = viewModel)
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
    onDecreasePitch: () -> Unit
) {

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
              IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = NepTuneTheme.colors.accentPrimary,
                    modifier = Modifier.size(32.dp))
              }
              Spacer(modifier = Modifier.weight(1f))

              PitchTempoSelector(
                  label = "Pitch",
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
            }

        Spacer(modifier = Modifier.height(8.dp))

        WaveformDisplay(
            modifier =
                Modifier.fillMaxWidth()
                    .height(120.dp)
                    .border(2.dp, NepTuneTheme.colors.accentPrimary, MaterialTheme.shapes.small)
                    .testTag(SamplerTestTags.WAVEFORM_DISPLAY),
            isPlaying = isPlaying,
            playbackPosition = playbackPosition,
            onPositionChange = onPositionChange)
      }
}

@Composable
fun PitchTempoSelector(
    label: String,
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

@Composable
fun WaveformDisplay(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    playbackPosition: Float = 0.0f,
    onPositionChange: (Float) -> Unit = {}
) {
  val soundWaveColor = NepTuneTheme.colors.soundWave
  val localDensity = LocalDensity.current
  val latestOnPositionChange = rememberUpdatedState(onPositionChange)

  val playbackPositionAnimatable = remember { Animatable(playbackPosition) }

  LaunchedEffect(playbackPosition) {
    if (!isPlaying) {
      playbackPositionAnimatable.snapTo(playbackPosition)
    }
  }

  LaunchedEffect(isPlaying) {
    if (isPlaying) {
      if (playbackPositionAnimatable.value < 1.0f) {

        playbackPositionAnimatable.animateTo(
            targetValue = 1.0f,
            animationSpec =
                tween(
                    durationMillis =
                        (SampleDurationMillis * (1.0f - playbackPositionAnimatable.value))
                            .roundToInt(),
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

  Canvas(
      modifier =
          modifier.background(Color.Black.copy(alpha = 0.5f)).padding(8.dp).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                  change.consume()

                  val rawX = change.position.x
                  val contentWidth = size.width - (2 * paddingPx)

                  val newPosition = (rawX - paddingPx) / contentWidth

                  latestOnPositionChange.value(newPosition.coerceIn(0f, 1f))
                })
          }) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val contentWidth = width - (2 * paddingPx)

        val barWidth = 2.dp.toPx()
        val numBars = 50
        val gapWidth = (contentWidth - (numBars * barWidth)) / (numBars - 1).coerceAtLeast(1)

        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(paddingPx, centerY),
            end = Offset(width - paddingPx, centerY),
            strokeWidth = 1f)

        for (i in 0 until numBars) {
          val simulatedAmplitude = (sin(i * 0.4f) * 0.3f + 0.5f).toFloat()
          val barHeight = simulatedAmplitude * (height - 2 * paddingPx) * 0.7f

          val startX = paddingPx + i * (barWidth + gapWidth) + barWidth / 2

          val startY = centerY - barHeight / 2
          val endY = centerY + barHeight / 2

          drawLine(
              color = soundWaveColor,
              start = Offset(startX, startY),
              end = Offset(startX, endY),
              strokeWidth = barWidth)
        }

        val xPosition = contentWidth * currentAnimPosition + paddingPx

        drawLine(
            brush = SolidColor(playheadColor),
            start = Offset(xPosition, paddingPx),
            end = Offset(xPosition, height - paddingPx),
            strokeWidth = playheadStrokeWidth)
      }
}

@Composable
fun SamplerTabs(currentTab: SamplerTab, onTabSelected: (SamplerTab) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().testTag(SamplerTestTags.SAMPLER_TABS)) {
    SamplerTab.values().forEach { tab ->
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
          SamplerTab.EQ ->
              Text(
                  "EQ Settings...",
                  color = NepTuneTheme.colors.smallText,
                  modifier = Modifier.align(Alignment.Center))
          SamplerTab.COMP ->
              Text(
                  "Compressor Settings...",
                  color = NepTuneTheme.colors.smallText,
                  modifier = Modifier.align(Alignment.Center))
          SamplerTab.TEMP ->
              Text(
                  "Tempo/Time Settings...",
                  color = NepTuneTheme.colors.smallText,
                  modifier = Modifier.align(Alignment.Center))
        }
      }
}

@Composable
fun BasicsTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .wrapContentHeight()
              .padding(vertical = 8.dp)
              .testTag(SamplerTestTags.TAB_BASICS_CONTENT),
      horizontalArrangement = Arrangement.SpaceAround) {
        ADSRKnob(
            label = "Attack",
            value = uiState.attack,
            onValueChange = viewModel::updateAttack,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_ATTACK))
        ADSRKnob(
            label = "Decay",
            value = uiState.decay,
            onValueChange = viewModel::updateDecay,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_DECAY))
        ADSRKnob(
            label = "Sustain",
            value = uiState.sustain,
            onValueChange = viewModel::updateSustain,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_SUSTAIN))
        ADSRKnob(
            label = "Release",
            value = uiState.release,
            onValueChange = viewModel::updateRelease,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_RELEASE))
      }
}

@Composable
fun ADSRKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
  val accentColor = NepTuneTheme.colors.accentPrimary
  val smallTextColor = NepTuneTheme.colors.smallText
  Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = "${String.format("%.2f", value)}s",
        color = NepTuneTheme.colors.accentPrimary,
        fontSize = 14.sp)

    Spacer(modifier = Modifier.height(4.dp))

    Box(
        modifier =
            Modifier.size(70.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, NepTuneTheme.colors.accentPrimary, CircleShape)
                .pointerInput(Unit) {
                  detectDragGestures(
                      onDrag = { change, _ ->
                        change.consume()

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f

                        var angleRadians: Float =
                            atan2(change.position.y - centerY, change.position.x - centerX)

                        angleRadians += (PI / 2).toFloat()
                        var angleDeg = Math.toDegrees(angleRadians.toDouble()).toFloat()
                        if (angleDeg < -135) {
                          angleDeg += 360
                        }
                        if (angleDeg > 225) {
                          angleDeg -= 360
                        }

                        val normalizedValue = ((angleDeg + 135f) / 270f).coerceIn(0f, 1f)

                        val newKnobValue = (normalizedValue * (maxValue - minValue)) + minValue

                        onValueChange(newKnobValue)
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
                color = smallTextColor,
                start = center,
                end = Offset(indicatorX, indicatorY),
                strokeWidth = 1.5.dp.toPx())
            drawCircle(
                color = smallTextColor,
                radius = 3.dp.toPx(),
                center = Offset(indicatorX, indicatorY))
          }
        }

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = label, color = NepTuneTheme.colors.smallText, fontSize = 16.sp)
  }
}

/*
@Preview(showBackground = true)
@Composable
fun PreviewSamplerScreen() {
  MaterialTheme { SamplerScreen(viewModel = SamplerViewModel()) }
}
*/
