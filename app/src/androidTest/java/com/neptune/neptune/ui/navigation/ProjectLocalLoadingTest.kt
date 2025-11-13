package com.neptune.neptune.ui.navigation

import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.neptune.neptune.MainActivity
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.NeptuneApp
import com.neptune.neptune.media.NeptuneMediaPlayer
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryVarVar
import com.neptune.neptune.model.project.TotalProjectItemsRepository
import com.neptune.neptune.model.project.TotalProjectItemsRepositoryProvider
import com.neptune.neptune.ui.sampler.SamplerTestTags
import com.neptune.neptune.ui.sampler.SamplerViewModel
import com.neptune.neptune.ui.theme.SampleAppTheme
import java.io.File
import java.io.FileOutputStream
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private fun copyAssetToFile(assetPath: String, targetFile: File) {
  val context = NepTuneApplication.appContext
  context.assets.open(assetPath).use { input ->
    FileOutputStream(targetFile).use { output -> input.copyTo(output) }
  }
}

class FakeMediaPlayer : NeptuneMediaPlayer() {
  var fakeUri: Uri? = null
  var isPlayingState = false
  var preparedListener: (() -> Unit)? = null
  var currentPositionMillis = 0
  var durationMillis = 4000

  override fun isPlaying(): Boolean = isPlayingState

  override fun getCurrentUri(): Uri? = fakeUri

  override fun getCurrentPosition(): Int = currentPositionMillis

  override fun getDuration(): Int = durationMillis

  override fun play(uri: Uri) {
    fakeUri = uri
    isPlayingState = true
    preparedListener?.invoke()
  }

  override fun pause() {
    isPlayingState = false
  }

  override fun resume() {
    isPlayingState = true
  }

  override fun goTo(positionMillis: Int) {
    currentPositionMillis = positionMillis
  }

  override fun togglePlay(uri: Uri) {
    isPlayingState = !isPlayingState
  }

  override fun setOnPreparedListener(listener: () -> Unit) {
    preparedListener = listener
  }
}

class LocalProjectLoadingTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private lateinit var assetZipFile: File
  private var fakeRepository: TotalProjectItemsRepository = ProjectItemsRepositoryVarVar()

  private val ASSET_ZIP_PATH = "fakeProject.zip"
  private val TARGET_PROJECT_ID = "42"

  @Before
  fun setUp() {
    val context = NepTuneApplication.appContext
    assetZipFile = File(context.cacheDir, "test_${ASSET_ZIP_PATH}")
    context.assets.open(ASSET_ZIP_PATH).use { inputStream ->
      FileOutputStream(assetZipFile).use { outputStream -> inputStream.copyTo(outputStream) }
    }
    val absoluteZipPath = assetZipFile.absolutePath

    runBlocking {
      fakeRepository.addProject(
          ProjectItem(
              uid = TARGET_PROJECT_ID,
              name = "Test Project ZIP",
              projectFilePath = absoluteZipPath,
              lastUpdated = Timestamp(100, 0)))
      TotalProjectItemsRepositoryProvider.repository = fakeRepository
    }

    composeTestRule.activity.setContent {
      SampleAppTheme { NeptuneApp(startDestination = Screen.ProjectList.route) }
    }
  }

  @Test
  fun projectClick_loadsSamplerKnobsCorrectly() {
    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("COMP").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("4:1", substring = true).assertIsDisplayed()
  }

  @Test
  fun endToEndLoadProjectSetsCorrectAudioDuration() {
    composeTestRule.onNodeWithTag("project_$TARGET_PROJECT_ID").performClick()
    composeTestRule.mainClock.advanceTimeBy(500L)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(SamplerTestTags.TIME_DISPLAY)
        .assertTextContains("00.00 / 04.00 s", substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun loadProjectData_updatesUiStateWithRealExtractor() = runBlocking {
    val viewModel = SamplerViewModel()
    val context = NepTuneApplication.appContext
    val zipFile = File(context.cacheDir, "fakeProject.zip")
    copyAssetToFile("fakeProject.zip", zipFile)

    viewModel.loadProjectData(zipFile.absolutePath)
    delay(500)

    val state = viewModel.uiState.value
    assertNotNull(state.currentAudioUri)
    assertEquals(0.35f, state.attack, 0.001f)
    assertEquals(0.6f, state.sustain, 0.001f)
    assertEquals(4, state.compRatio)
  }
}

class SamplerViewModelTogglePlayTest {

  private lateinit var viewModel: SamplerViewModel
  private lateinit var fakePlayer: FakeMediaPlayer
  private val testUri = Uri.parse("file://fake_audio.wav")

  @Before
  fun setup() {
    fakePlayer = FakeMediaPlayer()
    viewModel = SamplerViewModel()
    val field = SamplerViewModel::class.java.getDeclaredField("mediaPlayer")
    field.isAccessible = true
    field.set(viewModel, fakePlayer)
    viewModel._uiState.update { it.copy(currentAudioUri = testUri, playbackPosition = 0f) }
  }

  @Test
  fun togglePlayPause_firstPlay_startsPlayingFromZero() {
    runBlocking {
      viewModel.togglePlayPause()
      val state = viewModel.uiState.value
      assertTrue("Should be playing", state.isPlaying)
      assertEquals(0f, state.playbackPosition)
      assertEquals(testUri, fakePlayer.getCurrentUri())
      assertTrue(fakePlayer.isPlaying())
    }
  }

  @Test
  fun togglePlayPause_whenPlaying_pauses() {
    runBlocking {
      fakePlayer.isPlayingState = true
      viewModel.togglePlayPause()
      val state = viewModel.uiState.value
      assertFalse("Should be paused", state.isPlaying)
      assertFalse(fakePlayer.isPlaying())
    }
  }

  @Test
  fun togglePlayPause_whenNearEnd_resetsPosition() = runBlocking {
    viewModel._uiState.update { it.copy(playbackPosition = 0.99f) }
    viewModel.togglePlayPause()
    val state = viewModel.uiState.value
    assertEquals(0f, state.playbackPosition)
  }
}
