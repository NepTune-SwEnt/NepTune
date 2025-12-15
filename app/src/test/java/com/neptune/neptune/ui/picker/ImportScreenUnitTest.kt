package com.neptune.neptune.ui.picker

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ImportScreenUnitTest {

  @Test
  fun sanitizeAndRename_replacesInvalidCharsAndRenamesFile() {
    val tmpDir = createTempDir(prefix = "neptune_test_")
    try {
      val original = File(tmpDir, "recording.m4a")
      original.writeText("dummy")
      assertTrue("original should exist before rename", original.exists())

      val renamed = sanitizeAndRename(original, "My Recording!")

      // Expect sanitized name My_Recording.m4a
      assertEquals("My_Recording.m4a", renamed.name)
      assertTrue("renamed file should exist", renamed.exists())
      // original should no longer exist (was renamed)
      if (original.absolutePath != renamed.absolutePath) {
        assertFalse("original should not exist after successful rename", original.exists())
      }
    } finally {
      tmpDir.deleteRecursively()
    }
  }

  @Test
  fun sanitizeAndRename_doesNotOverwriteExistingFile() {
    val tmpDir = createTempDir(prefix = "neptune_test_")
    try {
      val original = File(tmpDir, "recording.m4a")
      original.writeText("orig")
      val desired = File(tmpDir, "My_Recording.m4a")
      desired.writeText("existing")

      assertTrue(original.exists())
      assertTrue(desired.exists())

      val result = sanitizeAndRename(original, "My Recording")

      // Because desired existed, sanitizeAndRename should return the original file unchanged
      assertEquals(original.absolutePath, result.absolutePath)
      assertTrue("original should still exist", original.exists())
      assertTrue("desired should still exist", desired.exists())
    } finally {
      tmpDir.deleteRecursively()
    }
  }
}
