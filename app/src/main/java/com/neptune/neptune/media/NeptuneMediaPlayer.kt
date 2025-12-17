package com.neptune.neptune.media

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import androidx.core.net.toUri
import com.neptune.neptune.NepTuneApplication
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class NeptuneMediaPlayer() {
  val context = NepTuneApplication.appContext

  private var mediaPlayer: MediaPlayer? = null
  var currentUri: Uri? = null
    private set

  private var onCompletionCallback: (() -> Unit)? = null

  open fun setOnCompletionListener(listener: () -> Unit) {
    this.onCompletionCallback = listener
    mediaPlayer?.setOnCompletionListener { player ->
      player.seekTo(0)
      onCompletionCallback?.invoke()
    }
  }

  private var onPreparedCallback: (() -> Unit)? = null

  open fun setOnPreparedListener(listener: () -> Unit) {
    this.onPreparedCallback = listener
  }

  /**
   * Play the audio from the given URI. If another audio is already playing, it will be stopped and
   * replaced.
   *
   * @param uri The URI of the audio to play.
   */
  open fun play(uri: Uri) {
    if (mediaPlayer == null) {
      mediaPlayer =
          MediaPlayer().apply {
            setDataSource(context, uri)
            setOnCompletionListener { player ->
              player.seekTo(0)
              onCompletionCallback?.invoke()
            }
            setOnPreparedListener {
              start()
              onPreparedCallback?.invoke()
            }
            prepareAsync()
          }
    } else {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.stop()
        }
        it.reset()
        it.setOnCompletionListener { player ->
          player.seekTo(0)
          onCompletionCallback?.invoke()
        }
        it.setOnPreparedListener {
          it.start()
          onPreparedCallback?.invoke()
        }
        it.setDataSource(context, uri)
        it.prepareAsync()
      }
    }
    currentUri = uri
  }

  /**
   * Toggle play/pause for the given URI. If the URI is currently playing, it will be paused. If
   * it's paused or a different URI is provided, it will start playing.
   *
   * @param uri The URI of the audio to play or pause.
   */
  open fun togglePlay(uri: Uri) {
    if (currentUri == uri) {
      if (isPlaying()) {
        pause()
      } else {
        resume()
      }
    } else {
      play(uri)
    }
  }

  /** Toggle between play and pause states. */
  fun togglePause() {
    if (isPlaying()) {
      pause()
    } else {
      resume()
    }
  }

  /** Pause the current audio playback. */
  open fun pause() {
    mediaPlayer?.let {
      if (it.isPlaying) {
        it.pause()
      }
    }
  }

  /** Resume the paused audio playback. */
  open fun resume() {
    mediaPlayer?.let {
      if (!it.isPlaying) {
        it.start()
      }
    }
  }

  /** Stop the audio playback and release resources. */
  open fun stop() {
    mediaPlayer?.let {
      if (it.isPlaying) {
        it.stop()
      }
      it.release()
      mediaPlayer = null
      currentUri = null
    }
  }

  /**
   * Check if the audio is currently playing.
   *
   * @return True if playing, false otherwise.
   */
  open fun isPlaying(): Boolean {
    return mediaPlayer?.isPlaying ?: false
  }

  /**
   * Go to a specific position in the audio track.
   *
   * @param position Position in milliseconds to seek to.
   */
  open fun goTo(position: Int) {
    mediaPlayer?.seekTo(position)
  }

  /**
   * Get the total duration of the audio in milliseconds.
   *
   * @return Duration in milliseconds, or -1 if no audio is loaded.
   */
  open fun getDuration(): Int {
    return mediaPlayer?.duration ?: -1
  }

  /**
   * Get the current playback position in milliseconds.
   *
   * @return Current position in milliseconds, or -1 if no audio is loaded.
   */
  open fun getCurrentPosition(): Int {
    return mediaPlayer?.currentPosition ?: -1
  }

  fun setVolume(level: Float) {
    val v = level.coerceIn(0f, 1f)
    mediaPlayer?.setVolume(v, v)
  }

  open fun stopWithFade(releaseMillis: Long) {
    val mp = mediaPlayer ?: return
    if (releaseMillis <= 0L) {
      releaseMillisNeg(mp)
    }

    CoroutineScope(Dispatchers.Main).launch { mediaPlayerCoroutine(releaseMillis, mp) }
  }

  private fun releaseMillisNeg(mp: MediaPlayer) {
    try {
      if (mp.isPlaying) mp.stop()
    } catch (e: Exception) {
      Log.e("NeptuneMediaPlayer", "Error stopping media player", e)
    }
    try {
      mp.release()
    } catch (e: Exception) {
      Log.e("NeptuneMediaPlayer", "Error releasing media player", e)
    }
    mediaPlayer = null
    currentUri = null
    return
  }

  private suspend fun mediaPlayerCoroutine(releaseMillis: Long, mp: MediaPlayer) {
    val steps = 20
    val stepDelay = (releaseMillis / steps).coerceAtLeast(5L)
    for (i in steps downTo 1) {
      val level = i.toFloat() / steps.toFloat()
      try {
        mp.setVolume(level, level)
      } catch (e: Exception) {
        Log.e("NeptuneMediaPlayer", "Error setting volume during fade", e)
      }
      delay(stepDelay)
    }
    try {
      mp.setVolume(0f, 0f)
      if (mp.isPlaying) mp.stop()
    } catch (e: Exception) {
      Log.e("NeptuneMediaPlayer", "Error stopping media player at fade end", e)
    }
    try {
      mp.release()
    } catch (e: Exception) {
      Log.e("NeptuneMediaPlayer", "Error releasing media player at fade end", e)
    }
    mediaPlayer = null
    currentUri = null
  }

  open fun forceStopAndRelease() {
    mediaPlayer?.let {
      try {
        if (it.isPlaying) it.stop()
      } catch (e: Exception) {
        Log.e("NeptuneMediaPlayer", "Error stopping media player in forceStopAndRelease", e)
      }
      try {
        it.release()
      } catch (e: Exception) {
        Log.e("NeptuneMediaPlayer", "Error releasing media player in forceStopAndRelease", e)
      }
    }
    mediaPlayer = null
    currentUri = null
  }

  /**
   * Helper function to map a sample ID to a URI for local raw resources.
   *
   * CAUTION: This function is used only for demonstration purposes with local raw resources and is
   * planned to be removed when integrating with real audio sources.
   *
   * @param sampleId The resource ID of the sample audio.
   * @return The URI pointing to the sample audio resource.
   */
  fun getUriFromSampleId(sampleId: String): Uri {
    val recordNumber = (abs(sampleId.hashCode()) % 2) + 1
    return "android.resource://${context.packageName}/raw/record$recordNumber".toUri()
  }
}

// Provide a CompositionLocal to access the NeptuneMediaPlayer instance throughout the app
val LocalMediaPlayer = compositionLocalOf<NeptuneMediaPlayer> { error("No MediaPlayer provided") }
