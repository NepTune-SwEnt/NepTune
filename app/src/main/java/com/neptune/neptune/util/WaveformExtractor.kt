package com.neptune.neptune.util

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Define the interface for testing
interface AudioWaveformExtractor {
  suspend fun extractWaveform(context: Context, uri: Uri, samplesCount: Int = 100): List<Float>

  suspend fun safeExtractWaveform(context: Context, uri: Uri, samplesCount: Int): List<Float>
}

/**
 * Utility class for decoding audio files. Changed from 'object' to 'class' to allow Constructor
 * Injection.
 *
 * @param ioDispatcher The dispatcher to run the heavy decoding on. Defaults to Dispatchers.IO.
 */
open class WaveformExtractor(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    AudioWaveformExtractor {

  private val waveformSemaphore = kotlinx.coroutines.sync.Semaphore(2)

  override suspend fun safeExtractWaveform(
      context: Context,
      uri: Uri,
      samplesCount: Int
  ): List<Float> {
    waveformSemaphore.acquire()
    return try {
      extractWaveform(context, uri, samplesCount)
    } finally {
      waveformSemaphore.release()
    }
  }

  /**
   * Decodes an audio file from a URI and returns a list of normalized amplitude samples.
   *
   * This function runs on the [Dispatchers.IO] context. It decodes the audio track, calculates the
   * average amplitude of buffer chunks, and resamples the result to fit the requested
   * [samplesCount].
   *
   * @param context The Android context used to resolve the URI.
   * @param uri The URI of the audio file to process.
   * @param samplesCount The desired number of data points in the resulting list. Defaults to 100.
   * @return A list of [Float] values ranging from 0.0 to 1.0 representing the waveform, or an empty
   *   list if processing fails.
   */
  override suspend fun extractWaveform(context: Context, uri: Uri, samplesCount: Int): List<Float> =
      withContext(ioDispatcher) {
        try {
          val decoded = AudioUtils.decodeAudioToPCM(uri) ?: return@withContext emptyList()

          val pcm = decoded.first
          val channelCount = decoded.third

          if (pcm.isEmpty() || samplesCount <= 0) {
            return@withContext emptyList()
          }

          val totalFrames = pcm.size / channelCount
          if (totalFrames <= 0) {
            return@withContext emptyList()
          }

          val framesPerBucket = totalFrames / samplesCount.toFloat()

          val buckets = FloatArray(samplesCount)
          val bucketHits = IntArray(samplesCount)

          for (frameIndex in 0 until totalFrames) {
            val bucketIndex = (frameIndex / framesPerBucket).toInt().coerceIn(0, samplesCount - 1)

            var frameSum = 0f
            for (ch in 0 until channelCount) {
              val sample = pcm[frameIndex * channelCount + ch]
              frameSum += kotlin.math.abs(sample)
            }

            val frameAmp = frameSum / channelCount
            buckets[bucketIndex] += frameAmp
            bucketHits[bucketIndex]++
          }

          return@withContext buckets.mapIndexed { i, sum ->
            if (bucketHits[i] > 0) {
              (sum / bucketHits[i]).coerceIn(0f, 1f)
            } else {
              0f
            }
          }
        } catch (e: Exception) {
          Log.e("WaveformExtractor", "Error extracting waveform", e)
          emptyList()
        }
      }
}
