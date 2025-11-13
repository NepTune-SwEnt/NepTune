package com.neptune.neptune.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for the [ImageStorageRepository].
 *
 * These tests run on the local JVM using Robolectric to simulate the Android environment, allowing
 * access to a real `Context` and file system operations.
 *
 * These tests were created using AI assistance.
 */
@RunWith(AndroidJUnit4::class)
class ImageStorageRepositoryTest {

  private lateinit var context: Context
  private lateinit var repository: ImageStorageRepository
  private lateinit var imagesDir: File

  /**
   * Sets up the test environment before each test.
   *
   * Initializes the context, the repository, and the target images directory. It ensures the
   * directory is clean before each test run.
   */
  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repository = ImageStorageRepository()
    imagesDir = File(context.filesDir, "images")
    if (imagesDir.exists()) {
      imagesDir.deleteRecursively() // Ensure a clean state
    }
    imagesDir.mkdirs()
  }

  /**
   * Cleans up the test environment after each test.
   *
   * Deletes the created image directory and its contents to avoid side effects between tests.
   */
  @After
  fun tearDown() {
    if (imagesDir.exists()) {
      imagesDir.deleteRecursively()
    }
  }

  /** Verifies that `saveImageFromUri` successfully copies a file to the app's internal storage. */
  @Test
  fun saveImageFromUriValidSourceCreatesFileAndSavesContent() = runTest {
    // Arrange: Create a temporary source file with content
    val sourceFile = File.createTempFile("test_source", ".jpg")
    sourceFile.writeText("test data")
    val sourceUri = Uri.fromFile(sourceFile)
    val targetFileName = "test_image.jpg"

    // Act: Save the image
    val savedFile = repository.saveImageFromUri(sourceUri, targetFileName)

    // Assert: Check that the file was created and contains the correct data
    assertThat(savedFile).isNotNull()
    assertThat(savedFile?.name).isEqualTo(targetFileName)
    assertThat(savedFile?.readText()).isEqualTo("test data")
    assertThat(File(imagesDir, targetFileName).exists()).isTrue()
  }

  /** Verifies that `getImageUri` successfully retrieves the URI of a previously saved file. */
  @Test
  fun getImageUriExistingFileReturnsCorrectUri() = runTest {
    // Arrange: Manually create a file in the repository's directory
    val fileName = "existing_image.jpg"
    val file = File(imagesDir, fileName)
    file.writeText("some content")

    // Act: Get the URI for the file
    val retrievedUri = repository.getImageUri(fileName)

    // Assert: Check that the URI is not null and points to the correct file
    assertThat(retrievedUri).isNotNull()
    assertThat(File(retrievedUri!!.path!!) == file)
  }

  /** Verifies that `getImageUri` returns null when the requested file does not exist. */
  @Test
  fun getImageUriNonExistentFileReturnsNull() = runTest {
    // Act: Attempt to get a URI for a file that doesn't exist
    val uri = repository.getImageUri("non_existent_file.jpg")

    // Assert: The result should be null
    assertThat(uri).isNull()
  }

  /**
   * Verifies that `getImageUri` returns null for a file that exists but is empty, as per the
   * implementation's `file.length() > 0` check.
   */
  @Test
  fun getImageUriEmptyFileReturnsNull() = runTest {
    // Arrange: Create an empty file
    val fileName = "empty_image.jpg"
    File(imagesDir, fileName).createNewFile()

    // Act: Attempt to get its URI
    val uri = repository.getImageUri(fileName)

    // Assert: The result should be null
    assertThat(uri).isNull()
  }

  /**
   * Verifies that `saveImageFromUri` returns null if the source URI is invalid or points to a
   * non-existent file.
   */
  @Test
  fun saveImageFromUriInvalidSourceUriReturnsNull() = runTest {
    // Arrange: Create a URI that points to a file that does not exist
    val invalidUri = Uri.fromFile(File("/path/to/non/existent/file.jpg"))

    // Act: Attempt to save from the invalid source
    val result = repository.saveImageFromUri(invalidUri, "some_file.jpg")

    // Assert: The operation should fail and return null
    assertThat(result).isNull()
  }
}
