package com.neptune.neptune.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.data.StoragePaths
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeptuneRecorderTest {

  private lateinit var context: Context
  private lateinit var recorder: NeptuneRecorder
  private lateinit var outputDir: File

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    outputDir = File("build/tmp/test/records")
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    recorder = NeptuneRecorder(context, StoragePaths(context))
  }

  @Test
  fun startRecordingPreparesAndStartsMediaRecorder() {
    val file = recorder.start("test.m4a")

    assertThat(file.name).isEqualTo("test.m4a")
  }

  @Test
  fun stopRecordingStopsAndReleasesMediaRecorder() {
    val file = recorder.start("test.m4a")
    val stoppedFile = recorder.stop()
    assertThat(stoppedFile).isEqualTo(file)
  }

  @Test
  fun startRecordingTwiceThrowsIllegalStateException() {
    recorder.start("test.m4a")
    assertThrows(IllegalStateException::class.java) { recorder.start("test2.m4a") }
  }

  @Test
  fun stopRecordingWithoutStartingThrowsIllegalStateException() {
    assertThrows(IllegalStateException::class.java) { recorder.stop() }
  }

  @Test
  fun startWithStartThrowIllegalStateException() {
    recorder.start("test.m4a")
    val e = assertThrows(java.lang.IllegalStateException::class.java) { recorder.start("test.m4a") }
    assertThat(e.message).isEqualTo("Already recording")
  }

  @Test
  fun startWithNegativeSampleRateThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException::class.java) {
      recorder.start("test.m4a", sampleRate = -44100)
    }
  }
}
