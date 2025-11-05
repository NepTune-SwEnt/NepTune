package com.neptune.neptune.media

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import java.io.File
import java.io.IOException

/**
 * NeptuneRecorder is an audio recorder that captures audio from the microphone and saves it to a
 * file.
 */
class NeptuneRecorder(private val context: Context) {

  var recorder: MediaRecorder? = null
  var isRecording = false
  private var outputFile: File? = null

  fun start(fileName: String = "rec_${System.currentTimeMillis()}.m4a", sampleRate: Int = 44100): File {
    if (sampleRate <= 0) throw IllegalArgumentException("Sample rate must be positive")
    if (isRecording) throw IllegalStateException("Already recording")
    val file = createOutputFile(fileName)
    recorder =
        MediaRecorder().apply {
          setAudioSource(MediaRecorder.AudioSource.MIC)
          setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
          setOutputFile(file.absolutePath)
          setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
          setAudioSamplingRate(sampleRate)
          setAudioEncodingBitRate(128000)
          try {
            prepare()
            start()
            isRecording = true
            outputFile = file
          } catch (e: IOException) {
            releaseSafely()
            throw e
          } catch (e: RuntimeException) {
            releaseSafely()
            throw IOException("Failed to start recorder", e)
          }
        }
    return file
  }

  fun stop(): File? {
    if (!isRecording) throw IllegalStateException("Not recording")
    return try {
      recorder?.apply {
        stop()
        release()
      }
      val recordedFile = outputFile
      outputFile = null

      recordedFile?.let { f ->
        try {
          MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), null, null)
        } catch (_: Exception) {}
      }

      recordedFile
    } catch (_: RuntimeException) {
      null
    } finally {
      recorder = null
      isRecording = false
    }
  }

  private fun releaseSafely() {
    try {
      recorder?.release()
    } catch (_: Exception) {}
    recorder = null
    isRecording = false
  }

  private fun createOutputFile(fileName: String): File {
    val dir = context.getExternalFilesDir("Records") ?: context.filesDir
    if (!dir.exists()) dir.mkdirs()
    return File(dir, fileName)
  }
}
