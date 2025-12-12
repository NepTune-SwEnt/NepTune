package com.neptune.neptune.ui.sampler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.project.ProjectWriter
import io.mockk.*
import java.io.File
import junit.framework.Assert.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamplerViewModelSaveProjectTest {

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
  fun saveProjectDataSync_noAudioUri_returnsEarly_and_noWrite() {
    val vm = SamplerViewModel()
    vm._uiState.update { it.copy(currentAudioUri = null) }
    mockkConstructor(ProjectWriter::class)
    vm.saveProjectDataSync("/tmp/some.zip")
    verify(exactly = 0) { anyConstructed<ProjectWriter>().writeProject(any(), any(), any()) }
  }

  @Test
  fun saveProjectDataSync_audioFileMissing_returnsEarly_and_noWrite() {
    val vm = SamplerViewModel()
    val missing = File("/tmp/does_not_exist.wav")
    vm._uiState.update {
      it.copy(currentAudioUri = missing.toURI().toString().let { android.net.Uri.parse(it) })
    }
    mockkConstructor(ProjectWriter::class)
    vm.saveProjectDataSync("/tmp/some.zip")
    verify(exactly = 0) { anyConstructed<ProjectWriter>().writeProject(any(), any(), any()) }
  }

  @Test
  fun saveProjectDataSync_withValidAudio_callsProjectWriter() {
    val vm = SamplerViewModel()
    val tmpAudio = File.createTempFile("audio", ".wav")
    tmpAudio.writeBytes(ByteArray(2048))
    vm._uiState.update {
      it.copy(
          currentAudioUri = tmpAudio.toURI().toString().let { android.net.Uri.parse(it) },
          audioDurationMillis = 2000,
          attack = 0.1f,
          sustain = 0.5f,
          tempo = 123)
    }

    mockkConstructor(ProjectWriter::class)
    every { anyConstructed<ProjectWriter>().writeProject(any(), any(), any()) } just Runs

    val zip = File.createTempFile("dest", ".zip")
    vm.saveProjectDataSync(zip.absolutePath)

    verify { anyConstructed<ProjectWriter>().writeProject(zip, any(), listOf(tmpAudio)) }
  }
}
