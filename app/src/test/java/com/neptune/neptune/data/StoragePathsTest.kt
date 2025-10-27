// app/src/test/java/com/neptune/neptune/data/StoragePathsTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that the StoragePaths returns directories and files
   under the app-specific external files directory, which is writable
   and does not require special permissions.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
class StoragePathsTest {

  @Test
  fun projectWorkspaceAndFileAreUnderInternalFilesDir() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)

    // This is the base directory Android gives you for app-specific "internal" storage:
    // Real device:  /data/data/com.neptune.neptune/files
    // Robolectric:  /tmp/.../external-files
    val expectedBase = requireNotNull(ctx.filesDir).canonicalPath

    val ws = paths.projectsWorkspace()
    assertThat(ws.exists() || ws.mkdirs()).isTrue()

    // workspace sits in internal files dir + "projects"
    assertThat(ws.parentFile!!.canonicalPath).isEqualTo(expectedBase)
    assertThat(ws.name).isEqualTo("projects")

    val f = paths.projectFile("unit-test-zip")
    assertThat(f.parentFile!!.absolutePath).isEqualTo(ws.absolutePath)
    assertThat(f.extension).isEqualTo("zip")

    // write something to ensure directory is writable
    File(f.parentFile, "touch.tmp").writeText("ok")
    assertThat(File(f.parentFile, "touch.tmp").exists()).isTrue()
  }
}
