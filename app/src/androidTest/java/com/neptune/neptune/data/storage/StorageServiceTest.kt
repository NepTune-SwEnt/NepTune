package com.neptune.neptune.data.storage

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// This class was made using AI assistance.
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class StorageServiceTest {

  private val testDispatcher = Dispatchers.IO

  // Real (emulated) services
  private lateinit var storageService: StorageService
  private lateinit var sampleRepo: SampleRepository // This is an interface

  // App context from the emulator
  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Before
  fun setUp() {
    // --- 1. Connect to Emulators ---
    // "10.0.2.2" is the magic IP for "localhost" from the Android emulator
    // We configure the GLOBAL Firebase instances
    try {
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
      Firebase.storage.useEmulator("10.0.2.2", 9199)
      Firebase.auth.useEmulator("10.0.2.2", 9099)
    } catch (_: IllegalStateException) {
      // Emulators are likely already connected, which is fine.
    }

    // --- 2. Initialize real services ---
    // We don't create SampleRepository, we get it from the Provider.
    // The Provider will automatically use the emulated Firebase.firestore.
    sampleRepo = SampleRepositoryProvider.repository

    // Initialize our service with the GLOBAL emulated Storage instance
    storageService = StorageService(Firebase.storage)

    // --- 3. Authenticate ---
    runBlocking { Firebase.auth.signInAnonymously().await() }
  }

  @After
  fun tearDown() = runBlocking {
    // --- 4. Clean up emulators after each test ---

    // Clear Storage
    val storageRef = Firebase.storage.reference
    val allFiles = storageRef.listAll().await()
    allFiles.items.forEach { it.delete().await() }
    allFiles.prefixes.forEach { ref ->
      val folderItems = ref.listAll().await()
      folderItems.items.forEach { it.delete().await() }
    }

    // Clear Firestore
    val firestore = Firebase.firestore
    val allSamples = firestore.collection("samples").get().await()
    allSamples.documents.forEach { it.reference.delete() }

    // Sign out
    Firebase.auth.signOut()
  }

  /** Creates a dummy file in the app's cache dir on the emulator and returns its Uri. */
  private fun createDummyFile(fileName: String, content: String): Uri {
    val file = File(context.cacheDir, fileName)
    file.writeText(content)
    file.deleteOnExit() // Clean up the local file
    return Uri.fromFile(file)
  }

  @Test
  fun uploadFileAndGetUrlSuccessReturnsDownloadURL() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val testUri = createDummyFile("test.txt", "hello world")
        val testPath = "uploads/test.txt"

        // --- Act ---
        storageService.uploadFile(testUri, testPath)
        val downloadUrl = storageService.getDownloadUrl(testPath)

        // --- Assert ---
        Assert.assertNotNull(downloadUrl)
        Assert.assertTrue(downloadUrl!!.contains("test.txt"))
        Assert.assertTrue(downloadUrl.contains(Firebase.storage.reference.bucket))
      }

  @Test
  fun uploadFileAndGetUrlFailureThrowsException() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val badUri = Uri.parse("file:///non-existent-file-${UUID.randomUUID()}.txt")
        val testPath = "uploads/bad.txt"

        // --- Act & Assert ---
        try {
          storageService.uploadFile(badUri, testPath)
          Assert.fail("Should have thrown an exception")
        } catch (e: Exception) {
          // Success
          Assert.assertNotNull(e.message)
          Assert.assertTrue(e.message!!.contains("Failed to upload file"))
        }
      }

  @Test
  fun uploadSampleFilesDeletesOldUploadsNewAndUpdatesRepo() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val sampleId = "sample-test-123"
        val oldZipName = "old_file.zip"
        val oldZipPath = "samples/$oldZipName"
        val newImageName = "new_image.png"

        val oldFileUri = createDummyFile(oldZipName, "old zip data")
        storageService.uploadFile(oldFileUri, oldZipPath)

        Assert.assertNotNull(storageService.getDownloadUrl(oldZipPath))

        val oldSample =
            Sample(
                id = sampleId,
                name = "Old Sample",
                storageZipPath = oldZipPath,
                storageImagePath = "",
                storagePreviewSamplePath = "",
                description = "",
                durationMillis = 0,
                tags = emptyList(),
                likes = 0,
                usersLike = emptyList(),
                comments = 0,
                downloads = 0)
        sampleRepo.addSample(oldSample)

        val newZipUri = createDummyFile("new_sample.zip", "new zip data")
        val newImageUri = createDummyFile(newImageName, "new image data")
        val processedUri = createDummyFile("new_sample.wav", "new wav data")

        // --- Act ---
        val sampleWithNewData = oldSample.copy(name = "New Sample Name")
        storageService.uploadSampleFiles(sampleWithNewData, newZipUri, newImageUri, processedUri)

        // --- Assert ---

        // 1. Verify the old file is deleted using its PATH
        try {
          Firebase.storage.reference.child(oldZipPath).metadata.await()
          Assert.fail("Old file still exists, should have been deleted")
        } catch (_: Exception) {
          // Success, file no longer exists (StorageException)
        }

        val updatedSample = sampleRepo.getSample(sampleId)
        Assert.assertNotNull(updatedSample)
        Assert.assertEquals("New Sample Name", updatedSample.name)

        Assert.assertEquals("samples/$sampleId.zip", updatedSample.storageZipPath)
        Assert.assertEquals("sample_image/$sampleId/$newImageName", updatedSample.storageImagePath)
      }

  @Test
  fun getDownloadUrlWhenFileExistsReturnsCorrectUrl() =
      runBlocking(testDispatcher) {
        val testUri = createDummyFile("test-download.txt", "hello")
        val testPath = "public/test-download.txt"
        storageService.uploadFile(testUri, testPath)
        val actualUrl = storageService.getDownloadUrl(testPath)
        Assert.assertNotNull(actualUrl)
        Assert.assertTrue(actualUrl!!.contains("test-download.txt"))
      }

  @Test
  fun getDownloadUrlWhenFileDoesNotExistReturnsNull() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val nonExistentPath = "folder/does-not-exist-${UUID.randomUUID()}.txt"

        // --- Act ---
        val resultUrl = storageService.getDownloadUrl(nonExistentPath)

        // --- Assert ---
        Assert.assertNull(resultUrl)
      }

  @Test
  fun getFileNameFromUriWithFileUriReturnsLastPathSegment() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val fileUri = createDummyFile("test-file-name.jpg", "hello")

        // --- Act ---
        val fileName = storageService.getFileNameFromUri(fileUri)

        // --- Assert ---
        Assert.assertEquals("test-file-name.jpg", fileName)
      }

  @Test
  fun getFileNameFromUriWithHttpUri_returnsLastPathSegment() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val httpUri = Uri.parse("https://example.com/some/path/on/web/image.png?v=123")

        // --- Act ---
        val fileName = storageService.getFileNameFromUri(httpUri)

        // --- Assert ---
        Assert.assertEquals("image.png", fileName)
      }

  @Test
  fun downloadFileByPathWhenFileExistsDownloadsToOutFileAndReturnsIt() {
    runBlocking(testDispatcher) {
      val storagePath = "processed/test-audio.txt"
      val content = "hello processed audio"
      val srcUri = createDummyFile("src-processed.txt", content)

      storageService.uploadFile(srcUri, storagePath)

      val outFile = File(context.cacheDir, "downloaded-processed.txt")
      if (outFile.exists()) outFile.delete()

      val progressEvents = mutableListOf<Int>()

      val returned =
          storageService.downloadFileByPath(storagePath, outFile) { p -> progressEvents.add(p) }

      Assert.assertEquals(outFile.absolutePath, returned.absolutePath)
      Assert.assertTrue(outFile.exists())
      Assert.assertEquals(content, outFile.readText())

      Assert.assertTrue(progressEvents.isNotEmpty())
      Assert.assertTrue(progressEvents.contains(100))

      outFile.delete()
    }
  }

  @Test
  fun downloadFileByPathWithBlankPathThrowsIllegalArgumentException() {
    runBlocking(testDispatcher) {
      val outFile = File(context.cacheDir, "x.txt")

      try {
        storageService.downloadFileByPath("   ", outFile) {}
        Assert.fail("Should have thrown IllegalArgumentException")
      } catch (e: IllegalArgumentException) {
        Assert.assertTrue(e.message?.contains("storagePath is blank") == true)
      }
    }
  }

  @Test
  fun downloadFileByPathWhenFileDoesNotExistThrowsIllegalArgumentException() {
    runBlocking(testDispatcher) {
      val missingPath = "processed/missing-${UUID.randomUUID()}.wav"
      val outFile = File(context.cacheDir, "missing.wav")
      if (outFile.exists()) outFile.delete()

      try {
        storageService.downloadFileByPath(missingPath, outFile) {}
        Assert.fail("Should have thrown IllegalArgumentException")
      } catch (e: IllegalArgumentException) {
        Assert.assertTrue(e.message?.contains("File not found in storage") == true)
        Assert.assertTrue(e.message?.contains(missingPath) == true)
      }
    }
  }

  @Test
  fun downloadFileByPathProgressIsNonDecreasingAndEndsAt100() {
    runBlocking(testDispatcher) {
      val storagePath = "processed/large-${UUID.randomUUID()}.bin"
      val bigContent = "a".repeat(300_000)
      val srcUri = createDummyFile("large.bin", bigContent)
      storageService.uploadFile(srcUri, storagePath)

      val outFile = File(context.cacheDir, "large-downloaded.bin")
      if (outFile.exists()) outFile.delete()

      val progressEvents = mutableListOf<Int>()

      storageService.downloadFileByPath(storagePath, outFile) { p -> progressEvents.add(p) }

      Assert.assertTrue(outFile.exists())
      Assert.assertEquals(bigContent.length, outFile.readText().length)

      Assert.assertTrue(progressEvents.isNotEmpty())
      Assert.assertTrue(progressEvents.contains(100))

      for (i in 1 until progressEvents.size) {
        Assert.assertTrue(progressEvents[i] >= progressEvents[i - 1])
      }

      outFile.delete()
    }
  }

  @Test
  fun getFileNameFromUriWithContentUriQueriesContentResolver() =
      runBlocking(testDispatcher) {
        // --- Arrange ---
        val testFileName = "my-content-file.mp3"
        val sharedDir = File(context.cacheDir, "images_to_share")
        sharedDir.mkdirs()
        val testFile = File(sharedDir, testFileName)
        try {
          testFile.writeText("dummy audio data")
        } catch (e: IOException) {
          Assert.fail("Failed to create test file: ${e.message}")
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri

        try {
          contentUri = FileProvider.getUriForFile(context, authority, testFile)
        } catch (e: Exception) {
          Assert.fail("FileProvider not set up in AndroidManifest.xml? ${e.message}")
          return@runBlocking
        }

        // --- Act ---
        val fileName = storageService.getFileNameFromUri(contentUri)

        // --- Assert ---
        Assert.assertEquals(testFileName, fileName)

        testFile.delete()
      }
}
