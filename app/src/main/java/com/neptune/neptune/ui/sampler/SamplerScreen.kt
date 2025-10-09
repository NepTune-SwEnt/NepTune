package com.neptune.neptune.ui.sampler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Importe toutes les icônes 'filled'
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import com.neptune.neptune.R
import com.neptune.neptune.Sample
import com.neptune.neptune.ui.theme.DarkBlue1
import com.neptune.neptune.ui.theme.DarkBlue2
import com.neptune.neptune.ui.theme.DarkBlueGray
import com.neptune.neptune.ui.theme.LightPurpleBlue
import com.neptune.neptune.ui.theme.LightTurquoise
import androidx.compose.ui.text.font.Font
import com.neptune.neptune.ui.main.MainScreenTestTags


val DarkBackground = Color(0xFF1E1D3F)
val LightText = Color(0xFFE8E7FF)
val AccentColor = Color(0xFF8B88FF)
val WaveformColor = Color(0xFFC7C5FF)
val FrameBorderColor = AccentColor

/**
 * Composant principal de l'écran du Sampler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplerScreen(
    viewModel: SamplerViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableIntStateOf(2) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NepTune",
                        style = TextStyle(
                            fontSize = 45.sp,
                            fontFamily = FontFamily(Font(R.font.lily_script_one)),
                            fontWeight = FontWeight(149),
                            color = LightTurquoise,
                        ),
                        modifier =
                            Modifier.padding(vertical = 25.dp).testTag(MainScreenTestTags.APP_TITLE),
                        textAlign = TextAlign.Center
                    )
                },
                actions = {
                    IconButton(
                        onClick = { /*Does nothing for now, Todo: Add an action with the click*/ },
                        modifier =
                            Modifier.padding(vertical = 25.dp, horizontal = 17.dp)
                                .size(57.dp)
                                .testTag(MainScreenTestTags.PROFILE_BUTTON)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.profile),
                            contentDescription = "Profile",
                            tint = Color.Unspecified
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBlue1
                    ),
                modifier =
                    Modifier.fillMaxWidth().height(112.dp).testTag(MainScreenTestTags.TOP_APP_BAR)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.testTag(MainScreenTestTags.BOTTOM_NAVIGATION_BAR)) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(), thickness = 0.75.dp, color = LightTurquoise)
                NavigationBar(containerColor = DarkBlue1) {
                    // Item 0: Home
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(R.drawable.home_planet), contentDescription = "Home", modifier = Modifier.size(33.dp)) },
                        selected = selectedItem == 0,
                        onClick = { selectedItem = 0 },
                        modifier = Modifier.testTag(MainScreenTestTags.NAV_HOME),
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = LightPurpleBlue, unselectedIconColor = LightTurquoise, indicatorColor = DarkBlue2))
                    // Item 1: Search
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(33.dp)) },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 },
                        modifier = Modifier.testTag(MainScreenTestTags.NAV_SEARCH),
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = LightPurpleBlue, unselectedIconColor = LightTurquoise, indicatorColor = DarkBlue2))
                    // Item 2: Sampler
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(R.drawable.music_note), contentDescription = "Sampler", modifier = Modifier.size(33.dp)) },
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 },
                        modifier = Modifier.testTag(MainScreenTestTags.NAV_SAMPLER),
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = LightPurpleBlue, unselectedIconColor = LightTurquoise, indicatorColor = DarkBlue2))
                    // Item 3: New Post
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "New Post", modifier = Modifier.size(33.dp)) },
                        selected = selectedItem == 3,
                        onClick = { selectedItem = 3 },
                        modifier = Modifier.testTag(MainScreenTestTags.NAV_NEW_POST),
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = LightPurpleBlue, unselectedIconColor = LightTurquoise, indicatorColor = DarkBlue2))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
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
                onDecreasePitch = viewModel::decreasePitch
            )

            Spacer(modifier = Modifier.height(16.dp))

            SamplerTabs(
                currentTab = uiState.currentTab,
                onTabSelected = viewModel::selectTab
            )

            TabContent(
                currentTab = uiState.currentTab,
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}


