package com.neptune.neptune.media

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.util.Log
import com.neptune.neptune.data.StoragePaths
import java.io.File
import java.io.IOException

/**
 * NeptuneRecorder is an audio recorder that captures audio from the microphone and saves it to a
 * file.
 */
class NeptuneRecorder(private val context: Context, private val paths: StoragePaths) {

  private var recorder: MediaRecorder? = null
  private var _isRecording = false
  private var outputFile: File? = null

  val isRecording: Boolean
    get() = _isRecording

  /**
   * Starts recording audio and saves it to a file.
   *
   * @param fileName The name of the output file. Defaults to "rec_<timestamp>.m4a".
   * @param sampleRate The sample rate for recording. Defaults to 44100 Hz.
   * @param bitRate The bit rate for recording. Defaults to 128000 bps.
   * @param audioSource The audio source for recording. Defaults to
   *   MediaRecorder.AudioSource.UNPROCESSED.
   * @return The File object representing the recorded audio file.
   * @throws IOException If there is an error preparing, starting, or deleting the file after a
   *   failure.
   */
  fun start(
      fileName: String = "rec_${System.currentTimeMillis()}.m4a",
      sampleRate: Int = 44100,
      bitRate: Int = 128000,
      audioSource: Int = MediaRecorder.AudioSource.UNPROCESSED
  ): File {
    require(sampleRate > 0) { "Sample rate must be positive" }
    require(!isRecording) { "Already recording" }
    val file: File
    paths.recordWorkspace().let { dir ->
      if (!dir.exists()) dir.mkdirs()
      file = File(dir, fileName)
    }
    recorder =
        MediaRecorder().apply {
          setAudioSource(audioSource)
          setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
          setOutputFile(file.absolutePath)
          setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
          setAudioSamplingRate(sampleRate)
          setAudioEncodingBitRate(bitRate)
          try {
            prepare()
            start()
            _isRecording = true
            outputFile = file
          } catch (e: IOException) {
            releaseSafely()
            if (file.exists() && !file.delete()) {
              throw IOException("Failed to delete incomplete recording file: ${file.absolutePath}")
            }
            throw e
          } catch (e: RuntimeException) {
            releaseSafely()
            if (file.exists() && !file.delete()) {
              throw IOException("Failed to delete incomplete recording file: ${file.absolutePath}")
            }
            throw IOException("Failed to start recorder", e)
          }
        }
    return file
  }

  /**
   * Stops the recording and releases the recorder resources.
   *
   * @return The File object representing the recorded audio file, or null if stopping failed.
   * @throws IllegalStateException If the recorder is not currently recording.
   */
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
      _isRecording = false
    }
  }

  private fun releaseSafely() {
    try {
      recorder?.release()
    } catch (_: Exception) {
      Log.e("NeptuneRecorder", "Failed to release recorder")
    }

    recorder = null
    _isRecording = false
  }
}
