package com.neptune.neptune.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A generic repository for saving and loading images in the application's internal storage.
 *
 * This class was made using AI assistance
 */
class ImageStorageRepository(private val context: Context) {
  private val ioDispatcher = Dispatchers.IO

  // A dedicated folder for our images in internal storage
  private val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

  /**
   * Copies an image from a source URI (e.g., Ucrop) to a file permanent in the app's internal
   * storage.
   *
   * @param sourceUri The URI of the temporary file to copy.
   * @param targetFileName The desired filename.
   * @return The saved file, or null if the copy fails.
   */
  suspend fun saveImageFromUri(sourceUri: Uri, targetFileName: String): File? {
    return withContext(ioDispatcher) {
      try {
        val targetFile = File(imagesDir, targetFileName)
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
          FileOutputStream(targetFile).use { outputStream -> inputStream.copyTo(outputStream) }
        }
        targetFile
      } catch (_: Exception) {
        null
      }
    }
  }

  /**
   * Retrieves the URI of an image file if it exists in our storage.
   *
   * @param fileName The name of the file to search for.
   * @return The URI (ready for Coil) if the file exists, otherwise null.
   */
  suspend fun getImageUri(fileName: String): Uri? {
    return withContext(ioDispatcher) {
      try {
        val file = File(imagesDir, fileName)
        if (file.exists() && file.length() > 0) {
          Uri.fromFile(file)
        } else {
          null
        }
      } catch (_: Exception) {
        null
      }
    }
  }
}
