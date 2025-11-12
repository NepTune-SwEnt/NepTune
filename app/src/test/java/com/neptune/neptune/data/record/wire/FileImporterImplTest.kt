package com.neptune.neptune.data.record.wire

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.data.FileImporterImpl
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.UnsupportedAudioFormat
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileImporterImplTest {
  private lateinit var context: Context
  private lateinit var paths: StoragePaths
  private lateinit var importer: FileImporterImpl

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    paths = StoragePaths(context)
    importer = FileImporterImpl(context, context.contentResolver, paths)
  }

  @After
  fun cleanup() {
    // remove any files created under imports/audio and records
    val audioDir = paths.audioWorkspace()
    audioDir.listFiles()?.forEach { it.delete() }
    val recDir = paths.recordWorkspace()
    recDir.listFiles()?.forEach { it.delete() }
  }

  @Test
  fun importRecorded_sanitizesName_and_collapsesMultipleSpecials() = runBlocking {
    // Create a temporary recorded file with special characters and repeated underscores
    val recDir = paths.recordWorkspace()
    if (!recDir.exists()) recDir.mkdirs()
    val rawName = "Bad!!__Name@@@###.mp3"
    val tmp = File(recDir, rawName)
    tmp.writeText("dummy")

    val imported = importer.importRecorded(tmp)

    // displayName should be a safe filename with the same extension
    assertTrue("extension preserved", imported.displayName.endsWith(".mp3"))

    val base = imported.displayName.removeSuffix(".mp3")

    // should not contain invalid characters (only letters, digits, dot, underscore, dash)
    assertTrue("only allowed chars in base", base.matches(Regex("^[A-Za-z0-9._-]+$")))

    // should not contain consecutive underscores (collapse multiple to single)
    assertFalse("no consecutive underscores in $base", base.contains("__"))

    // file should exist in audio workspace
    val resultingFile = File(paths.audioWorkspace(), imported.displayName)
    assertTrue("file exists in audio workspace", resultingFile.exists())
  }

  @Test
  fun importRecorded_whenNameTaken_usesDashSuffix_withoutSpace() = runBlocking {
    val audioDir = paths.audioWorkspace()
    if (!audioDir.exists()) audioDir.mkdirs()

    // create existing file audio.mp3
    val existing = File(audioDir, "audio.mp3")
    existing.writeText("existing")

    // prepare recorded file with same name
    val recDir = paths.recordWorkspace()
    if (!recDir.exists()) recDir.mkdirs()
    val tmp = File(recDir, "audio.mp3")
    tmp.writeText("dummy")

    val imported = importer.importRecorded(tmp)

    // Should not produce a name containing a space before the suffix (no " (2)")
    assertFalse("no space before suffix", imported.displayName.contains(" ("))

    // Should contain a dash-number suffix like audio-2.mp3 (uniqueFile starts numbering at 2)
    assertTrue("dash-number suffix", imported.displayName.matches(Regex("^audio-\\d+\\.mp3$")))

    // resulting file should exist
    assertTrue(File(paths.audioWorkspace(), imported.displayName).exists())
  }

  @Test
  fun importRecorded_nonexistentFile_throwsIllegalArgumentException() = runBlocking {
    val missing = File(paths.recordWorkspace(), "does_not_exist.mp3")
    try {
      importer.importRecorded(missing)
      fail("Expected IllegalArgumentException for missing recorded file")
    } catch (e: IllegalArgumentException) {
      // expected
    }
  }

  @Test
  fun importRecorded_unsupportedExtension_throwsUnsupportedAudioFormat() = runBlocking {
    val recDir = paths.recordWorkspace()
    if (!recDir.exists()) recDir.mkdirs()
    val tmp = File(recDir, "not_audio.txt")
    tmp.writeText("dummy")

    try {
      importer.importRecorded(tmp)
      fail("Expected UnsupportedAudioFormat for .txt file")
    } catch (e: UnsupportedAudioFormat) {
      // expected
    }
  }

  @Test
  fun importRecorded_removesAllWhitespace_fromDisplayName() = runBlocking {
    val recDir = paths.recordWorkspace()
    if (!recDir.exists()) recDir.mkdirs()
    val tmp = File(recDir, "  spaced name  here .mp3")
    tmp.writeText("dummy")

    val imported = importer.importRecorded(tmp)

    val base = imported.displayName.removeSuffix(".mp3")
    assertFalse("no spaces in base", base.contains(Regex("\\s")))
    assertTrue(File(paths.audioWorkspace(), imported.displayName).exists())
  }

  @Test
  fun importRecorded_manyCollisions_picksNextAvailableSuffix() = runBlocking {
    val audioDir = paths.audioWorkspace()
    if (!audioDir.exists()) audioDir.mkdirs()

    // create audio.mp3 and audio-2..audio-5.mp3 existing
    File(audioDir, "audio.mp3").writeText("x")
    for (i in 2..5) File(audioDir, "audio-$i.mp3").writeText("x")

    val recDir = paths.recordWorkspace()
    if (!recDir.exists()) recDir.mkdirs()
    val tmp = File(recDir, "audio.mp3")
    tmp.writeText("dummy")

    val imported = importer.importRecorded(tmp)

    assertTrue(imported.displayName.matches(Regex("^audio-\\d+\\.mp3$")))
    // ensure it picks suffix >5 (i.e., 6 or more)
    val num = imported.displayName.removePrefix("audio-").removeSuffix(".mp3").toInt()
    assertTrue(num >= 6)
    assertTrue(File(paths.audioWorkspace(), imported.displayName).exists())
  }

  @Test
  fun importFile_missingSource_throwsIllegalArgumentException() = runBlocking {
    // use file:// URI pointing to a missing path
    val missing = URI.create("file:///nonexistent/path/doesnt_exist.mp3")
    try {
      importer.importFile(missing)
      fail("Expected IllegalArgumentException for missing source file")
    } catch (e: IllegalArgumentException) {
      // expected
    }
  }
}
