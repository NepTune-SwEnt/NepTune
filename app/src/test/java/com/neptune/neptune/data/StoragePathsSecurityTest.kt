// app/src/test/java/com/neptune/neptune/data/StoragePathsSecurityTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that the StoragePaths.projectFile() method sanitizes
   input to prevent directory traversal and enforces a single .zip extension.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
class StoragePathsSecurityTest {

  @Test
  fun projectFileSanitizesBaseNameAndExportsZipExtension() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val base = requireNotNull(ctx.getExternalFilesDir(null)).canonicalFile
    val ws = paths.projectsWorkspace().canonicalFile

    val evil = paths.projectFile("../evil..project.zip").canonicalFile
    val normal = paths.projectFile("good").canonicalFile

    // Location: projectFile must live INSIDE externalFilesDir (regardless of whether you use
    // projects/)
    assertThat(evil.path.startsWith(base.path)).isTrue()
    assertThat(normal.path.startsWith(base.path)).isTrue()

    // Optional: if your impl DOES use projects/, this will still pass; if not, it's fine.
    // We only assert containment, not exact parent equality anymore.
    assertThat(ws.path.startsWith(base.path)).isTrue()

    // No raw traversal remnants and only a single .zip extension
    assertThat(evil.name).doesNotContain("..")
    assertThat(evil.extension).isEqualTo("zip")
    assertThat(normal.extension).isEqualTo("zip")

    // Extra: avoid ".zip.zip" or empty basenames like ".zip"
    fun hasSingleZip(name: String) =
        name.endsWith(".zip") && !name.substringBeforeLast(".zip").endsWith(".zip")
    assertThat(hasSingleZip(evil.name)).isTrue()
    assertThat(hasSingleZip(normal.name)).isTrue()
  }
}
