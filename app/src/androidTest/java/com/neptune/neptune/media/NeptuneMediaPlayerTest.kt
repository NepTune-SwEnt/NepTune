package com.neptune.neptune.media

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.net.toUri
import com.neptune.neptune.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NeptuneMediaPlayerTest {
  private lateinit var mediaPlayer: NeptuneMediaPlayer
  private lateinit var testURI1: Uri
  private lateinit var testURI2: Uri
  private lateinit var context: Context

  fun isPlayingURI(uri: Uri): Boolean {
    return mediaPlayer.isPlaying() && mediaPlayer.getCurrentUri() == uri
  }

  fun waitForPlayback(timeoutMs: Long = 200): Boolean {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeoutMs) {
      if (mediaPlayer.isPlaying()) return true
      Thread.sleep(10)
    }
    return false
  }

  var timeStart = 0L

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    context = composeTestRule.activity.applicationContext
    mediaPlayer = NeptuneMediaPlayer()

    testURI1 = Uri.parse("android.resource://${context.packageName}/${R.raw.record1}")
    testURI2 = Uri.parse("android.resource://${context.packageName}/${R.raw.record2}")
  }

  @Test
  fun testNoURIPlaying() {
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == null)
  }

  @Test
  fun testPlay() {
    mediaPlayer.play(testURI1)
    timeStart = System.currentTimeMillis()
    waitForPlayback()
    assert(isPlayingURI(testURI1))

    mediaPlayer.play(testURI2)
    waitForPlayback()
    assert(isPlayingURI(testURI2))
  }

  @Test
  fun testPause() {
    mediaPlayer.play(testURI1)
    waitForPlayback()

    mediaPlayer.pause()
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == testURI1)
  }

  @Test
  fun testResume() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    mediaPlayer.pause()
    mediaPlayer.resume()
    waitForPlayback()
    assert(isPlayingURI(testURI1))
  }

  @Test
  fun testTogglePlayOnSameURI() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    assert(isPlayingURI(testURI1))

    mediaPlayer.togglePlay(testURI1)
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == testURI1)

    mediaPlayer.togglePlay(testURI1)
    waitForPlayback()
    assert(isPlayingURI(testURI1))
  }

  @Test
  fun testTogglePlayOnDifferentURI() {
    mediaPlayer.play(testURI1)
    waitForPlayback()

    mediaPlayer.togglePlay(testURI2)
    waitForPlayback()
    assert(isPlayingURI(testURI2))
  }

  @Test
  fun testTogglePause() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    assert(isPlayingURI(testURI1))

    mediaPlayer.togglePause()
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == testURI1)

    mediaPlayer.togglePause()
    waitForPlayback()
    assert(isPlayingURI(testURI1))
  }

  @Test
  fun testStop() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    mediaPlayer.stop()
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == null)
  }

  @Test
  fun testGetDurationValid() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    val duration = mediaPlayer.getDuration()
    assert(duration > 0)
  }

  @Test
  fun testGetDurationNoMedia() {
    assert(mediaPlayer.getDuration() == -1)
  }

  @Test
  fun testGetCurrentPositionValid() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    assert(mediaPlayer.getCurrentPosition() >= 0)
  }

  @Test
  fun testGetCurrentPositionNoMedia() {
    assert(mediaPlayer.getCurrentPosition() == -1)
  }

  @Test
  fun testSeekTo() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    mediaPlayer.goTo(500)
    assert(mediaPlayer.getCurrentPosition() >= 500)
  }

  @Test
  fun testSeekToNoMedia() {
    mediaPlayer.goTo(500)
    assert(mediaPlayer.getCurrentPosition() == -1)
  }

  @Test
  fun testGetUriFromSampleId() {
    val uri1 = mediaPlayer.getUriFromSampleId("0")
    val uri2 = mediaPlayer.getUriFromSampleId("1")

    assert(uri1 == "android.resource://${context.packageName}/raw/record1".toUri())
    assert(uri2 == "android.resource://${context.packageName}/raw/record2".toUri())
  }

  @Test
  fun testSetVolume() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    mediaPlayer.setVolume(2.5f)
    mediaPlayer.setVolume(-5f)
    mediaPlayer.setVolume(0.5f)
  }

  @Test
  fun testStopWithFadeZero() {
    mediaPlayer.play(testURI1)
    waitForPlayback()
    mediaPlayer.stopWithFade(0)
    Thread.sleep(50)
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == null)
  }

  @Test
  fun testStopWithFadePositive() {
    mediaPlayer.play(testURI1)
    waitForPlayback()

    mediaPlayer.stopWithFade(2000)
    assert(mediaPlayer.isPlaying())

    Thread.sleep(3000)
    assert(!mediaPlayer.isPlaying())
    assert(mediaPlayer.getCurrentUri() == null)

    mediaPlayer.stopWithFade(200)
  }
}
