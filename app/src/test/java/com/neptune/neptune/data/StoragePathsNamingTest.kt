package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoragePathsNamingTest {

  @Test
  fun differentBaseNamesCreateIndependentZipFilesUnderWorkspace() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)

    val ws = paths.projectsWorkspace()
    ws.mkdirs()
    val f1 = paths.projectFile("alpha")
    val f2 = paths.projectFile("beta")

    // They should be under the same workspace, with .zip extension and unique names
    assertThat(f1.parentFile!!.absolutePath).isEqualTo(ws.absolutePath)
    assertThat(f2.parentFile!!.absolutePath).isEqualTo(ws.absolutePath)
    assertThat(f1.name).endsWith(".zip")
    assertThat(f2.name).endsWith(".zip")
    assertThat(f1.name).isNotEqualTo(f2.name)

    // Touch them to guarantee writability
    f1.writeText("x")
    f2.writeText("y")
    assertThat(f1.exists()).isTrue()
    assertThat(f2.exists()).isTrue()
  }
}
