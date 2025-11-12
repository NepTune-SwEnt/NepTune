package com.neptune.neptune.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.neptune.neptune.model.sample.SampleRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.lang.Exception
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class StorageServiceTest {

  // The dispatcher must be shared by setUp and all runTest blocks
  private val testDispatcher = UnconfinedTestDispatcher()

  // 1. Mock all dependencies
  private lateinit var storage: FirebaseStorage
  private lateinit var sampleRepo: SampleRepository
  private lateinit var storageRef: StorageReference
  private lateinit var fileRef: StorageReference

  // The class we are testing
  private lateinit var storageService: StorageService

  private val localImageUri: Uri = mockk()

  @Before
  fun setUp() {
    // Use an UnconfinedTestDispatcher for simple coroutine testing
    // This makes coroutines run eagerly.
    Dispatchers.setMain(testDispatcher)

    // 2. Create relaxed mocks
    // 'relaxed = true' avoids having to stub every single method.
    storage = mockk(relaxed = true)
    sampleRepo = mockk(relaxed = true)
    storageRef = mockk(relaxed = true)
    fileRef = mockk(relaxed = true)

    // 3. Initialize the service with mocks
    storageService = StorageService(storage, sampleRepo)

    // 4. Define behavior for the mocked dependencies

    // Mock the chain: storage.reference.child(path) -> fileRef
    every { storage.reference } returns storageRef
    every { storageRef.child(any()) } returns fileRef

    // --- Mocking the tricky getFileNameFromUri ---
    // This private method uses Android's ContentResolver, which is hard to test.
    // A simple workaround is to mock the Uris to avoid the "content://" scheme,
    // so it uses the 'lastPathSegment' fallback.
    every { localImageUri.scheme } returns "file" // Not "content"
    every { localImageUri.lastPathSegment } returns "test_image.png"
  }

    @Test
    fun `uploadFileAndGetUrl - success - returns download URL`() =
        runTest(testDispatcher) {
            // --- Arrange ---
            val testUri: Uri = mockk()
            val testPath = "samples/test.zip"
            val expectedUrl = "http://fake.storage.com/samples/test.zip"

            // --- Mocking step-by-step (more robust) ---

            // 1. Mock: fileRef.putFile(testUri) -> returns UploadTask
            val mockUploadTask: UploadTask = mockk() // 'relaxed' n'est pas nécessaire
            every { fileRef.putFile(testUri) } returns mockUploadTask


            // --- C'EST LA LIGNE MANQUANTE ---
            // 2. Mockez le await() sur la tâche d'upload
            coEvery { mockUploadTask.await() } returns mockk() // Ceci débloque le test
            // --- FIN DE LA CORRECTION ---

            // 3. Mock: fileRef.downloadUrl -> returns Task<Uri>
            val mockDownloadUriTask: com.google.android.gms.tasks.Task<Uri> = mockk()
            val mockDownloadUri: Uri = mockk()
            every { mockDownloadUri.toString() } returns expectedUrl
            every { fileRef.downloadUrl } returns mockDownloadUriTask

            // 4. Mock: mockDownloadUriTask.await() -> returns Uri
            coEvery { mockDownloadUriTask.await() } returns mockDownloadUri

            // --- Act ---
            val resultUrl = storageService.uploadFileAndGetUrl(testUri, testPath)

            // --- Assert ---
            assertEquals(expectedUrl, resultUrl)
            verify { storageRef.child(testPath) }
            verify { fileRef.putFile(testUri) }
        }

  @Test
  fun `uploadFileAndGetUrl - failure - throws exception`() =
      runTest(testDispatcher) {
        // --- Arrange ---
        val testUri: Uri = mockk()
        val testPath = "samples/test.zip"
        val exception = Exception("Upload failed")

        // --- Mocking step-by-step (THE FIX) ---

        // 1. Mock: fileRef.putFile(testUri) -> returns a task
        val mockUploadTask: UploadTask = mockk() // Not relaxed
        every { fileRef.putFile(testUri) } returns mockUploadTask

        // 2. Mock: task.await() -> throws exception
        coEvery { mockUploadTask.await() } throws exception

        // --- Act & Assert ---
        try {
          storageService.uploadFileAndGetUrl(testUri, testPath)
          Assert.fail(
              "Should have thrown an exception") // This line fails the test if no exception is
                                                 // thrown
        } catch (e: Exception) {
          // Success
          Assert.assertEquals("Failed to upload file: Upload failed", e.message)
        }
      }
}
