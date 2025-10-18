package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Test that custom volume and startSeconds parameters are respected in the generated config.json
// written with ChatGPT
@RunWith(RobolectricTestRunner::class)
class NeptunePackagerCustomParamsTest {

  @Test
  fun createProjectZip_respectsVolumeAndStartSeconds() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val packager = NeptunePackager(StoragePaths(ctx))

    val audio =
        File(ctx.cacheDir, "voice.wav").apply {
          FileOutputStream(this).use { it.write(ByteArray(16) { 0x22 }) }
        }

    val out =
        packager.createProjectZip(
            audioFile = audio,
            durationMs = 2400L, // 2.4s -> rounds to 2.4
            volume = 73,
            startSeconds = 1.75)

    ZipFile(out).use { zip ->
      val cfg = zip.getInputStream(zip.getEntry("config.json")).bufferedReader().readText()

      assertThat(cfg).contains("\"filename\":\"voice.wav\"")
      assertThat(cfg).contains("\"volume\":73")
      assertThat(cfg).contains("\"start\":1.75")
      assertThat(cfg).contains("\"duration\":2.4")
    }
  }
}
