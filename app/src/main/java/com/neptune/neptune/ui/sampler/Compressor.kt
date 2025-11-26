package com.neptune.neptune.ui.sampler

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/**
 * Offline, feed-forward compressor working on PCM float samples in [-1, 1].
 *
 * Parameters:
 * - thresholdDb: threshold in dBFS above which compression starts
 * - ratio: compression ratio (e.g. 4.0f for 4:1)
 * - kneeDb: soft knee width in dB (0 = hard knee)
 * - makeupGainDb: makeup gain applied after compression in dB
 * - attackSeconds: attack time (how fast gain reduction kicks in)
 * - releaseSeconds: release time (how fast gain recovers) written with help from ChatGPT
 */
internal class Compressor(
    private val sampleRate: Int,
    private val thresholdDb: Float,
    private val ratio: Float,
    private val kneeDb: Float,
    private val makeUpDb: Float,
    attackSeconds: Float,
    releaseSeconds: Float
) {
  private val attackCoeff: Float
  private val releaseCoeff: Float
  private val compressionFactor: Float
  private var knee: Float

  companion object {
    private const val EPS = 1e-5f
    private const val MIN_DB = -100f
  }

  init {
    val safeAttack = max(attackSeconds, 0.0001f)
    val safeRelease = max(releaseSeconds, 0.0001f)
    // smooth coefficients for the attack and release
    attackCoeff = exp((-1.0 / (sampleRate * safeAttack)).toFloat())
    releaseCoeff = exp((-1.0 / (sampleRate * safeRelease)).toFloat())
    compressionFactor = 1f / ratio.coerceAtLeast(1f)
    knee = kneeDb.coerceAtLeast(0f)
  }

  fun ampToDb(x: Float): Float {
    val absX = abs(x).coerceAtLeast(EPS)
    return (20f * log10(absX)).coerceAtLeast(MIN_DB)
  }

  fun dbToAmp(db: Float): Float {
    return 10f.pow(db / 20f)
  }

  /** Map input sample array to compressed samples */
  fun process(input: FloatArray): FloatArray {
    if (input.isEmpty()) return input
    val output = FloatArray(input.size)
    var currentGainDb = 0f

    for (i in input.indices) {
      val x = input[i]
      // the audio sample level before compression
      val inputDb = ampToDb(x)
      // compressed level we want based on threshold, ration, knee
      val outputDb = mapInputDbToOutputDb(inputDb)
      // required gain in dB to go from inputDb to outputDb (+makeup)
      val targetGainDb = (outputDb - inputDb) + makeUpDb

      // Attack when gain needs to drop (more negative)
      // Release when gain can rise (more positive)
      currentGainDb =
          if (currentGainDb > targetGainDb) {
            attackCoeff * currentGainDb + (1 - attackCoeff) * targetGainDb
          } else {
            releaseCoeff * currentGainDb + (1 - releaseCoeff) * targetGainDb
          }
      val linearGain = dbToAmp(currentGainDb)
      output[i] = x * linearGain
    }
    return output
  }

  private fun mapInputDbToOutputDb(inputDb: Float): Float {
    // hard knee case
    if (knee <= 0f) {
      return if (inputDb > thresholdDb) {
        thresholdDb + (inputDb - thresholdDb) * compressionFactor
      } else {
        inputDb
      }
    }

    val kneeHalf = knee / 2f
    val kneeStartDb = thresholdDb - kneeHalf
    val kneeEndDb = thresholdDb + kneeHalf

    return when {
      inputDb <= kneeStartDb -> {
        // Below knee: no compression
        inputDb
      }
      inputDb >= kneeEndDb -> {
        // Above knee: full compression
        thresholdDb + (inputDb - thresholdDb) * compressionFactor
      }
      else -> {
        // In knee: soft interpolation
        val x = (inputDb - kneeStartDb) / knee
        val compressedAtEnd = thresholdDb + (kneeEndDb - thresholdDb) * compressionFactor
        val uncompressedAtStart = kneeStartDb
        uncompressedAtStart + (compressedAtEnd - uncompressedAtStart) * x
      }
    }
  }
}
