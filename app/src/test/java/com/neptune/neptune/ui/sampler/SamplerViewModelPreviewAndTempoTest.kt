package com.neptune.neptune.ui.sampler

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.NepTuneApplication
import io.mockk.*
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamplerViewModelPreviewAndTempoTest {

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
  fun playPreview_withUri_startsMediaPlayer_and_setsPreviewPlaying() {
    val vm = SamplerViewModel()
    val testFile = Uri.parse("file:///tmp/fake_audio.wav")
    vm._uiState.update { it.copy(currentAudioUri = testFile) }
    mockkConstructor(MediaPlayer::class)
    val mpSlot = slot<MediaPlayer>()
    every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any()) } just Runs
    every { anyConstructed<MediaPlayer>().prepare() } just Runs
    every { anyConstructed<MediaPlayer>().start() } just Runs
    every { anyConstructed<MediaPlayer>().setOnCompletionListener(any()) } just Runs
    vm.playPreview(context)
    verify { anyConstructed<MediaPlayer>().setDataSource(context, testFile) }
    verify { anyConstructed<MediaPlayer>().prepare() }
    verify { anyConstructed<MediaPlayer>().start() }
    verify { anyConstructed<MediaPlayer>().setOnCompletionListener(any()) }
    assertTrue(vm.uiState.value.previewPlaying)
  }

  @Test
  fun playPreview_withNoUri_doesNothing() {
    val vm = SamplerViewModel()
    vm._uiState.update { it.copy(currentAudioUri = null, previewPlaying = false) }
    mockkConstructor(MediaPlayer::class)
    vm.playPreview(context)
    verify(exactly = 0) { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any()) }
    assertFalse(vm.uiState.value.previewPlaying)
  }

  @Test
  fun stopPreview_stopsAndReleases_and_setsPreviewPlayingFalse() {
    val vm = SamplerViewModel()
    val testFile = Uri.parse("file:///tmp/fake_audio.wav")
    vm._uiState.update { it.copy(currentAudioUri = testFile) }
    mockkConstructor(MediaPlayer::class)
    every { anyConstructed<MediaPlayer>().setDataSource(any<Context>(), any()) } just Runs
    every { anyConstructed<MediaPlayer>().prepare() } just Runs
    every { anyConstructed<MediaPlayer>().start() } just Runs
    every { anyConstructed<MediaPlayer>().setOnCompletionListener(any()) } just Runs
    every { anyConstructed<MediaPlayer>().stop() } just Runs
    every { anyConstructed<MediaPlayer>().release() } just Runs
    vm.playPreview(context)
    assertTrue(vm.uiState.value.previewPlaying)
    vm.stopPreview()
    verify { anyConstructed<MediaPlayer>().stop() }
    verify { anyConstructed<MediaPlayer>().release() }
    assertFalse(vm.uiState.value.previewPlaying)
  }
}
