package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that NeptunePackager creates unique zip files
   in the correct workspace directory, and that the zips contain
   the expected config and audio files.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NeptunePackagerTest {
  private val testDispatcher = StandardTestDispatcher()

  @Test
  fun createProjectZipCreatesUniqueZipInsideWorkspace() =
      runTest(testDispatcher) { // La coroutine est déjà lancée ici
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val paths = StoragePaths(ctx)
        val packager = NeptunePackager(paths, testDispatcher)

        val input = File(ctx.cacheDir, "a.wav").apply { writeBytes(ByteArray(4)) }

        val z1 = packager.createProjectZip(input, durationMs = 500L)
        val z2 = packager.createProjectZip(input, durationMs = 600L)

        assertThat(z1.exists()).isTrue()
        assertThat(z2.exists()).isTrue()
        assertThat(z1.parentFile!!.canonicalPath).isEqualTo(paths.projectsWorkspace().canonicalPath)
        assertThat(z2.parentFile!!.canonicalPath).isEqualTo(paths.projectsWorkspace().canonicalPath)
        assertThat(z1.name).isNotEqualTo(z2.name)
      }

  @Test
  fun projectFileNumbersEqualExistingCountAndFallsBackIfTaken() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val paths = StoragePaths(ctx)
    val ws = paths.projectsWorkspace().apply { mkdirs() }

    // none -> "a.zip"
    val f0 = paths.projectFile("a")
    assertThat(f0.name).isEqualTo("a.zip")
    f0.writeText("x")

    // one exists -> "a-1.zip"
    val f1 = paths.projectFile("a")
    assertThat(f1.name).isEqualTo("a-1.zip")
    f1.writeText("y")

    // two exist -> "a-2.zip"
    val f2 = paths.projectFile("a")
    assertThat(f2.name).isEqualTo("a-2.zip")
    f2.writeText("z")

    // If someone already created "a-3.zip", count=3, prefer "a-3.zip" but if taken, fall to
    // "a-4.zip"
    File(ws, "a-3.zip").writeText("w")
    val f3 = paths.projectFile("a")
    assertThat(f3.name).isEqualTo("a-4.zip")
  }

  @Test
  fun createsZipWithConfigAndAudio() =
      runTest(testDispatcher) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val paths = StoragePaths(ctx)

        val packager = NeptunePackager(paths, testDispatcher)

        val input =
            File(ctx.cacheDir, "in.wav").apply {
              outputStream().use { FileOutputStream(this).write(ByteArray(48) { 0x55 }) }
            }

        val out = packager.createProjectZip(audioFile = input, durationMs = 1200L)

        assertThat(out.exists()).isTrue()
        assertThat(out.extension).isEqualTo("zip")

        ZipFile(out).use { zip ->
          val names = zip.entries().toList().map { it.name }
          assertThat(names).containsAtLeast("config.json", "in.wav")
          val cfg = zip.getInputStream(zip.getEntry("config.json")).bufferedReader().readText()
          val normalizedConfig = cfg.replace(Regex("\\s+"), "")
          assertThat(normalizedConfig).contains("\"name\":\"in.wav\"")
          assertThat(normalizedConfig).contains("\"volume\":100")
          assertThat(normalizedConfig).contains("\"start\":0.0")
          assertThat(normalizedConfig).contains("\"duration\":1.2")
          assertThat(normalizedConfig).contains("\"audioFiles\":[")
          assertThat(normalizedConfig).contains("\"parameters\":[]")
        }
      }
}
