package com.neptune.neptune.ui.sampler

import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.LightPurpleBlue
import com.neptune.neptune.ui.theme.LightSkyBlue
import com.neptune.neptune.ui.theme.White
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

val FrameBorderColor = LightPurpleBlue

const val SampleDurationMillis = 4000

const val ADSR_MAX_TIME = 5.0f
const val ADSR_DEFAULT_VIEW_TIME = 5.0f
const val ADSR_MAX_SUSTAIN = 1.0f

val ADSR_GRID_COLOR = Color.Gray.copy(alpha = 0.4f)

private const val EQ_GAIN_MIN = -20.0f
private const val EQ_GAIN_MAX = 20.0f





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplerScreen(viewModel: SamplerViewModel = viewModel(), onBack: () -> Unit = {}) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedItem by remember { mutableIntStateOf(2) }

  Scaffold(
      containerColor = DarkBlue1,
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
              .background(DarkBlue1)
              .testTag(SamplerTestTags.PLAYHEAD_CONTROLS)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = LightPurpleBlue,
                    modifier = Modifier.size(32.dp))
              }
              IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = LightPurpleBlue,
                    modifier = Modifier.size(32.dp))
              }
              Spacer(modifier = Modifier.weight(1f))

              PitchTempoSelector(
                  label = "Pitch",
                  value = pitch,
                  onIncrease = onIncreasePitch,
                  onDecrease = onDecreasePitch,
                  modifier =
                      Modifier.border(2.dp, FrameBorderColor, MaterialTheme.shapes.small)
                          .testTag(SamplerTestTags.PITCH_SELECTOR))
              Spacer(modifier = Modifier.width(8.dp))

              PitchTempoSelector(
                  label = tempo.toString(),
                  value = "",
                  onIncrease = { onTempoChange(tempo + 1) },
                  onDecrease = { onTempoChange(tempo - 1) },
                  modifier =
                      Modifier.border(2.dp, FrameBorderColor, MaterialTheme.shapes.small)
                          .testTag(SamplerTestTags.TEMPO_SELECTOR))
            }

        Spacer(modifier = Modifier.height(8.dp))

        WaveformDisplay(
            modifier =
                Modifier.fillMaxWidth()
                    .height(120.dp)
                    .border(2.dp, FrameBorderColor, MaterialTheme.shapes.small)
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
        Text(text = label, color = White, fontSize = 16.sp)
        if (value.isNotEmpty()) {
          Text(
              text = value,
              color = White,
              fontSize = 16.sp,
              modifier = Modifier.padding(start = 4.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              imageVector = Icons.Default.KeyboardArrowUp,
              contentDescription = "Increase",
              tint = LightPurpleBlue,
              modifier = Modifier.size(24.dp).clickable(onClick = onIncrease))
          Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = "Decrease",
              tint = LightPurpleBlue,
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
              color = LightSkyBlue,
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
          color = if (isSelected) LightPurpleBlue else White.copy(alpha = 0.6f),
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          modifier =
              Modifier.weight(1f)
                  .clip(MaterialTheme.shapes.small)
                  .clickable { onTabSelected(tab) }
                  .background(if (isSelected) White.copy(alpha = 0.1f) else Color.Transparent)
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
              .border(2.dp, FrameBorderColor)) {
        when (currentTab) {
          SamplerTab.BASICS -> BasicsTabContent(uiState, viewModel)
          SamplerTab.EQ -> EQTabContent(uiState, viewModel)
          SamplerTab.COMP ->
              Text(
                  "Compressor Settings...",
                  color = White,
                  modifier = Modifier.align(Alignment.Center))
          SamplerTab.TEMP ->
              Text(
                  "Tempo/Time Settings...",
                  color = White,
                  modifier = Modifier.align(Alignment.Center))
        }
      }
}

@Composable
fun BasicsTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
    var isADSrExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(top = 8.dp)
        .testTag(SamplerTestTags.TAB_BASICS_CONTENT)
    ) {
        ExpandableSection(
            title = "ADSR Envelope Controls",
            isExpanded = isADSrExpanded,
            onToggle = { isADSrExpanded = !isADSrExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ADSRCurveEditor(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    maxSustain = ADSR_MAX_SUSTAIN
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 8.dp)
                        .background(DarkBlue1),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ADSRKnob(
                        label = "Attack",
                        value = uiState.attack,
                        onValueChange = viewModel::updateAttack,
                        minValue = 0.0f,
                        maxValue = ADSR_MAX_TIME,
                        isTimeBased = true,
                        modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_ATTACK)
                    )
                    ADSRKnob(
                        label = "Decay",
                        value = uiState.decay,
                        onValueChange = viewModel::updateDecay,
                        minValue = 0.0f,
                        maxValue = ADSR_MAX_TIME,
                        isTimeBased = true,
                        modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_DECAY)
                    )
                    ADSRKnob(
                        label = "Sustain",
                        value = uiState.sustain,
                        onValueChange = viewModel::updateSustain,
                        minValue = 0.0f,
                        maxValue = ADSR_MAX_SUSTAIN, // CLÉ: MAX 1.0f pour le niveau
                        isTimeBased = false,
                        modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_SUSTAIN)
                    )
                    ADSRKnob(
                        label = "Release",
                        value = uiState.release,
                        onValueChange = viewModel::updateRelease,
                        minValue = 0.0f,
                        maxValue = ADSR_MAX_TIME,
                        isTimeBased = true,
                        modifier = Modifier.weight(1f).testTag(SamplerTestTags.KNOB_RELEASE)
                    )
                }
            }
        }
    }
}

