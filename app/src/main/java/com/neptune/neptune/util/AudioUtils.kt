package com.neptune.neptune.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.neptune.neptune.NepTuneApplication
import java.io.File
import kotlin.math.roundToInt

// This object was made using AI assistance.
object AudioUtils {
  fun convertToWav(sourceUri: Uri, outputFile: File): Boolean {
    return try {
      val (samples, sampleRate, channelCount) = decodeAudioToPCM(sourceUri) ?: return false
      encodePCMToWAV(samples, sampleRate, channelCount, outputFile)
      true
    } catch (e: Exception) {
      Log.e("AudioUtils", "Conversion failed", e)
      false
    }
  }

  /**
   * Encodes the processed float PCM samples back into a standard WAV file format. This is an
   * implementation of a basic 16-bit PCM WAV writer.
   */
  fun encodePCMToWAV(samples: FloatArray, sampleRate: Int, channelCount: Int, outputFile: File) {
    try {
      // convert to 16-bit PCM (interleaved samples assumed)
      val pcmData = ByteArray(samples.size * 2)
      for (i in samples.indices) {
        val s = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        pcmData[i * 2] = (s.toInt() and 0xFF).toByte()
        pcmData[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
      }

      outputFile.outputStream().use { out ->
        val bitsPerSample = 16
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = (channelCount * bitsPerSample / 8)
        val dataSize = pcmData.size

        out.write("RIFF".toByteArray())
        out.write(intToBytes(36 + dataSize))
        out.write("WAVE".toByteArray())

        out.write("fmt ".toByteArray())
        out.write(intToBytes(16))
        out.write(shortToBytes(1)) // PCM
        out.write(shortToBytes(channelCount.toShort()))
        out.write(intToBytes(sampleRate))
        out.write(intToBytes(byteRate))
        out.write(shortToBytes(blockAlign.toShort()))
        out.write(shortToBytes(bitsPerSample.toShort()))

        out.write("data".toByteArray())
        out.write(intToBytes(dataSize))
        out.write(pcmData)
      }
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error encoding WAV: ${e.message}", e)
      throw e
    }
  }

  /**
   * function to decode audio files (MP3/WAV) into raw PCM float samples (normalized -1.0 to 1.0).
   * Uses Android's MediaCodec and MediaExtractor for low-level decoding.
   */
  fun decodeAudioToPCM(uri: Uri): Triple<FloatArray, Int, Int>? {
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(NepTuneApplication.appContext, uri, null)

      // Find the first audio track
      var trackIndex = -1
      var audioTrackFound = false
      var i = 0
      while (i < extractor.trackCount && !audioTrackFound) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME)

        if (mime?.startsWith("audio/") == true) {
          trackIndex = i
          extractor.selectTrack(i)
          audioTrackFound = true
        }
        i++
      }

      if (trackIndex == -1) {
        Log.e("SamplerViewModel", "No audio track found")
        return null
      }

      // Setup MediaCodec decoder
      val format = extractor.getTrackFormat(trackIndex)
      val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      val mime = format.getString(MediaFormat.KEY_MIME)!!

      val codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val allSamples = mutableListOf<Float>()
      val bufferInfo = MediaCodec.BufferInfo()
      var isEOS = false // End of Stream flag

      while (!isEOS) {
        // Feed input data to the codec
        val inputIndex = codec.dequeueInputBuffer(10000)
        if (inputIndex >= 0) {
          val inputBuffer = codec.getInputBuffer(inputIndex)!!
          val sampleSize = extractor.readSampleData(inputBuffer, 0)
          if (sampleSize < 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            isEOS = true
          } else {
            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
            extractor.advance()
          }
        }

        // Receive output data from the codec (decoded PCM)
        var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
        while (outputIndex >= 0) {
          val outputBuffer = codec.getOutputBuffer(outputIndex)!!
          val shortBuffer = outputBuffer.asShortBuffer()
          val chunk = ShortArray(shortBuffer.remaining())
          shortBuffer.get(chunk)

          // Convert PCM16 (Short.MAX_VALUE range) to float (-1.0 to 1.0)
          for (sample in chunk) {
            allSamples.add(sample.toFloat() / Short.MAX_VALUE)
          }

          codec.releaseOutputBuffer(outputIndex, false)
          outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
        }
      }

      // Cleanup
      codec.stop()
      codec.release()
      extractor.release()

      return Triple(allSamples.toFloatArray(), sampleRate, channelCount)
    } catch (e: Exception) {
      Log.e("SamplerViewModel", "Error decoding audio: ${e.message}", e)
      // Ensure extractor is released if error occurs before cleanup block
      extractor.release()
      return null
    }
  }

  internal fun intToBytes(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte())
  }

  internal fun shortToBytes(value: Short): ByteArray {
    return byteArrayOf((value.toInt() and 0xFF).toByte(), ((value.toInt() shr 8) and 0xFF).toByte())
  }
}
