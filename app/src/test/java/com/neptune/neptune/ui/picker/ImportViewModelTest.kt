package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
import java.util.zip.ZipFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class FakeRepo : MediaRepository {
  private val flow = MutableStateFlow<List<MediaItem>>(emptyList())

  override fun observeAll(): Flow<List<MediaItem>> = flow

  override suspend fun upsert(item: MediaItem) {
    flow.value = flow.value + item
  }
}

private class FakeImporter(private val dir: File) : FileImporter {
  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    val f =
        File(dir, "picked.wav").apply {
          outputStream().use { FileOutputStream(this).write(ByteArray(32) { 0x44 }) }
        }
    return FileImporter.ImportedFile(
        displayName = f.name,
        mimeType = "audio/wav",
        sourceUri = sourceUri,
        localUri = f.toURI(),
        sizeBytes = f.length(),
        durationMs = 1234L)
  }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

  private lateinit var vm: ImportViewModel
  private lateinit var repo: FakeRepo
  private lateinit var importer: FakeImporter

  @Before
  fun setUp() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    repo = FakeRepo()
    importer = FakeImporter(ctx.cacheDir)
    val packager = NeptunePackager(StoragePaths(ctx))
    val importUC = ImportMediaUseCase(importer, repo, packager)
    val libraryUC = GetLibraryUseCase(repo)
    vm = ImportViewModel(importUC, libraryUC)
  }

  @Test
  fun import_updates_list_with_zip_project() {
    runTest {
      val before = vm.library.first()
      assertThat(before).isEmpty()

      vm.importFromSaf("content://any-audio")
      advanceUntilIdle() // flush launched coroutines

      val after = vm.library.first()
      assertThat(after).hasSize(1)
      val item = after.first()
      assertThat(item.projectUri).endsWith(".zip")

      // verify the produced zip has the expected entries
      val zipFile = File(URI(item.projectUri))
      val names = ZipFile(zipFile).use { z -> z.entries().toList().map { it.name } }
      assertThat(names).containsAtLeast("config.json", "picked.wav")
    }
  }

  @Test
  fun multiple_imports_append_list() {
    runTest {
      vm.importFromSaf("content://a")
      vm.importFromSaf("content://b")
      advanceUntilIdle()

      val list = vm.library.first()
      assertThat(list).hasSize(2)
    }
  }
}
