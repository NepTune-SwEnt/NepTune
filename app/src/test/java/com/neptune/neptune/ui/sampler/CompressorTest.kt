package com.neptune.neptune.ui.sampler

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class CompressorTest {

  private fun ampFromDb(db: Float): Float = 10f.pow(db / 20f)

  private fun dbFromAmp(a: Float): Float = 20f * log10(a.toDouble().toFloat().coerceAtLeast(1e-12f))

  @Test
  fun processEmptyInputReturnsEmpty() {
    val compressor =
        Compressor(
            sampleRate = 48000,
            thresholdDb = -12f,
            ratio = 4f,
            kneeDb = 0f,
            makeUpDb = 0f,
            attackSeconds = 0.01f,
            releaseSeconds = 0.1f)

    val input = floatArrayOf()
    val output = compressor.process(input)

    assertNotNull(output)
    assertEquals(0, output.size)
  }

  @Test
  fun belowThresholdHardKneeDoesNotChangeLevel() {
    val sampleRate = 48000
    val thresholdDb = -12f
    val inputDb = -24f // clearly below threshold
    val amp = ampFromDb(inputDb)

    val compressor =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = 4f,
            kneeDb = 0f, // hard knee
            makeUpDb = 0f,
            attackSeconds = 0.01f,
            releaseSeconds = 0.1f)

    val input = FloatArray(512) { amp }
    val output = compressor.process(input)

    // At steady state, level should be basically unchanged
    val outDb = dbFromAmp(abs(output.last()))
    assertEquals(inputDb, outDb, 0.5f) // 0.5 dB tolerance
  }

  @Test
  fun aboveThresholdHardKneeReducesLevelWithRatio() {
    val sampleRate = 48000
    val thresholdDb = -12f
    val ratio = 4f

    val inputDb = -4f // 8 dB above threshold
    val inputAmp = ampFromDb(inputDb)

    val expectedOutputDb = thresholdDb + (inputDb - thresholdDb) / ratio
    val expectedGainDb = expectedOutputDb - inputDb

    val compressor =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = ratio,
            kneeDb = 0f, // hard knee
            makeUpDb = 0f,
            attackSeconds = 0.001f, // very fast so we reach steady state
            releaseSeconds = 0.001f)

    val input = FloatArray(4096) { inputAmp }
    val output = compressor.process(input)

    val steadyAmp = abs(output.last())
    val steadyDb = dbFromAmp(steadyAmp)

    // Check that final level matches expected compressed level
    assertEquals(expectedOutputDb, steadyDb, 0.75f)
    // Also check that we actually reduced the level
    assertTrue("Output should be quieter than input", steadyDb < inputDb)
  }

  @Test
  fun softKneeBehavesSmoothlyAcrossRegions() {
    val sampleRate = 48000
    val thresholdDb = -12f
    val kneeDb = 6f
    val ratio = 4f

    val kneeHalf = kneeDb / 2f
    val kneeStartDb = thresholdDb - kneeHalf
    val kneeEndDb = thresholdDb + kneeHalf

    val compressor =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = ratio,
            kneeDb = kneeDb,
            makeUpDb = 0f,
            attackSeconds = 0.0001f,
            releaseSeconds = 0.0001f)

    // 1) Below knee region: should be effectively uncompressed
    run {
      val inputDb = kneeStartDb - 6f
      val amp = ampFromDb(inputDb)
      // below threshold: no gain change, so even first sample is fine
      val out = compressor.process(FloatArray(1024) { amp })
      val outDb = dbFromAmp(abs(out.last()))
      assertEquals("Below knee should be basically unchanged", inputDb, outDb, 0.5f)
    }

    // 2) Above knee region: use long buffer so envelope can reach steady state
    run {
      val inputDb = kneeEndDb + 6f
      val amp = ampFromDb(inputDb)

      val expectedFullCompDb = thresholdDb + (inputDb - thresholdDb) / ratio

      val out = compressor.process(FloatArray(4096) { amp })
      val outDb = dbFromAmp(abs(out.last()))

      assertEquals(
          "Above knee should match full compression at steady state",
          expectedFullCompDb,
          outDb,
          1.0f)
      // sanity: it really is quieter than input
      assertTrue(outDb < inputDb)
    }

    // 3) Inside knee region: also use long buffer & compare ranges
    run {
      val inputDb = thresholdDb // center of knee
      val amp = ampFromDb(inputDb)

      val out = compressor.process(FloatArray(4096) { amp })
      val outDb = dbFromAmp(abs(out.last()))

      val noCompAtKneeStart = kneeStartDb
      val fullCompAtKneeEnd = thresholdDb + (kneeEndDb - thresholdDb) / ratio

      // sanity for downward compressor
      assertTrue(fullCompAtKneeEnd > noCompAtKneeStart)

      assertTrue(
          "Soft knee should land between kneeStart (no comp) and kneeEnd (full comp) at steady state",
          outDb >= noCompAtKneeStart - 0.5f && // small tolerance
              outDb <= fullCompAtKneeEnd + 0.5f)

      // and it should still be quieter than the raw input
      assertTrue(outDb < inputDb)
    }
  }

  @Test
  fun makeupGainRaisesLevelBySpecifiedDbWhenNotCompressing() {
    val sampleRate = 48000
    val thresholdDb = 0f // threshold above our signal so no compression
    val inputDb = -18f
    val makeup = 6f

    val inputAmp = ampFromDb(inputDb)

    val compressorNoMakeup =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = 4f,
            kneeDb = 0f,
            makeUpDb = 0f,
            attackSeconds = 0.0001f,
            releaseSeconds = 0.0001f)
    val compressorWithMakeup =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = 4f,
            kneeDb = 0f,
            makeUpDb = makeup,
            attackSeconds = 0.0001f,
            releaseSeconds = 0.0001f)

    val input = FloatArray(1024) { inputAmp }

    val outNoMakeup = compressorNoMakeup.process(input)
    val outWithMakeup = compressorWithMakeup.process(input)

    val dbNoMakeup = dbFromAmp(abs(outNoMakeup.last()))
    val dbWithMakeup = dbFromAmp(abs(outWithMakeup.last()))

    val diff = dbWithMakeup - dbNoMakeup
    assertEquals("Makeup gain should add approximately its dB value", makeup, diff, 0.75f)
  }

  @Test
  fun zeroInputProducesZeroOutputAndNoNaN() {
    val compressor =
        Compressor(
            sampleRate = 48000,
            thresholdDb = -12f,
            ratio = 4f,
            kneeDb = 6f,
            makeUpDb = 0f,
            attackSeconds = 0.01f,
            releaseSeconds = 0.1f)

    val input = FloatArray(512) { 0f }
    val output = compressor.process(input)

    assertEquals(input.size, output.size)
    output.forEachIndexed { idx, sample ->
      assertFalse("Sample $idx should not be NaN", sample.isNaN())
      assertFalse(
          "Sample $idx should not be infinite",
          sample == Float.POSITIVE_INFINITY || sample == Float.NEGATIVE_INFINITY)
      // If input is all zeros, output should remain effectively zero
      assertEquals(0f, sample, 1e-6f)
    }
  }

  @Test
  fun attackAndReleaseUseDifferentTimeConstants() {
    val sampleRate = 1000 // keep numbers simple
    val thresholdDb = -20f
    val ratio = 4f

    val quietDb = -40f
    val loudDb = -4f

    val quietAmp = ampFromDb(quietDb)
    val loudAmp = ampFromDb(loudDb)

    val compressor =
        Compressor(
            sampleRate = sampleRate,
            thresholdDb = thresholdDb,
            ratio = ratio,
            kneeDb = 0f,
            makeUpDb = 0f,
            attackSeconds = 0.1f, // 100 ms
            releaseSeconds = 0.3f // 300 ms (slower)
            )

    // Signal:
    //  - 200 samples quiet
    //  - 400 samples loud
    //  - 400 samples quiet again
    val input =
        FloatArray(200 + 400 + 400) { idx ->
          when {
            idx < 200 -> quietAmp
            idx < 600 -> loudAmp
            else -> quietAmp
          }
        }

    val output = compressor.process(input)

    // During the loud section, we expect some compression:
    val loudOut = output.sliceArray(250 until 600) // mid-loud region
    val loudOutDb = dbFromAmp(loudOut.map { abs(it) }.maxOrNull() ?: 0f)
    assertTrue("Loud region should be compressed (quieter than input)", loudOutDb < loudDb)

    // After returning to quiet, release should be slower:
    val earlyReleaseDb = dbFromAmp(abs(output[610]))
    val lateReleaseDb = dbFromAmp(abs(output.last()))

    // As release happens, gain reduction should relax, so the quiet
    // samples should gradually approach the original quietDb level.
    assertTrue(
        "Release should be gradual: late quiet samples closer to original quiet level",
        abs(lateReleaseDb - quietDb) < abs(earlyReleaseDb - quietDb))
  }
}
