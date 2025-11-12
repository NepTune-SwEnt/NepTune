package com.neptune.neptune.media

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import androidx.core.net.toUri
import com.neptune.neptune.NepTuneApplication

class NeptuneMediaPlayer() {
  val context = NepTuneApplication.appContext

  private var mediaPlayer: MediaPlayer? = null
  private var currentUri: Uri? = null

  private var onCompletionCallback: (() -> Unit)? = null

  fun setOnCompletionListener(listener: () -> Unit) {
    this.onCompletionCallback = listener
  }

  private var onPreparedCallback: (() -> Unit)? = null

  fun setOnPreparedListener(listener: () -> Unit) {
    this.onPreparedCallback = listener
  }

  /**
   * Play the audio from the given URI. If another audio is already playing, it will be stopped and
   * replaced.
   *
   * @param uri The URI of the audio to play.
   */
  fun play(uri: Uri) {
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
  fun togglePlay(uri: Uri) {
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
  fun pause() {
    mediaPlayer?.let {
      if (it.isPlaying) {
        it.pause()
      }
    }
  }

  /** Resume the paused audio playback. */
  fun resume() {
    mediaPlayer?.let {
      if (!it.isPlaying) {
        it.start()
      }
    }
  }

  /** Stop the audio playback and release resources. */
  fun stop() {
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
  fun isPlaying(): Boolean {
    return mediaPlayer?.isPlaying ?: false
  }

  /**
   * Go to a specific position in the audio track.
   *
   * @param position Position in milliseconds to seek to.
   */
  fun goTo(position: Int) {
    mediaPlayer?.seekTo(position)
  }

  /**
   * Get the total duration of the audio in milliseconds.
   *
   * @return Duration in milliseconds, or -1 if no audio is loaded.
   */
  fun getDuration(): Int {
    return mediaPlayer?.duration ?: -1
  }

  /**
   * Get the current playback position in milliseconds.
   *
   * @return Current position in milliseconds, or -1 if no audio is loaded.
   */
  fun getCurrentPosition(): Int {
    return mediaPlayer?.currentPosition ?: -1
  }

  /**
   * Get the URI of the currently loaded audio.
   *
   * @return The current URI, or null if no audio is loaded.
   */
  fun getCurrentUri(): Uri? {
    return currentUri
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
  fun getUriFromSampleId(sampleId: Int): Uri {
    val recordNumber = (sampleId % 2) + 1
    return "android.resource://${context.packageName}/raw/record$recordNumber".toUri()
  }
}

// Provide a CompositionLocal to access the NeptuneMediaPlayer instance throughout the app
val LocalMediaPlayer = compositionLocalOf<NeptuneMediaPlayer> { error("No MediaPlayer provided") }
