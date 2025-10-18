// app/src/test/java/com/neptune/neptune/data/AudioWorkspaceTest.kt
package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that the audio workspace is located under the "imports" folder
   within the app-specific external files directory, and that it is writable.
   Written with help from ChatGPT.
*/
@RunWith(RobolectricTestRunner::class)
class AudioWorkspaceTest {

  @Test
  fun audioWorkspace_is_under_imports_folder_and_writable() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)

    val externalBase = requireNotNull(ctx.getExternalFilesDir(null)).canonicalFile
    val audioDir = paths.audioWorkspace().canonicalFile

    // The audio workspace must live inside "<external>/imports"
    val importsDir = File(externalBase, "imports").canonicalFile
    assertThat(audioDir.path.startsWith(importsDir.path)).isTrue()

    // Name check (last folder is "audio")
    assertThat(audioDir.name).isEqualTo("audio")

    // Ensure directory exists and is writable
    assertThat(audioDir.exists() || audioDir.mkdirs()).isTrue()
    val testFile = File(audioDir, "beep.tmp").apply { writeText("ok") }
    assertThat(testFile.exists()).isTrue()
  }
}
