package com.neptune.neptune.media

import android.media.MediaPlayer
import kotlinx.coroutines.*

fun fadeOutAndRelease(
    mediaPlayer: MediaPlayer,
    releaseMillis: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    onFinished: (() -> Unit)? = null
) {
  if (releaseMillis <= 0L) {
    try {
      if (mediaPlayer.isPlaying) mediaPlayer.stop()
    } catch (_: Exception) {}

    try {
      mediaPlayer.release()
    } catch (_: Exception) {}

    onFinished?.invoke()
    return
  }

  scope.launch {
    val steps = 20
    val stepDelay = (releaseMillis / steps).coerceAtLeast(5L)

    for (i in steps downTo 1) {
      val level = i.toFloat() / steps
      try {
        mediaPlayer.setVolume(level, level)
      } catch (_: Exception) {}
      delay(stepDelay)
    }

    try {
      mediaPlayer.setVolume(0f, 0f)
      if (mediaPlayer.isPlaying) mediaPlayer.stop()
    } catch (_: Exception) {}

    try {
      mediaPlayer.release()
    } catch (_: Exception) {}

    onFinished?.invoke()
  }
}