val SpectrogramBackground = Color.Black.copy(alpha = 0.5f)
val PointColor = Color.Red


enum class CurvePoint(val id: Int) {
    P1(1), P2(2), P3(3)
}

@Composable
fun CurveCanvas(
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
    p1x: Float, p2x: Float, p2y: Float, p3x: Float,
    onPointsChange: (p1x: Float, p2x: Float, p2y: Float, p3x: Float) -> Unit,
    activePoint: CurvePoint?,
    onActivePointChange: (CurvePoint?) -> Unit,
    detectionRadius: Dp,
    maxHorizontalTime: Float,
    pointRadius: Float = 6.0f
) {
    val frameBorderColor = LightPurpleBlue
    val lightText = White
    val pointColor = PointColor
    val spectrogramBackground = Color.Black.copy(alpha = 0.5f)
    val detectionRadiusPx = LocalDensity.current.run { detectionRadius.toPx() }

    Box(
        modifier =
            modifier
                .background(spectrogramBackground)
                .border(2.dp, frameBorderColor)
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

                            val determinedPoint = when {
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
                                    CurvePoint.P2 -> onPointsChange(p1x, newNormX.coerceIn(p1x, p3x), newNormY, p3x)
                                    CurvePoint.P3 -> onPointsChange(p1x, p2x, p2y, newNormX.coerceIn(p2x, 1f))
                                    else -> {}
                                }
                            }
                        })
                }) {
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
                        strokeWidth = 1f
                    )
                }
                yPos += dashLength * 2
            }

            val p1Draw = Offset(width * p1x, 0f)
            val p2Draw = Offset(width * p2x, height * p2y)
            val p3Draw = Offset(width * p3x, height)
            val startDraw = Offset(0f, height)

            drawLine(color = frameBorderColor, start = startDraw, end = p1Draw, strokeWidth = lineStrokeWidth)
            drawLine(color = frameBorderColor, start = p1Draw, end = p2Draw, strokeWidth = lineStrokeWidth)
            drawLine(color = frameBorderColor, start = p2Draw, end = p3Draw, strokeWidth = lineStrokeWidth)

            val pointsToDraw = listOf(p1Draw, p2Draw, p3Draw)
            val radiusPx = pointRadius.dp.toPx()
            val contourWidthPx = 2.dp.toPx()

            pointsToDraw.forEach { center ->
                drawCircle(color = lightText, radius = radiusPx, center = center)

                drawCircle(
                    color = pointColor,
                    radius = radiusPx + contourWidthPx / 2,
                    center = center,
                    style = Stroke(width = contourWidthPx)
                )
            }
        }
    }
}

