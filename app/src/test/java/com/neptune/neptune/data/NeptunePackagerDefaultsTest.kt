package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that when creating a project zip with default parameters
   (omitting volume and startSeconds), the resulting config.json in the zip
   uses the expected default values and that duration is rounded to one decimal place.
   Adjust expected default values if your implementation differs.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
class NeptunePackagerDefaultsTest {

  @Test
  fun createProjectZip_uses_defaults_and_rounds_duration_to_1_decimal() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val packager = NeptunePackager(StoragePaths(ctx))

    // Non-wav to ensure arbitrary extensions are preserved
    val audio = File(ctx.cacheDir, "voice.m4a").apply { writeBytes(ByteArray(128) { 0x2A }) }

    // 1.999 s -> 2.0 (check boundary rounding)
    val zip =
        packager.createProjectZip(
            audioFile = audio, durationMs = 1999L // 1.999 sec
            // volume & startSeconds omitted => defaults path
            )

    ZipFile(zip).use { z ->
      val cfg = z.getInputStream(z.getEntry("config.json")).bufferedReader().readText()
      val files = JSONObject(cfg).getJSONArray("files")
      val f0 = files.getJSONObject(0)

      // filename preserved with extension
      assertThat(f0.getString("filename")).isEqualTo("voice.m4a")

      // Defaults: these should be whatever your code sets by default.
      // Adjust expected values if your defaults differ.
      assertThat(f0.getInt("volume")).isEqualTo(100) // default path
      assertThat(f0.getDouble("start")).isWithin(1e-9).of(0.0)

      // Boundary rounding to one decimal place
      assertThat(f0.getDouble("duration")).isWithin(1e-9).of(2.0)
    }
  }
}
