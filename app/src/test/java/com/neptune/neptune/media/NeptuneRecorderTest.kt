package com.neptune.neptune.media

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class NeptuneRecorderTest {

  private lateinit var context: Context
  private lateinit var recorder: NeptuneRecorder
  private lateinit var outputDir: File

  @Before
  fun setUp() {
    context = mockk(relaxed = true)
    outputDir = File("build/tmp/test/records")
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    every { context.getExternalFilesDir("Records") } returns outputDir

    // Mock MediaRecorder constructor to control its instances
    mockkConstructor(MediaRecorder::class)
    every { anyConstructed<MediaRecorder>().prepare() } returns Unit
    every { anyConstructed<MediaRecorder>().start() } returns Unit
    every { anyConstructed<MediaRecorder>().stop() } returns Unit
    every { anyConstructed<MediaRecorder>().release() } returns Unit

    // Mock static MediaScannerConnection.scanFile
    mockkStatic(MediaScannerConnection::class)
    every { MediaScannerConnection.scanFile(any(), any(), any(), any()) } returns Unit

    recorder = NeptuneRecorder(context)
  }

  @After
  fun tearDown() {
    unmockkAll()
    outputDir.deleteRecursively()
  }

  @Test
  fun startRecordingPreparesAndStartsMediaRecorder() {
    val file = recorder.start("test.m4a")

    assertThat(file.name).isEqualTo("test.m4a")
    assertThat(file.parentFile.absolutePath).isEqualTo(outputDir.absolutePath)
    verify { anyConstructed<MediaRecorder>().prepare() }
    verify { anyConstructed<MediaRecorder>().start() }
  }

  @Test
  fun stopRecordingStopsAndReleasesMediaRecorder() {
    val file = recorder.start("test.m4a")
    val stoppedFile = recorder.stop()

    assertThat(stoppedFile).isEqualTo(file)
    verify { anyConstructed<MediaRecorder>().stop() }
    verify { anyConstructed<MediaRecorder>().release() }
    verify { MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null) }
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
  fun startWithPrepareThrowingIOExceptionAlsoThrowsIOException() {
    every { anyConstructed<MediaRecorder>().prepare() } throws IOException("prepare failed")

    val e = assertThrows(IOException::class.java) { recorder.start("test.m4a") }
    assertThat(e.message).isEqualTo("prepare failed")

    // Verify that release is called to clean up
    assert(recorder.recorder == null)
  }

  @Test
  fun startWithStartThrowingRuntimeExceptionThrowsIOException() {
    every { anyConstructed<MediaRecorder>().start() } throws RuntimeException("start failed")

    val e = assertThrows(IOException::class.java) { recorder.start("test.m4a") }
    assertThat(e.message).isEqualTo("Failed to start recorder")
    assertThat(e.cause).isInstanceOf(RuntimeException::class.java)

    // Verify that release is called to clean up
    assert(recorder.recorder == null)
  }

  @Test
  fun stopWithRecorderThrowingRuntimeExceptionReturnsNull() {
    recorder.start("test.m4a")
    every { anyConstructed<MediaRecorder>().stop() } throws RuntimeException("stop failed")

    val file = recorder.stop()

    assertThat(file).isNull()

    // Verify state is reset, so another stop throws exception
    assertThrows(IllegalStateException::class.java) { recorder.stop() }
  }
}