@Composable
fun ADSRCurveEditor(
    modifier: Modifier,
    attack: Float, decay: Float, sustain: Float, release: Float,
    onAttackChange: (Float) -> Unit, onDecayChange: (Float) -> Unit,
    onSustainChange: (Float) -> Unit, onReleaseChange: (Float) -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clip(MaterialTheme.shapes.small)
            .testTag("curveEditorScrollContainer")
            .horizontalScroll(scrollState, enabled = activePoint == null)
    ) {
        CurveCanvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(
                    LocalDensity.current.run { (LocalConfiguration.current.screenWidthDp.dp - 16.dp) * canvasWidthRatio }
                ),
            p1x = p1xNorm,
            p2x = p2xNorm,
            p2y = p2yNorm,
            p3x = p3xNorm,
            onPointsChange = handlePointsChange,
            activePoint = activePoint,
            onActivePointChange = { activePoint = it },
            detectionRadius = detectionRadius,
            maxHorizontalTime = maxHorizontalTime
        )
    }
}


@Composable
fun ADSRKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    isTimeBased: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = LightPurpleBlue
    val lightText = White

    val displayValue = if (isTimeBased) {
        "${String.format("%.2f", value)}s"
    } else {
        "${(value * 100).roundToInt()}%"
    }
  Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = displayValue, color = accentColor, fontSize = 14.sp)

    Spacer(modifier = Modifier.height(4.dp))

    Box(
        modifier =
            Modifier.size(70.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, LightPurpleBlue, CircleShape)
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
                color = LightPurpleBlue.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 1.5.dp.toPx()))

            drawArc(
                color = LightPurpleBlue,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

            val indicatorX = center.x + radius * cos(adjustedAngleRad).toFloat()
            val indicatorY = center.y + radius * sin(adjustedAngleRad).toFloat()

            drawLine(
                color = White,
                start = center,
                end = Offset(indicatorX, indicatorY),
                strokeWidth = 1.5.dp.toPx())
            drawCircle(color = White, radius = 3.dp.toPx(), center = Offset(indicatorX, indicatorY))
          }
        }

    Spacer(modifier = Modifier.height(8.dp))

    Text(text = label, color = White, fontSize = 16.sp)
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
    val frameBorderColor = LightPurpleBlue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, frameBorderColor)
            .clip(MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(imageVector = indicator, contentDescription = if (isExpanded) "Collapse" else "Expand", tint = LightPurpleBlue)
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
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
    val lightPurpleBlue = LightPurpleBlue
    val white = White

    val range = maxGain - minGain

    var currentGain by remember { mutableFloatStateOf(gain) }

    LaunchedEffect(gain) {
        currentGain = gain
    }

    val dBValueText = String.format("%+d", gain.roundToInt())

    Column(
        modifier = modifier.height(200.dp).width(40.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dBValueText, color = white, fontSize = 10.sp, maxLines = 1)
            Text("dB", color = white, fontSize = 8.sp, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(minGain, maxGain) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = { },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val heightPx = size.height.toFloat()

                            val gainChange = -dragAmount.y / heightPx * range

                            val newGain = (currentGain + gainChange).coerceIn(minGain, maxGain)
                            currentGain = newGain

                            onGainChange(newGain)
                        }
                    )
                }
        ) {
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
                    strokeWidth = 1.dp.toPx()
                )

                drawRect(
                    color = lightPurpleBlue,
                    topLeft = Offset(
                        x = 0f,
                        y = if (currentGain >= 0) faderTopY else zeroDbY
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width,
                        height = kotlin.math.abs(faderHeightFromCenter)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("${frequency} Hz", color = white, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun EQTabContent(uiState: SamplerUiState, viewModel: SamplerViewModel) {
    val eqFrequencies = listOf(60, 120, 250, 500, 1000, 2500, 5000, 10000)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        eqFrequencies.forEachIndexed { index, frequency ->
            EQFader(
                frequency = frequency,
                gain = uiState.eqBands.getOrElse(index) { EQ_GAIN_DEFAULT },
                onGainChange = { newGain -> viewModel.updateEqBand(index, newGain) }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSamplerScreen() {
  MaterialTheme { SamplerScreen(viewModel = SamplerViewModel()) }
}

