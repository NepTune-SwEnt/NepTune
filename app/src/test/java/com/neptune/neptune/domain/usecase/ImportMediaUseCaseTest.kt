package com.neptune.neptune.domain.usecase

import android.content.Context
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
}

@RunWith(RobolectricTestRunner::class)
class ImportMediaUseCaseTest {
  @Test
  fun pipeline_produces_zip_and_repository_emits() = runBlocking {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val paths = StoragePaths(ctx)
    val packager = NeptunePackager(paths)
    val repo = FakeRepo()
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
}
