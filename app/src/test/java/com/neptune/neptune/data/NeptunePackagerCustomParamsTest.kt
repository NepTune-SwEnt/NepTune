package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Test that custom volume and startSeconds parameters are respected in the generated config.json
// written with ChatGPT
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NeptunePackagerCustomParamsTest {
  private val testDispatcher = StandardTestDispatcher()

  @Test
  fun createProjectZipRespectsVolumeAndStartSeconds() =
      runTest(testDispatcher) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val paths = StoragePaths(ctx)

        val packager = NeptunePackager(paths, testDispatcher)

        val input =
            File(ctx.cacheDir, "voice.wav").apply {
              FileOutputStream(this).use { it.write(ByteArray(16) { 0x22 }) }
            }

        val out =
            packager.createProjectZip(
                audioFile = input, durationMs = 2400L, volume = 73, startSeconds = 1.75)

        ZipFile(out).use { zip ->
          val cfg = zip.getInputStream(zip.getEntry("config.json")).bufferedReader().readText()

          val root = JSONObject(cfg)
          val filesArray = root.getJSONArray("audioFiles")
          val firstFile = filesArray.getJSONObject(0)

          assertThat(firstFile.getString("name")).isEqualTo("voice.wav")

          assertThat(firstFile.getInt("volume")).isEqualTo(73)
          assertThat(firstFile.getDouble("start")).isWithin(1e-9).of(1.75)

          assertThat(firstFile.getDouble("duration")).isWithin(1e-9).of(2.4)

          val parameters = root.getJSONArray("parameters")
          assertThat(parameters.length()).isEqualTo(0)
        }
      }
}
