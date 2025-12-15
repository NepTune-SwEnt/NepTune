package com.neptune.neptune.utils

import android.net.Uri
import android.util.Log
import com.neptune.neptune.util.AudioUtils
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioUtilsTest {

  @Before
  fun setup() {
    // Spy on the AudioUtils object to intercept internal calls
    mockkObject(AudioUtils)

    // Mock Log to avoid Android dependencies issues and verify error logging
    mockkStatic(Log::class)
    every { Log.e(any(), any(), any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun convertToWavReturnsTrueAndCallsEncodeWhenDecodingSucceeds() {
    val mockUri = mockk<Uri>()
    val mockFile = mockk<File>()
    val dummySamples = floatArrayOf(0.1f, -0.1f)
    val dummyRate = 44100
    val dummyChannels = 2

    // Mock the internal methods:
    // 1. decodeAudioToPCM returns valid data
    every { AudioUtils.decodeAudioToPCM(mockUri) } returns
        Triple(dummySamples, dummyRate, dummyChannels)
    // 2. encodePCMToWAV just runs without error
    every { AudioUtils.encodePCMToWAV(any(), any(), any(), any()) } just Runs

    // Act
    val result = AudioUtils.convertToWav(mockUri, mockFile)

    // Assert
    assertTrue(result)

    // Verify encode was called with the data from decode
    verify { AudioUtils.encodePCMToWAV(dummySamples, dummyRate, dummyChannels, mockFile) }
  }

  @Test
  fun convertToWavReturnsFalseWhenDecodingReturnsNull() {
    val mockUri = mockk<Uri>()
    val mockFile = mockk<File>()

    // Mock decoding to fail (return null)
    every { AudioUtils.decodeAudioToPCM(mockUri) } returns null

    // Act
    val result = AudioUtils.convertToWav(mockUri, mockFile)

    // Assert
    assertFalse(result)

    // Verify encode was NOT called
    verify(exactly = 0) { AudioUtils.encodePCMToWAV(any(), any(), any(), any()) }
  }

  @Test
  fun convertToWavReturnsFalseAndLogsErrorWhenExceptionOccurs() {
    val mockUri = mockk<Uri>()
    val mockFile = mockk<File>()
    val exception = RuntimeException("Decoding error")

    // Mock decoding to throw an exception
    every { AudioUtils.decodeAudioToPCM(mockUri) } throws exception

    // Act
    val result = AudioUtils.convertToWav(mockUri, mockFile)

    // Assert
    assertFalse(result)

    // Verify error was logged
    verify { Log.e("AudioUtils", "Conversion failed", exception) }
  }
}
