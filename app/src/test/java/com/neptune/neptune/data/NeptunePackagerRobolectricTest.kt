// app/src/test/java/com/neptune/neptune/data/NeptunePackagerRobolectricTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
class NeptunePackagerRobolectricTest {

  @Test
  fun createProjectZip_writesConfigAndAudio() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val paths = StoragePaths(context)
    val packager = NeptunePackager(paths)

    // Create a tiny "audio" file in your imports/audio (or whatever your method is)
    val audioDir: File = paths.audioWorkspace()   // if your API is importsAudioDir(), use that
    audioDir.mkdirs()
    val audio = File(audioDir, "clip.wav").apply {
      // simpler write
      writeBytes(ByteArray(256) { 0x23 })
    }

    val zip = packager.createProjectZip(
      audioFile = audio,
      durationMs = 3456L,  // 3.456s -> rounded to 3.5
      volume = 80,
      startSeconds = 0.5
    )

    assertTrue(zip.exists())
    assertEquals("zip", zip.extension)

    ZipFile(zip).use { zf ->
      // Collect entry names (Enumeration -> set)
      val names = mutableSetOf<String>()
      val en = zf.entries()
      while (en.hasMoreElements()) {
        names += en.nextElement().name
      }

      assertTrue("config.json missing", "config.json" in names)
      assertTrue("clip.wav missing", "clip.wav" in names)

      // Read and parse JSON (ignore whitespace & formatting)
      val cfg = zf.getInputStream(zf.getEntry("config.json"))
        .bufferedReader().readText()

      val root = JSONObject(cfg)
      val files = root.getJSONArray("files")
      val first = files.getJSONObject(0)

      assertEquals("clip.wav", first.getString("filename"))
      assertEquals(80, first.getInt("volume"))
      assertEquals(0.5, first.getDouble("start"), 1e-9)
      assertEquals(3.5, first.getDouble("duration"), 1e-9)
    }
  }
}
