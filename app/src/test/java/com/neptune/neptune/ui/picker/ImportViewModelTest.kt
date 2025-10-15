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
  private class Repo : MediaRepository {
    private val flow = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun observeAll() = flow

    override suspend fun upsert(item: MediaItem) {
      flow.value = flow.value + item
    }
  }

  private class Importer(private val dir: File) : FileImporter {
    override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
      val f =
          File(dir, "clip.wav").apply {
            outputStream().use { FileOutputStream(this).write(ByteArray(32) { 1 }) }
          }
      return FileImporter.ImportedFile(
          displayName = f.name,
          mimeType = "audio/wav",
          sourceUri = sourceUri,
          localUri = f.toURI(),
          sizeBytes = f.length(),
          durationMs = 800L)
    }
  }

  @Test
  fun import_triggers_repo_and_exposes_item() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val vm =
        ImportViewModel(
            ImportMediaUseCase(Importer(ctx.cacheDir), Repo(), NeptunePackager(StoragePaths(ctx))),
            GetLibraryUseCase(Repo()))
    vm.importFromSaf("content://x")
    val items = vm.library.first { it.isNotEmpty() }
    assertEquals(1, items.size)
  }
}
