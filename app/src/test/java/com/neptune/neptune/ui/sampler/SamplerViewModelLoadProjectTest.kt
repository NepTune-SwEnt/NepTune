package com.neptune.neptune.ui.sampler

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.project.AudioFileMetadata
import com.neptune.neptune.model.project.ParameterMetadata
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.project.SamplerProjectData
import io.mockk.*
import java.io.File
import java.io.IOException
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamplerViewModelLoadProjectTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    try {
      val f = NepTuneApplication::class.java.getDeclaredField("appContext")
      f.isAccessible = true
      f.set(null, context)
    } catch (_: Exception) {}
  }

  @After
  fun tearDown() {
    unmockkAll()
    clearAllMocks()
  }

  @Test
  fun loadProject_zipNotFound_setsProjectLoadError() = runBlocking {
    val vm = SamplerViewModel()
    val fakePath = "/no/such/file.zip"

    vm.loadProjectData(fakePath)

    Thread.sleep(200)

    assertEquals("ZIP not found", vm.uiState.value.projectLoadError)
    assertNull(vm.uiState.value.currentAudioUri)
    assertFalse(vm.uiState.value.showInitialSetupDialog)
  }

  @Test
  fun loadProject_metadataReadError_setsProjectLoadError() = runBlocking {
    val vm = SamplerViewModel()

    val extractor = mockk<ProjectExtractor>()
    every { extractor.extractMetadata(any()) } throws IOException("bad json")

    try {
      val f = SamplerViewModel::class.java.getDeclaredField("extractor")
      f.isAccessible = true
      f.set(vm, extractor)
    } catch (_: Exception) {}

    val temp = File.createTempFile("test", ".zip")
    vm.loadProjectData(temp.absolutePath)
    Thread.sleep(200)
    assertEquals("Can't read config.json", vm.uiState.value.projectLoadError)
    assertNull(vm.uiState.value.currentAudioUri)
  }

  @Test
  fun loadProject_noAudioFile_setsProjectLoadError() = runBlocking {
    val vm = SamplerViewModel()
    val extractor = mockk<ProjectExtractor>()
    val metadata = SamplerProjectData(audioFiles = emptyList(), parameters = emptyList())
    every { extractor.extractMetadata(any()) } returns metadata
    try {
      val f = SamplerViewModel::class.java.getDeclaredField("extractor")
      f.isAccessible = true
      f.set(vm, extractor)
    } catch (_: Exception) {}

    val temp = File.createTempFile("test_no_audio", ".zip")
    vm.loadProjectData(temp.absolutePath)
    Thread.sleep(200)
    assertEquals("No audio file in project", vm.uiState.value.projectLoadError)
    assertNull(vm.uiState.value.currentAudioUri)
  }

  @Test
  fun loadProject_audioExtractionError_setsProjectLoadError() = runBlocking {
    val vm = SamplerViewModel()
    val extractor = mockk<ProjectExtractor>()
    val audioMeta = AudioFileMetadata(name = "sound.wav", volume = 1.0f, durationSeconds = 1.0f)
    val metadata = SamplerProjectData(audioFiles = listOf(audioMeta), parameters = emptyList())
    every { extractor.extractMetadata(any()) } returns metadata
    every { extractor.extractAudioFile(any(), any(), any()) } throws IOException("extract fail")
    try {
      val f = SamplerViewModel::class.java.getDeclaredField("extractor")
      f.isAccessible = true
      f.set(vm, extractor)
    } catch (_: Exception) {}

    val temp = File.createTempFile("test_audio_error", ".zip")
    vm.loadProjectData(temp.absolutePath)
    Thread.sleep(200)
    assertEquals("Audio file extraction impossible", vm.uiState.value.projectLoadError)
    assertNull(vm.uiState.value.currentAudioUri)
  }

  @Test
  fun loadProject_needsSetup_setsShowInitialSetupDialog_and_currentAudioUri() = runBlocking {
    val vm = SamplerViewModel()
    val extractor = mockk<ProjectExtractor>()
    val audioMeta = AudioFileMetadata(name = "sound.wav", volume = 1.0f, durationSeconds = 1.0f)
    val metadata = SamplerProjectData(audioFiles = listOf(audioMeta), parameters = emptyList())
    every { extractor.extractMetadata(any()) } returns metadata
    val tmpAudio = File.createTempFile("sound", ".wav")
    tmpAudio.writeBytes(ByteArray(1024))
    every { extractor.extractAudioFile(any(), any(), audioMeta.name) } returns
        Uri.fromFile(tmpAudio)
    try {
      val f = SamplerViewModel::class.java.getDeclaredField("extractor")
      f.isAccessible = true
      f.set(vm, extractor)
    } catch (_: Exception) {}
    try {
      val mpField = SamplerViewModel::class.java.getDeclaredField("mediaPlayer")
      mpField.isAccessible = true
      val fakeMediaPlayer = mockk<com.neptune.neptune.media.NeptuneMediaPlayer>()
      every { fakeMediaPlayer.getDuration() } returns 1234
      mpField.set(vm, fakeMediaPlayer)
    } catch (_: Exception) {}

    val temp = File.createTempFile("test_needs_setup", ".zip")
    vm.loadProjectData(temp.absolutePath)
    Thread.sleep(200)

    assertTrue(vm.uiState.value.showInitialSetupDialog)
    assertNotNull(vm.uiState.value.currentAudioUri)
    assertNull(vm.uiState.value.projectLoadError)
  }

  @Test
  fun loadProject_withTempoAndPitch_loadsValues() = runBlocking {
    val vm = SamplerViewModel()
    val extractor = mockk<ProjectExtractor>()
    val audioMeta = AudioFileMetadata(name = "sound.wav", volume = 1.0f, durationSeconds = 1.0f)
    val params =
        listOf(ParameterMetadata("tempo", 138f, "global"), ParameterMetadata("pitch", 3f, "global"))
    val metadata = SamplerProjectData(audioFiles = listOf(audioMeta), parameters = params)
    every { extractor.extractMetadata(any()) } returns metadata
    val tmpAudio = File.createTempFile("sound", ".wav")
    tmpAudio.writeBytes(ByteArray(1024))
    every { extractor.extractAudioFile(any(), any(), audioMeta.name) } returns
        Uri.fromFile(tmpAudio)
    try {
      val f = SamplerViewModel::class.java.getDeclaredField("extractor")
      f.isAccessible = true
      f.set(vm, extractor)
    } catch (_: Exception) {}
    try {
      val mpField = SamplerViewModel::class.java.getDeclaredField("mediaPlayer")
      mpField.isAccessible = true
      val fakeMediaPlayer = mockk<com.neptune.neptune.media.NeptuneMediaPlayer>()
      every { fakeMediaPlayer.getDuration() } returns 2000
      mpField.set(vm, fakeMediaPlayer)
    } catch (_: Exception) {}

    val temp = File.createTempFile("test_full_load", ".zip")
    vm.loadProjectData(temp.absolutePath)
    Thread.sleep(200)

    assertFalse(vm.uiState.value.showInitialSetupDialog)
    assertEquals(138, vm.uiState.value.tempo)
    assertNotNull(vm.uiState.value.currentAudioUri)
    assertNull(vm.uiState.value.projectLoadError)
  }
}
