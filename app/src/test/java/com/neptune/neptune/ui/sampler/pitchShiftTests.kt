package com.neptune.neptune.ui.sampler

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner

typealias AudioData = Triple<FloatArray, Int, Int>

@RunWith(RobolectricTestRunner::class)
class SamplerAudioCoverageTest {

  private val mockAudioProcessor: SamplerViewModel.AudioProcessor = mock()
  private val mockUri: Uri = mock()
  private val initialSamples = floatArrayOf(0f, 0f)

  private fun executeProcessAudioTest(semitones: Int, mockAudioData: AudioData) {

    val realViewModel = SamplerViewModel()
    val viewModelSpy = spy(realViewModel)

    doReturn(mockAudioData).`when`(viewModelSpy).decodeAudio(any())
    doReturn(initialSamples).`when`(mockAudioProcessor).pitchShift(any(), any())

    viewModelSpy.processAudio(
        currentAudioUri = mockUri,
        eqBands = emptyList(),
        reverbWet = 0f,
        reverbSize = 0f,
        reverbWidth = 0f,
        reverbDepth = 0f,
        reverbPredelay = 0f,
        attack = 0f,
        decay = 0f,
        sustain = 0f,
        release = 0f,
        audioProcessor = mockAudioProcessor,
        semitones = semitones)
  }

  @Test
  fun processAudio_shiftsPitchWhenSemitonesIsPositive() {
    val semitones = 5
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)

    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(semitones, mockAudioData)

    verify(mockAudioProcessor, times(1)).pitchShift(eq(testSamples), eq(semitones))
  }

  @Test
  fun processAudio_shiftsPitchWhenSemitonesIsNegative() {
    val semitones = -3
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)
    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(semitones, mockAudioData)

    verify(mockAudioProcessor, times(1)).pitchShift(eq(testSamples), eq(semitones))
  }

  @Test
  fun processAudio_doesNotShiftPitchWhenSemitonesIsZero() {
    val semitones = 0
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)
    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(semitones, mockAudioData)

    verify(mockAudioProcessor, never()).pitchShift(any(), any())
  }
}
