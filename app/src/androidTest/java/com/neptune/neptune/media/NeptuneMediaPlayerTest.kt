package com.neptune.neptune.media

import android.content.Context
import com.neptune.neptune.R
import android.net.Uri
import org.junit.Before
import org.junit.Test

class NeptuneMediaPlayerTest {
    private lateinit var contextMock: Context
    private lateinit var mediaPlayer: NeptuneMediaPlayer
    private lateinit var testURI1: Uri
    private lateinit var testURI2: Uri

    // Helper functions
    fun isPlayingURI(uri: Uri): Boolean {
        return mediaPlayer.isPlaying() && mediaPlayer.getCurrentUri() == uri
    }

    @Before
    fun setup() {
        testURI1 = Uri.parse("android.resource://com.neptune.neptune/" + R.raw.record1)
        testURI2 = Uri.parse("android.resource://com.neptune.neptune/" + R.raw.record2)
        println(testURI1.toString())
        println(testURI2.toString())
        contextMock = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        mediaPlayer = NeptuneMediaPlayer(contextMock)
    }

    @Test
    fun testNoURIPlaying() {
        assert(!mediaPlayer.isPlaying())
        assert(mediaPlayer.getCurrentUri() == null)
    }

    @Test
    fun testPlay() {
        mediaPlayer.play(testURI1)
        assert(isPlayingURI(testURI1))

        mediaPlayer.play(testURI2)
        assert(isPlayingURI(testURI2))
    }

    @Test
    fun testPause(){
        mediaPlayer.play(testURI1)

        mediaPlayer.pause()
        assert(!mediaPlayer.isPlaying())
        assert(mediaPlayer.getCurrentUri() == testURI1)
    }

    @Test
    fun testResume(){
        mediaPlayer.play(testURI1)
        mediaPlayer.pause()
        mediaPlayer.resume()
        assert(isPlayingURI(testURI1))
    }

    @Test
    fun testTogglePlayTogglePauseOnSameURI(){
        mediaPlayer.play(testURI1)
        assert(isPlayingURI(testURI1))

        mediaPlayer.togglePlay(testURI1)
        assert(!mediaPlayer.isPlaying())
        assert(mediaPlayer.getCurrentUri() == testURI1)

        mediaPlayer.togglePlay(testURI1)
        assert(isPlayingURI(testURI1))
    }

    @Test
    fun testTogglePlayOnDifferentURI(){
        mediaPlayer.play(testURI1)

        mediaPlayer.togglePlay(testURI2)
        assert(isPlayingURI(testURI2))
    }

    @Test
    fun testStop(){
        mediaPlayer.play(testURI1)
        mediaPlayer.stop()
        assert(!mediaPlayer.isPlaying())
        assert(mediaPlayer.getCurrentUri() == null)
    }

    @Test
    fun testGetDurationValid(){
        mediaPlayer.play(testURI1)
        val duration = mediaPlayer.getDuration()
        assert(duration > 0)
    }

    @Test
    fun testGetDurationNoMedia(){
        assert(mediaPlayer.getDuration() == -1)
    }


    @Test
    fun testGetCurrentPositionValid(){
        mediaPlayer.play(testURI1)
        Thread.sleep(500)
        assert(mediaPlayer.getCurrentPosition() >= 500)
    }

    @Test
    fun testGetCurrentPositionNoMedia(){
        assert(mediaPlayer.getCurrentPosition() == -1)
    }

    @Test
    fun testSeekTo() {
        mediaPlayer.play(testURI1)
        mediaPlayer.goTo(500)
        assert(mediaPlayer.getCurrentPosition() >= 500)
    }

    @Test
    fun testSeekToNoMedia() {
        mediaPlayer.goTo(500)
        assert(mediaPlayer.getCurrentPosition() == -1)
    }



}