package com.neptune.neptune.ui.sampler

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamplerAudioTempoTest {

  private val mockAudioProcessor: SamplerViewModel.AudioProcessor = mock()
  private val mockUri: Uri = mock()
  private val initialSamples = floatArrayOf(0f, 0f)

  private fun executeProcessAudioTest(tempoRatio: Double, mockAudioData: AudioData) {
    val realViewModel = SamplerViewModel()
    val viewModelSpy = spy(realViewModel)

    doReturn(mockAudioData).`when`(viewModelSpy).decodeAudio(any())
    doReturn(initialSamples).`when`(mockAudioProcessor).timeStretch(any(), any())

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
        semitones = 0,
        tempoRatio = tempoRatio)
  }

  @Test
  fun processAudioAppliesTimeStretchWhenTempoRatioGreaterThanOne() {
    val tempoRatio = 1.25
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)

    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(tempoRatio, mockAudioData)

    verify(mockAudioProcessor, times(1)).timeStretch(eq(testSamples), eq(tempoRatio.toFloat()))
  }

  @Test
  fun processAudioAppliesTimeStretchWhenTempoRatioLowerThanOne() {
    val tempoRatio = 0.75
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)

    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(tempoRatio, mockAudioData)

    verify(mockAudioProcessor, times(1)).timeStretch(eq(testSamples), eq(tempoRatio.toFloat()))
  }

  @Test
  fun processAudioDoesNotApplyTimeStretchWhenTempoRatioIsOne() {
    val tempoRatio = 1.0
    val testSamples = floatArrayOf(0.1f, 0.2f, 0.3f)

    val mockAudioData: AudioData = Triple(first = testSamples, second = 44100, third = 1)

    executeProcessAudioTest(tempoRatio, mockAudioData)

    verify(mockAudioProcessor, never()).timeStretch(any(), any())
  }
}
