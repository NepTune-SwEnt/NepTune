package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipFile
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that when creating a project zip with default parameters
   (omitting volume and startSeconds), the resulting config.json in the zip
   uses the expected default values and that duration is rounded to one decimal place.
*/
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NeptunePackagerDefaultsTest {
  private val testDispatcher: CoroutineDispatcher = StandardTestDispatcher()

  @Test
  fun createProjectZipUsesDefaultsAndRoundsDurationTo1Decimal() =
      runTest(testDispatcher) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val paths = StoragePaths(ctx)
        val packager = NeptunePackager(paths, testDispatcher)

        val audio = File(ctx.cacheDir, "voice.m4a").apply { writeBytes(ByteArray(128) { 0x2A }) }

        val zip = packager.createProjectZip(audioFile = audio, durationMs = 1999L)

        assertTrue(zip.exists())
        assertEquals("zip", zip.extension)

        ZipFile(zip).use { z ->
          val cfg = z.getInputStream(z.getEntry("config.json")).bufferedReader().readText()

          val root = JSONObject(cfg)

          val audioFiles = root.getJSONArray("audioFiles")
          val firstFile = audioFiles.getJSONObject(0)

          val parameters = root.getJSONArray("parameters")

          assertThat(parameters.length()).isEqualTo(0)

          assertThat(firstFile.getString("name")).isEqualTo("voice.m4a")

          assertEquals(100, firstFile.getInt("volume"))
          assertThat(firstFile.getDouble("start")).isWithin(1e-9).of(0.0)

          assertThat(firstFile.getDouble("duration")).isWithin(1e-9).of(2.0)
        }
      }
}
