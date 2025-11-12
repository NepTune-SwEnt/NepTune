package com.neptune.neptune.data.record.wire

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neptune.neptune.data.StoragePaths
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoragePathsTest {
  private lateinit var context: Context
  private lateinit var paths: StoragePaths

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    paths = StoragePaths(context)
  }

  @After
  fun cleanup() {
    val ws = paths.projectsWorkspace()
    ws.listFiles()?.forEach { it.delete() }
  }

  @Test
  fun sanitize_removesWhitespace_and_collapsesUnderscores() {
    val bad = "  Bad  Name__..test  "
    val candidate = paths.projectFile(bad)
    val stem = candidate.name.removeSuffix(".zip")

    assertFalse("no whitespace", stem.contains(Regex("\\s")))
    assertFalse("no consecutive underscores", stem.contains("__"))
    assertFalse("no leading dot", stem.startsWith("."))
    assertTrue(
        "inside projects workspace",
        candidate.canonicalPath.startsWith(paths.projectsWorkspace().canonicalPath))
  }

  @Test
  fun projectFile_suffixing_handlesGaps_and_returnsNextAvailable() {
    val ws = paths.projectsWorkspace()
    if (!ws.exists()) ws.mkdirs()

    // Create myproj.zip, myproj-1.zip, myproj-3.zip
    File(ws, "myproj.zip").writeText("x")
    File(ws, "myproj-1.zip").writeText("x")
    File(ws, "myproj-3.zip").writeText("x")

    val candidate = paths.projectFile("myproj")
    // Should pick the next available suffix >3, i.e., myproj-4.zip
    assertEquals("myproj-4.zip", candidate.name)
  }

  @Test
  fun projectFile_emptyName_returnsDefaultProject() {
    val candidate = paths.projectFile("")
    assertTrue(candidate.name.startsWith("project"))
    assertTrue(candidate.name.endsWith(".zip"))
  }
}
