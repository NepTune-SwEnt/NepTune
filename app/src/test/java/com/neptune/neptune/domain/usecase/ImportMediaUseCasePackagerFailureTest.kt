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
   This test ensures that if the Packager fails (e.g. because the imported file
   is missing), the repository remains unchanged and no zip file is created.
   Written with help from ChatGPT.
*/
class FakeRepo : MediaRepository {
  private val s = MutableStateFlow<List<MediaItem>>(emptyList())

  override fun observeAll(): Flow<List<MediaItem>> = s

  override suspend fun upsert(item: MediaItem) {
    s.value = s.value + item
  }
}

private class DisappearingImporter(private val dir: File) : FileImporter {
  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    val tmp = File(dir, "soon_gone.wav").apply { writeBytes(ByteArray(16)) }
    // delete the file so Packager fails when trying to read it
    val uri = tmp.toURI()
    tmp.delete()
    return FileImporter.ImportedFile(
        displayName = "soon_gone.wav",
        mimeType = "audio/wav",
        sourceUri = sourceUri,
        localUri = uri,
        sizeBytes = 16L,
        durationMs = 500L)
  }
}

@RunWith(RobolectricTestRunner::class)
class ImportMediaUseCasePackagerFailureTest {

  @Test
  fun whenPackagerFails_repositoryRemainsUnchanged_and_noZipIsCreated() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val packager = NeptunePackager(paths)
    val repo = FakeRepo()
    val importer = DisappearingImporter(ctx.cacheDir)
    val uc = ImportMediaUseCase(importer, repo, packager)

    val beforeFiles = paths.projectsWorkspace().list()?.toList().orEmpty()

    try {
      uc("content://x") // Depending on your UC behavior, this may throw; we just observe
      // side-effects.
    } catch (_: Throwable) {
      /* propagation OK */
    }

    val afterFiles = paths.projectsWorkspace().list()?.toList().orEmpty()
    assertThat(afterFiles).isEqualTo(beforeFiles)
    assertThat(repo.observeAll().first()).isEmpty()
  }
}
