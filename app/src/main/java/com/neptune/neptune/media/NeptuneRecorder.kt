package com.neptune.neptune.media

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import com.neptune.neptune.data.StoragePaths
import java.io.File
import java.io.IOException

/**
 * NeptuneRecorder is an audio recorder that captures audio from the microphone and saves it to a
 * file.
 */
class NeptuneRecorder(private val context: Context, private val paths: StoragePaths) {

  var recorder: MediaRecorder? = null
  var isRecording = false
  private var outputFile: File? = null

  fun start(
      fileName: String = "rec_${System.currentTimeMillis()}.m4a",
      sampleRate: Int = 44100,
      audioSource: Int = MediaRecorder.AudioSource.UNPROCESSED
  ): File {
    require(sampleRate > 0) { "Sample rate must be positive" }
    require(isRecording) { "Already recording" }
    val file: File
    paths.recordWorkspace().let { dir ->
      if (!dir.exists()) dir.mkdirs()
      file = File(dir, fileName)
    }
    recorder =
        MediaRecorder().apply {
          // TODO: try different audio sources
          setAudioSource(audioSource)
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
    require(isRecording) { "Not recording" }
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
        } catch (_: Exception) {
          null
        }
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
    } catch (_: Exception) {

    }
    recorder = null
    isRecording = false
  }
}
