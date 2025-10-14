// app/src/test/java/com/neptune/neptune/data/NeptunePackagerRobolectricTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeptunePackagerRobolectricTest {

  @Test
  fun createProjectZip_writesConfigAndAudio() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val paths = StoragePaths(context)
    val packager = NeptunePackager(paths)

    // Create a tiny “audio” file inside the real app external files dir
    val audioDir = paths.audioWorkspace()
    val audio =
        File(audioDir, "clip.wav").apply {
          outputStream().use { FileOutputStream(this).write(ByteArray(256) { 0x23 }) }
        }

    val zip = packager.createProjectZip(audio, durationMs = 3456L, volume = 80, startSeconds = 0.5)
    assertTrue(zip.exists())
    assertEquals("zip", zip.extension)

    val names = ZipFile(zip).use { zf -> zf.entries().toList().map { it.name }.toSet() }
    assertTrue("config.json missing", "config.json" in names)
    assertTrue("clip.wav missing", "clip.wav" in names)

    val config =
        ZipFile(zip).use { zf -> zf.getInputStream(zf.getEntry("config.json")).reader().readText() }
    assertTrue(config.contains("\"filename\": \"clip.wav\""))
    assertTrue(config.contains("\"volume\": 80"))
    assertTrue(config.contains("\"start\": 0.5"))
    assertTrue(config.contains("\"duration\": 3.5"))
  }
}
