package com.neptune.neptune

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.data.MediaRepositoryImpl
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.data.StoragePaths
import com.neptune.neptune.data.local.MediaDb
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.fakes.FakeFileImporter
import java.io.File
import java.net.URI
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImportPipelineTest {

  private lateinit var context: Context
  private lateinit var db: MediaDb
  private lateinit var repo: MediaRepository
  private lateinit var paths: StoragePaths
  private lateinit var packager: NeptunePackager
  private lateinit var importer: FakeFileImporter
  private lateinit var importUC: ImportMediaUseCase
  private lateinit var libraryUC: GetLibraryUseCase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    // In-memory DB for testing
    db = Room.inMemoryDatabaseBuilder(context, MediaDb::class.java).allowMainThreadQueries().build()
    repo = MediaRepositoryImpl(db.mediaDao())
    paths = StoragePaths(context)
    packager = NeptunePackager(paths)
    importer = FakeFileImporter(context)
    importUC = ImportMediaUseCase(importer, repo, packager)
    libraryUC = GetLibraryUseCase(repo)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun import_createsNeptuneZip_andStoresOnlyProjectUri() = runBlocking {
    // WHEN: we run the import use case with any URI (unused by fake)
    val item = importUC(URI("file://dummy").toString())

    // THEN: .neptune exists
    val project = File(URI(item.projectUri))
    assertTrue("Project .zip file should exist", project.exists())
    assertEquals("zip", project.extension)

    // AND: zip contains config.json + an audio file
    val entries = ZipFile(project).use { zf -> zf.entries().toList().map { it.name }.toSet() }
    assertTrue("config.json missing from zip", "config.json" in entries)
    assertTrue(
        "audio file missing from zip", entries.any { it.endsWith(".wav") || it.endsWith(".mp3") })

    // AND: repository stores only id + projectUri (no other metadata fields exist now)
    val items = libraryUC().first()
    assertEquals(1, items.size)
    assertEquals(item.id, items[0].id)
    assertEquals(item.projectUri, items[0].projectUri)
  }

  @Test
  fun import_deletesTempAudioCopy_ifUseCaseConfiguredToDoSo() = runBlocking {
    // The fake importer writes to cacheDir. After import, ImportMediaUseCase deletes the temp
    // audio.
    val probeUri = URI("file://dummy")
    val probe = importer.importFile(probeUri) // create the temp file the same way use case sees it
    val tempAudio = File(URI(probe.localUri.toString()))
    assertTrue("Precondition failed: temp audio not created", tempAudio.exists())

    // Now do the real import (it will run packager and delete the temp)
    importUC(probeUri.toString())

    // The temp audio should be gone (use case calls delete())
    assertFalse("Temp audio should be deleted after packaging", tempAudio.exists())
  }
}
