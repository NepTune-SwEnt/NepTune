package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that when creating a project zip with a very tiny duration (1 ms),
   the resulting config.json in the zip has a non-negative duration that rounds to
   either 0.0 or the minimum step (implementation-specific).
*/
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NeptunePackagerTinyDurationTest {
  private val testDispatcher: CoroutineDispatcher = StandardTestDispatcher()

  @Test
  fun tinyDurationIsNotNegativeAndRoundsToZeroOrMinStep() =
      runTest(testDispatcher) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val paths = StoragePaths(ctx)
        val packager = NeptunePackager(paths, testDispatcher)

        val audio = File(ctx.cacheDir, "blip.wav").apply { writeBytes(ByteArray(8) { 0x01 }) }

        val zip = packager.createProjectZip(audioFile = audio, durationMs = 1L)

        ZipFile(zip).use { z ->
          val cfg = z.getInputStream(z.getEntry("config.json")).bufferedReader().readText()
          val root = JSONObject(cfg)
          val firstFile = root.getJSONArray("audioFiles").getJSONObject(0)
          val dur = firstFile.getDouble("duration")

          assertThat(dur).isAtLeast(0.0)

          assertThat(dur).isWithin(1e-9).of(0.0)
        }
      }
}
