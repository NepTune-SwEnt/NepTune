package com.neptune.neptune.data.storage

import android.net.Uri
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
    storageService = StorageService(Firebase.storage, sampleRepo)

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
        val downloadUrl = storageService.uploadFileAndGetUrl(testUri, testPath)

        // --- Assert ---
        Assert.assertNotNull(downloadUrl)
        Assert.assertTrue(downloadUrl.contains("test.txt"))
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
          storageService.uploadFileAndGetUrl(badUri, testPath)
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
        val newImageName = "new_image.png"

        // 1. Create an old file to be deleted
        val oldFileUri = createDummyFile(oldZipName, "old zip data")
        val oldUrl = storageService.uploadFileAndGetUrl(oldFileUri, "samples/$oldZipName")

        // 2. Create the old Sample in Firestore
        val oldSample =
            Sample(
                id = sampleId,
                name = "Old Sample",
                storageZipPath = oldUrl, // The URL of the file to delete
                storageImagePath = "",
                storagePreviewSamplePath = "",
                description = "",
                durationSeconds = 0,
                tags = emptyList(),
                likes = 0,
                usersLike = emptyList(),
                comments = 0,
                downloads = 0)
        sampleRepo.addSample(oldSample)

        // 3. Create the NEW local files
        val newZipUri = createDummyFile("new_sample.zip", "new zip data")
        val newImageUri = createDummyFile(newImageName, "new image data")

        // --- Act ---
        val sampleWithNewData = oldSample.copy(name = "New Sample Name")
        storageService.uploadSampleFiles(sampleWithNewData, newZipUri, newImageUri)

        // --- Assert ---

        // 1. Verify the old file is deleted
        try {
          Firebase.storage.getReferenceFromUrl(oldUrl).metadata.await()
          Assert.fail("Old file still exists, should have been deleted")
        } catch (_: Exception) {
          // Success, file no longer exists
        }

        // 2. Verify the Sample in Firestore was updated
        val updatedSample = sampleRepo.getSample(sampleId)
        Assert.assertNotNull(updatedSample)
        Assert.assertEquals("New Sample Name", updatedSample.name)

        // 3. Verify the new paths are correct
        // Check that the encoded path (%2F instead of /) is in the final URL
        Assert.assertTrue(updatedSample.storageZipPath.contains("samples%2F${sampleId}.zip"))
        Assert.assertTrue(
            updatedSample.storageImagePath.contains("samples%2F${sampleId}%2F$newImageName"))
      }
}
