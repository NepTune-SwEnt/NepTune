package com.neptune.neptune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoragePathsTest {
  @Test
  fun creates_projects_workspace_and_zip_name() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val paths = StoragePaths(ctx)
    val dir = paths.projectsWorkspace()
    assertTrue(dir.exists() || dir.mkdirs())
    val f = paths.projectFile("demo")
    assertEquals("zip", f.extension)
    assertTrue(f.parentFile!!.absolutePath.startsWith(dir.absolutePath))
  }
}
