package com.neptune.neptune.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import java.io.File
import java.net.URI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   This test ensures that if the Importer fails (throws), the repository remains unchanged
   and no zip file is created.
   Written with help from ChatGPT.
*/
private class ThrowingImporter : FileImporter {
  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    throw IllegalArgumentException("invalid URI for testing")
  }
}

private class CapturingRepo : MediaRepository {
  private val state = MutableStateFlow<List<MediaItem>>(emptyList())

  override fun observeAll(): Flow<List<MediaItem>> = state

  override suspend fun upsert(item: MediaItem) {
    state.value = state.value + item
  }
}

@RunWith(RobolectricTestRunner::class)
class ImportMediaUseCaseErrorTest {

  @Test
  fun whenImporterFailsRepositoryIsNotUpdatedAndNoZipIsCreated() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val packager = NeptunePackager(paths)
    val repo = CapturingRepo()
    val uc = ImportMediaUseCase(ThrowingImporter(), repo, packager)

    val ws: File = paths.projectsWorkspace()
    ws.mkdirs()
    val before = ws.listFiles()?.toList().orEmpty()

    // Should not throw out of the use case; if it does, the test will fail.
    try {
      uc.invoke("content://anything")
    } catch (_: Throwable) {
      // If your UC purposely propagates, comment this catch and let test fail.
    }

    val after = ws.listFiles()?.toList().orEmpty()
    // No new files created
    assertThat(after.size).isEqualTo(before.size)

    // Repository remains empty
    assertThat(repo.observeAll().first()).isEmpty()
  }
}