@Composable
fun SamplerTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Retour",
            tint = LightText,
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onBack)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Sampler",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = LightText
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AccentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Sauvegarder",
                    tint = AccentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            PitchTempoSelector(
                label = "Pitch",
                value = pitch,
                onIncrease = onIncreasePitch,   // <-- utilise le callback passé
                onDecrease = onDecreasePitch,   // <-- utilise le callback passé
                modifier = Modifier.border(2.dp, FrameBorderColor, MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(8.dp))

            PitchTempoSelector(
                label = tempo.toString(),
                value = "",
                onIncrease = { onTempoChange(tempo + 1) },
                onDecrease = { onTempoChange(tempo - 1) },
                modifier = Modifier.border(2.dp, FrameBorderColor, MaterialTheme.shapes.small)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        WaveformDisplay(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(2.dp, FrameBorderColor, MaterialTheme.shapes.small),
            isPlaying = isPlaying,
            playbackPosition = playbackPosition,
            onPositionChange = onPositionChange
        )
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
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = LightText, fontSize = 16.sp)
        if (value.isNotEmpty()) {
            Text(text = value, color = LightText, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Augmenter",
                tint = AccentColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onIncrease)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Diminuer",
                tint = AccentColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDecrease)
            )
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
                val durationMillis = 4000

                playbackPositionAnimatable.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(
                        durationMillis = (durationMillis * (1.0f - playbackPositionAnimatable.value)).roundToInt(),
                        easing = LinearEasing
                    )
                )

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
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()

                        val rawX = change.position.x
                        val contentWidth = size.width - (2 * paddingPx)

                        val newPosition = (rawX - paddingPx) / contentWidth

                        latestOnPositionChange.value(newPosition.coerceIn(0f, 1f))
                    }
                )
            }
    ) {
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
            strokeWidth = 1f
        )

        for (i in 0 until numBars) {
            val simulatedAmplitude = (sin(i * 0.4f) * 0.3f + 0.5f).toFloat()
            val barHeight = simulatedAmplitude * (height - 2 * paddingPx) * 0.7f

            val startX = paddingPx + i * (barWidth + gapWidth) + barWidth / 2

            val startY = centerY - barHeight / 2
            val endY = centerY + barHeight / 2

            drawLine(
                color = WaveformColor,
                start = Offset(startX, startY),
                end = Offset(startX, endY),
                strokeWidth = barWidth
            )
        }

        val xPosition = contentWidth * currentAnimPosition + paddingPx

        drawLine(
            brush = SolidColor(playheadColor),
            start = Offset(xPosition, paddingPx),
            end = Offset(xPosition, height - paddingPx),
            strokeWidth = playheadStrokeWidth
        )
    }
}


@Composable
fun SamplerTabs(
    currentTab: SamplerTab,
    onTabSelected: (SamplerTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        SamplerTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            Text(
                text = tab.name.uppercase(),
                color = if (isSelected) AccentColor else LightText.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onTabSelected(tab) }
                    .background(if (isSelected) LightText.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun TabContent(
    currentTab: SamplerTab,
    uiState: SamplerUiState,
    viewModel: SamplerViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .border(2.dp, FrameBorderColor)
    ) {
        when (currentTab) {
            SamplerTab.BASICS -> BasicsTabContent(uiState, viewModel)
            SamplerTab.EQ -> Text("EQ Settings...", color = LightText, modifier = Modifier.align(Alignment.Center))
            SamplerTab.COMP -> Text("Compressor Settings...", color = LightText, modifier = Modifier.align(Alignment.Center))
            SamplerTab.TEMP -> Text("Tempo/Time Settings...", color = LightText, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun BasicsTabContent(
    uiState: SamplerUiState,
    viewModel: SamplerViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        ADSRKnob(
            label = "Attack",
            value = uiState.attack,
            onValueChange = viewModel::updateAttack,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f)
        )
        ADSRKnob(
            label = "Decay",
            value = uiState.decay,
            onValueChange = viewModel::updateDecay,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f)
        )
        ADSRKnob(
            label = "Sustain",
            value = uiState.sustain,
            onValueChange = viewModel::updateSustain,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f)
        )
        ADSRKnob(
            label = "Release",
            value = uiState.release,
            onValueChange = viewModel::updateRelease,
            minValue = 0.0f,
            maxValue = 5.0f,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Composant de bouton rotatif (Knob) interactif avec glissement vertical et rendu optimisé.
 */
@Composable
fun ADSRKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${String.format("%.2f", value)}s",
            color = AccentColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(2.dp, AccentColor, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()

                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            var angleRadians: Float = atan2(
                                change.position.y - centerY,
                                change.position.x - centerX
                            )

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
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 4.dp.toPx()

                val normalizedValue = (value - minValue) / (maxValue - minValue).coerceAtLeast(0.001f)
                val sweepAngle = 270f * normalizedValue

                val currentRotationDegrees = (normalizedValue * 270f) - 135f

                val finalAngle = currentRotationDegrees - 90f
                val adjustedAngleRad = Math.toRadians(finalAngle.toDouble())

                drawCircle(
                    color = AccentColor.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                drawArc(
                    color = AccentColor,
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                val indicatorX = center.x + radius * cos(adjustedAngleRad).toFloat()
                val indicatorY = center.y + radius * sin(adjustedAngleRad).toFloat()

                drawLine(
                    color = LightText,
                    start = center,
                    end = Offset(indicatorX, indicatorY),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(
                    color = LightText,
                    radius = 3.dp.toPx(),
                    center = Offset(indicatorX, indicatorY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = LightText,
            fontSize = 16.sp
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSamplerScreen() {
    MaterialTheme {
        SamplerScreen(viewModel = SamplerViewModel())
    }
}