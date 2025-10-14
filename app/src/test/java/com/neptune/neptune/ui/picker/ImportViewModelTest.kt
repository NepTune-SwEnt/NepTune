package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportViewModelTest {

  private class FakeRepo : MediaRepository {
    private val flow = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun observeAll() = flow

    override suspend fun upsert(item: MediaItem) {
      flow.value = flow.value + item
    }
  }

  /**
   * Implements the current FileImporter API: suspend fun importFile(sourceUri: URI):
   * FileImporter.ImportedFile
   *
   * If your ImportedFile uses a different field name (e.g., 'file' instead of 'localUri'), change
   * the return statement accordingly.
   */
  private class FakeImporter(private val dir: File) : FileImporter {
    override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
      val f =
          File(dir, "picked.wav").apply {
            outputStream().use { FileOutputStream(this).write(ByteArray(128) { 0x55 }) }
          }
      return FileImporter.ImportedFile(
          displayName = f.name,
          mimeType = "audio/wav",
          localUri = f.toURI(), // if your type expects 'file = f', switch this line
          sizeBytes = f.length(),
          sourceUri = sourceUri,
          durationMs = 1100L)
    }
  }

  @Test
  fun import_updates_list_with_zip_project() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(context)
    val packager = NeptunePackager(paths) // keep if your use case still needs it
    val repo = FakeRepo()
    val importer = FakeImporter(context.cacheDir)

    // If your ImportMediaUseCase ctor changed (e.g., no packager anymore), drop 'packager' here.
    val importUC = ImportMediaUseCase(importer, repo, packager)
    val libraryUC = GetLibraryUseCase(repo)

    val vm = ImportViewModel(importUC, libraryUC)

    vm.importFromSaf("content://picked")

    val items = vm.library.first { it.isNotEmpty() }
    assertEquals(1, items.size)
    val projectUri = items.first().projectUri
    check(projectUri.endsWith(".zip")) { "expected .zip, got $projectUri" }
  }
}
