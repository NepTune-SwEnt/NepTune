package com.neptune.neptune.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/*
   Fakes with minimal implementation to support ImportViewModelTest.
   More extensive tests of the individual components are in their own
   test files (NeptunePackagerTest, FileImporterImplCoverageTest, etc).
   Written with help from ChatGPT.
*/
private class FakeRepo2 : MediaRepository {
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
          outputStream().use { FileOutputStream(this).write(ByteArray(64) { 0x55 }) }
        }
    return FileImporter.ImportedFile(
        displayName = f.name,
        mimeType = "audio/wav",
        sourceUri = sourceUri,
        localUri = f.toURI(),
        sizeBytes = f.length(),
        durationMs = 1200L)
  }

  override suspend fun importRecorded(file: File): FileImporter.ImportedFile {
    throw NotImplementedError("importRecorded not implemented in FakeImporter")
  }
}

@RunWith(RobolectricTestRunner::class)
class ImportMediaUseCaseTest {
  @Test
  fun pipelineProducesZipAndRepositoryEmits() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val packager = NeptunePackager(paths)
    val repo = FakeRepo2()
    val importer = FakeImporter(ctx.cacheDir)
    val uc = ImportMediaUseCase(importer, repo, packager)

    val item = uc("content://picked")
    val zip = File(URI(item.projectUri))
    assertTrue(zip.exists())
    assertEquals("zip", zip.extension)

    val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name } }
    assertTrue(names.contains("config.json"))
    assertTrue(names.contains("picked.wav"))

    val fromRepo = repo.observeAll().first()
    assertEquals(1, fromRepo.size)
    assertEquals(item.projectUri, fromRepo.first().projectUri)
  }

  @Test
  fun finalizeImport_createsAudioPreviewAndStoresIt() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val packager = NeptunePackager(paths)
    val repo = FakeRepo2()
    val importer = FakeImporter(ctx.cacheDir)

    // Create a temp preview file that the fake SamplerViewModel will return
    val previewFile = File(ctx.cacheDir, "temp_preview.mp3").apply { writeText("preview") }
    val fakeProvider =
        object : SamplerProvider {
          override fun loadProjectData(zipFilePath: String) {
            // no-op
          }

          override suspend fun audioBuilding(): Uri? {
            return Uri.fromFile(previewFile)
          }
        }

    // Subclass the usecase to inject the fake SamplerProvider
    val uc =
        object : ImportMediaUseCase(importer, repo, packager) {
          override fun createSamplerProvider(): SamplerProvider = fakeProvider
        }

    val item = uc("content://picked")

    // Check that previews dir contains a file for the item id
    val previewsDir = File(ctx.filesDir, "previews")
    assertTrue(previewsDir.exists())
    val files = previewsDir.listFiles() ?: emptyArray()
    assertTrue(files.isNotEmpty())

    // Check the project entry was created with audioPreviewLocalPath
    // ProjectItemsRepositoryLocal saves projects to app files dir's projects.json
    val projectsRepo = com.neptune.neptune.model.project.ProjectItemsRepositoryLocal(ctx)
    val project = projectsRepo.findProjectWithProjectFile(item.projectUri)
    assertTrue(project.audioPreviewLocalPath?.isNotBlank() == true)

    // The preview file path should point to a file inside previewsDir
    val previewPath = project.audioPreviewLocalPath!!
    val previewFileSaved = File(URI(previewPath))
    assertTrue(previewFileSaved.exists())
  }
}
