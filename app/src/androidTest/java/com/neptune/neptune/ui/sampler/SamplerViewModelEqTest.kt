package com.neptune.neptune.ui.sampler

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SamplerViewModelEqTest {
  private lateinit var viewModel: SamplerViewModel

  @Before
  fun setup() {
    viewModel = SamplerViewModel()
  }

  @Test
  fun applyEQFilters_withZeroGains_returnsUnchangedSamples() {
    val sampleRate = 44100
    val original = FloatArray(2048) { i -> sin(2 * Math.PI * 440 * i / sampleRate).toFloat() }
    val eqBands = List(8) { 0.0f }

    val processed = viewModel.applyEQFilters(original, sampleRate, eqBands)
    var maxDiff = 0f
    for (i in original.indices) {
      maxDiff = maxOf(maxDiff, abs(original[i] - processed[i]))
    }
    assertTrue("Max diff should be < 1e-5 when gains are zero; got $maxDiff", maxDiff < 1e-5f)
  }

  @Test
  fun applyEQFilters_withGain_modifiesSamples() {
    val sampleRate = 44100
    val original = FloatArray(4096) { i -> sin(2 * Math.PI * 1000 * i / sampleRate).toFloat() }
    val eqBands = listOf(0f, 0f, 0f, 0f, 5f, 0f, 0f, 0f) // Boost near 1kHz

    val processed = viewModel.applyEQFilters(original, sampleRate, eqBands)
    var totalDiff = 0f
    for (i in original.indices) totalDiff += abs(original[i] - processed[i])
    val avgDiff = totalDiff / original.size
    assertTrue("Average diff should be > 1e-4 when gain applied; got $avgDiff", avgDiff > 1e-4f)
  }

  @Test
  fun encodePCMToWAV_writesValidFileAndHeader() {
    val sampleRate = 22050
    val channelCount = 1
    val samples = FloatArray(1000) { i -> sin(2 * Math.PI * 440 * i / sampleRate).toFloat() }
    val outFile =
        File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "wav_header_test.wav")
    if (outFile.exists()) outFile.delete()

    viewModel.encodePCMToWAV(samples, sampleRate, channelCount, outFile)
    assertTrue("WAV file should exist", outFile.exists())
    assertTrue("WAV file should be > 44 bytes", outFile.length() > 44)
    val bytes = outFile.readBytes()
    assertEquals('R'.code.toByte(), bytes[0])
    assertEquals('I'.code.toByte(), bytes[1])
    assertEquals('F'.code.toByte(), bytes[2])
    assertEquals('F'.code.toByte(), bytes[3])
    assertEquals('W'.code.toByte(), bytes[8])
    assertEquals('A'.code.toByte(), bytes[9])
    assertEquals('V'.code.toByte(), bytes[10])
    assertEquals('E'.code.toByte(), bytes[11])
  }

  @Test
  fun equalizeAudio_nullUri_keepsState() {
    val before = viewModel.uiState.value.currentAudioUri
    viewModel.equalizeAudio(null, viewModel.uiState.value.eqBands)
    assertEquals(before, viewModel.uiState.value.currentAudioUri)
  }

  @Test
  fun equalizeAudio_existingSource_createsEqualizedFile() {
    val sampleRate = 8000
    val channelCount = 1
    val samples = FloatArray(2000) { i -> sin(2 * Math.PI * 440 * i / sampleRate).toFloat() }
    val sourceFile =
        File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "source_eq.wav")
    if (sourceFile.exists()) sourceFile.delete()
    viewModel.encodePCMToWAV(samples, sampleRate, channelCount, sourceFile)

    val sourceUri = Uri.fromFile(sourceFile)
    viewModel.equalizeAudio(sourceUri, listOf(5f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
    val newUri = viewModel.uiState.value.currentAudioUri
    assertTrue("Equalized URI should not be null", newUri != null)
    assertTrue("Equalized file should exist", File(newUri!!.path!!).exists())
    assertTrue(
        "File name should contain _equalized", newUri.lastPathSegment!!.contains("_equalized"))
  }
}
