package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.utils.MainDispatcherRule
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.zip.ZipFile
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/* fakes */
private class FakeRepo : MediaRepository {
  private val flow = MutableStateFlow<List<MediaItem>>(emptyList())

  override fun observeAll(): Flow<List<MediaItem>> = flow

  override suspend fun upsert(item: MediaItem) {
    flow.value = flow.value + item
  }

  override suspend fun delete(item: MediaItem) {
    flow.value = flow.value - item
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

  override suspend fun importRecorded(file: File): FileImporter.ImportedFile {
    throw NotImplementedError("Not needed for these tests")
  }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule() // sets Dispatchers.Main to a TestDispatcher
  private val testDispatcher
    get() = mainRule.dispatcher

  private lateinit var vm: ImportViewModel
  private lateinit var repo: FakeRepo
  private lateinit var importer: FakeImporter

  @Before
  fun setUp() {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    repo = FakeRepo()
    importer = FakeImporter(ctx.cacheDir)

    // inject the SAME test dispatcher into NeptunePackager, so IO is controlled by runTest
    val packager = NeptunePackager(StoragePaths(ctx), io = testDispatcher)

    val importUC = ImportMediaUseCase(importer, repo, packager)
    val libraryUC = GetLibraryUseCase(repo)
    vm = ImportViewModel(importUC, libraryUC)
  }

  @Test
  fun importUpdatesListWithZipProject() =
      runTest(testDispatcher) {
        val before = vm.library.first()
        assertThat(before).isEmpty()

        vm.importFromSaf("content://any-audio")
        advanceUntilIdle() // drains viewModelScope + IO (because both use testDispatcher)

        val after = vm.library.first()
        assertThat(after).hasSize(1)
        val item = after.first()
        assertThat(item.projectUri).endsWith(".zip")

        val zipFile = File(URI(item.projectUri))
        val names = ZipFile(zipFile).use { z -> z.entries().toList().map { it.name } }
        assertThat(names).containsAtLeast("config.json", "picked.wav")
      }

  @Test
  fun multipleImportsAppendList() =
      runTest(testDispatcher) {
        vm.importFromSaf("content://a")
        vm.importFromSaf("content://b")
        advanceUntilIdle()
        val list =
            vm.library
                .drop(1) // Skips the initial empty list []
                .first { it.size == 2 } // Waits until the state is updated to contain 2 items

        // advanceUntilIdle() is still fine, but the above line does the heavy lifting
        advanceUntilIdle()

        assertThat(list).hasSize(2)
      }

  private class NotImportViewModel : ViewModel()

  @Test
  fun create_unknownViewModelClass_throwsIllegalStateException() {
    val importUC = mockk<ImportMediaUseCase>(relaxed = true)
    val libraryUC = mockk<GetLibraryUseCase>(relaxed = true)
    val factory = ImportVMFactory(importUC, libraryUC)

    val ex =
        assertFailsWith<IllegalStateException> { factory.create(NotImportViewModel::class.java) }
    assertThat(ex).hasMessageThat().isEqualTo("Unknown ViewModel class")
  }
}
