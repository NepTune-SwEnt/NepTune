// app/src/test/java/com/neptune/neptune/data/NeptunePackagerRobolectricTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test verifies that NeptunePackager.createProjectZip correctly creates a zip file
   containing the specified audio file and a config.json with the expected content.
   It uses Robolectric to provide an Android-like environment for file operations.
   Written with help from ChatGPT.
*/
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NeptunePackagerRobolectricTest {
  private val testDispatcher = StandardTestDispatcher()

  @Test
  fun createProjectZip_writesConfigAndAudio() =
      runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val paths = StoragePaths(context)

        val packager = NeptunePackager(paths, testDispatcher)

        val audioDir: File = paths.audioWorkspace()
        audioDir.mkdirs()
        val audio = File(audioDir, "clip.wav").apply { writeBytes(ByteArray(256) { 0x23 }) }

        val zip =
            packager.createProjectZip(
                audioFile = audio, durationMs = 3456L, volume = 80, startSeconds = 0.5)

        assertTrue(zip.exists())
        assertEquals("zip", zip.extension)

        ZipFile(zip).use { zf ->
          val cfg = zf.getInputStream(zf.getEntry("config.json")).bufferedReader().readText()
          val root = JSONObject(cfg)

          val filesArray = root.getJSONArray("audioFiles")
          val firstFile = filesArray.getJSONObject(0)

          assertEquals("clip.wav", firstFile.getString("name"))

          assertEquals(80, firstFile.getInt("volume"))
          assertEquals(0.5, firstFile.getDouble("start"), 1e-9)

          assertEquals(3.5, firstFile.getDouble("duration"), 1e-9)

          val paramsArray = root.getJSONArray("parameters")
          assertTrue("Parameters array should be empty", paramsArray.length() == 0)
        }
      }
}
