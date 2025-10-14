package com.neptune.neptune.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf

class NeptuneMediaPlayer(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null
  private var currentUri: Uri? = null

  /**
   * Play the audio from the given URI. If another audio is already playing, it will be stopped and
   * replaced.
   */
  fun play(uri: Uri) {
    if (mediaPlayer == null) {
      mediaPlayer =
          MediaPlayer().apply {
            setDataSource(context, uri)
            prepareAsync()
            setOnPreparedListener { start() }
          }
    } else {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.stop()
        }
        it.reset()
        it.setDataSource(context, uri)
        it.prepareAsync()
      }
    }
    currentUri = uri
  }

  /**
   * Toggle play/pause for the given URI. If the URI is currently playing, it will be paused. If
   * it's paused or a different URI is provided, it will start playing.
   */
  fun togglePlay(uri: Uri) {
    if (isPlaying() && uri == currentUri) {
      pause()
    } else {
      if (mediaPlayer == null) {
        play(uri)
      } else {
        resume()
      }
    }
  }

  /** Pause the currently playing audio. */
  fun pause() {
    mediaPlayer?.let {
      if (it.isPlaying) {
        it.pause()
      }
    }
  }

  /** Resume the currently paused audio. */
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

  /** Check if the audio is currently playing. */
  fun isPlaying(): Boolean {
    return mediaPlayer?.isPlaying ?: false
  }
}

// Provide a CompositionLocal to access the NeptuneMediaPlayer instance throughout the app
val LocalMediaPlayer = compositionLocalOf<NeptuneMediaPlayer> { error("No MediaPlayer provided") }
