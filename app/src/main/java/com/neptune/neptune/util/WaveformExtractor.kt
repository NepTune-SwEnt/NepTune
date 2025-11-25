package com.neptune.neptune.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Define the interface for testing
interface AudioWaveformExtractor {
  suspend fun extractWaveform(context: Context, uri: Uri, samplesCount: Int = 100): List<Float>
}

/**
 * Utility class for decoding audio files. Changed from 'object' to 'class' to allow Constructor
 * Injection.
 *
 * @param ioDispatcher The dispatcher to run the heavy decoding on. Defaults to Dispatchers.IO.
 */
open class WaveformExtractor(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    AudioWaveformExtractor {

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
        val extractor = MediaExtractor()
        try {
          extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
          Log.e("WaveformExtractor", "Failed to set data source", e)
          return@withContext emptyList()
        }

        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
          val format = extractor.getTrackFormat(i)
          val mime = format.getString(MediaFormat.KEY_MIME)
          if (mime?.startsWith("audio/") == true) {
            trackIndex = i
            extractor.selectTrack(i)
            break
          }
        }

        if (trackIndex == -1) {
          extractor.release()
          return@withContext emptyList()
        }

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val decodedSamples = mutableListOf<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        try {
          while (!isEOS) {
            val inputIndex = codec.dequeueInputBuffer(5000)
            if (inputIndex >= 0) {
              val inputBuffer = codec.getInputBuffer(inputIndex)
              val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
              if (sampleSize < 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isEOS = true
              } else {
                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
              }
            }

            var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 5000)
            while (outputIndex >= 0) {
              val outputBuffer = codec.getOutputBuffer(outputIndex)
              if (outputBuffer != null && bufferInfo.size > 0) {
                val shortBuffer = outputBuffer.asShortBuffer()

                var sum = 0.0
                var count = 0

                while (shortBuffer.hasRemaining()) {
                  val sample = shortBuffer.get()
                  sum += abs(sample.toInt())
                  count++
                }

                if (count > 0) {
                  val avg = sum / count
                  decodedSamples.add((avg / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f))
                }
              }
              codec.releaseOutputBuffer(outputIndex, false)
              outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
          }
        } catch (e: Exception) {
          Log.e("WaveformExtractor", "Error decoding", e)
        } finally {
          try {
            codec.stop()
            codec.release()
            extractor.release()
          } catch (e: Exception) {
            Log.e("WaveformExtractor", "Error releasing resources", e)
          }
        }

        return@withContext resample(decodedSamples, samplesCount)
      }

  private fun resample(samples: List<Float>, targetCount: Int): List<Float> {
    if (samples.isEmpty()) return emptyList()
    if (samples.size <= targetCount) return samples

    val result = mutableListOf<Float>()
    val step = samples.size / targetCount.toFloat()

    for (i in 0 until targetCount) {
      val index = (i * step).toInt().coerceIn(samples.indices)
      result.add(samples[index])
    }
    return result
  }
}
