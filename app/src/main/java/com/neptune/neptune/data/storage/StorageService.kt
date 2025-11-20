package com.neptune.neptune.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.sample.Sample
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StorageService(val storage: FirebaseStorage) {
  private val storageRef = storage.reference
  private val sampleRepo = com.neptune.neptune.model.sample.SampleRepositoryProvider.repository

  suspend fun exists(ref: StorageReference): Boolean {
    return try {
      ref.metadata.await()
      true
    } catch (e: StorageException) {
      if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) false else throw e
    }
  }
  // correct -> obtain path from Firestore and not Storage
  @Throws(IOException::class)
  suspend fun downloadZippedSample(
      sample: Sample,
      context: Context,
      onProgress: (Int) -> Unit = {}
  ): File =
      withContext(Dispatchers.IO) {
        val sampleRef = storageRef.child(sample.storageZipPath)
        if (!exists(sampleRef))
            throw IllegalArgumentException(
                "Sample file not found in storage at : ${sample.storageZipPath}")

        val tmp = File(context.cacheDir, "${sample.name}.zip")

        sampleRef
            .getFile(tmp)
            .addOnProgressListener { snapshot ->
              // size in bytes of the file being transferred
              val total = snapshot.totalByteCount
              if (total > 0) {
                // progress percentage
                val progress = (100.0 * snapshot.bytesTransferred / total).toInt().coerceIn(0, 100)
                onProgress(progress)
              }
            }
            .await()
        // ensure progress is marked as complete
        onProgress(100)
        tmp
      }

  /**
   * Unzips [zipFile] into [outputDir] without creating subdirectories. All files are extracted
   * directly into [outputDir]. Validates that the ZIP contains at least one .mp3/.wav and one .json
   * file. Keeps the .zip in [outputDir].
   *
   * @return the .zip file in [outputDir]
   * @throws IllegalArgumentException if no audio or no json file is found
   * @throws java.io.IOException for I/O errors or unsafe paths
   */
  fun persistZipToDownloads(zipFile: File, outputDir: File): File {
    require(zipFile.isFile) { "zipFile must be a file: ${zipFile.path}" }

    var hasAudio = false
    var hasJson = false

    ZipInputStream(FileInputStream(zipFile)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) {
          val lower = entry.name.lowercase()
          if (lower.endsWith(".mp3") || lower.endsWith(".wav")) hasAudio = true
          if (lower.endsWith(".json")) hasJson = true
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    if (!hasAudio || !hasJson)
        throw IllegalArgumentException("Archive missing required files in ${zipFile.name}")
    // Now extract files
    val outFile = File(outputDir, zipFile.name)
    FileInputStream(zipFile).use { input ->
      FileOutputStream(outFile).use { output -> input.copyTo(output) }
    }
    return outFile
  }

  /**
   * Uploads sample zip and image files, cleans up old files, and updates the sample in the repo.
   *
   * @param sample The sample metadata to update.
   * @param localZipUri The local Uri for the .zip file.
   * @param localImageUri The local Uri for the cover image.
   */
  suspend fun uploadSampleFiles(sample: Sample, localZipUri: Uri, localImageUri: Uri) {
    val sampleId = sample.id

    val oldSample: Sample? =
        try {
          sampleRepo.getSample(sampleId)
        } catch (_: Exception) {
          null
        }

    oldSample?.let {
      deleteFileByUrl(it.storageZipPath)
      deleteFileByUrl(it.storageImagePath)
      deleteFileByUrl(it.storagePreviewSamplePath)
    }

    val newStorageZipPath = "samples/${sampleId}.zip"
    val newStorageImagePath = "samples/${sampleId}/${getFileNameFromUri(localImageUri)}"

    coroutineScope {
      val deferredZipUrl = async { uploadFileAndGetUrl(localZipUri, newStorageZipPath) }

      val deferredImageUrl = async { uploadFileAndGetUrl(localImageUri, newStorageImagePath) }

      val newZipUrl = deferredZipUrl.await()
      val newImageUrl = deferredImageUrl.await()

      val finalSample =
          sample.copy(
              storageZipPath = newZipUrl,
              storageImagePath = newImageUrl,
              storagePreviewSamplePath = "")
      sampleRepo.addSample(finalSample)
    }
  }

  /**
   * Uploads a file from a local URI to a specified storage path.
   *
   * @param localUri The URI of the file on the device (e.g., after cropping).
   * @param storagePath The full path in Firebase Storage (e.g., "profile_pictures/userID.jpg").
   * @return The public download URL of the file.
   */
  suspend fun uploadFileAndGetUrl(localUri: Uri, storagePath: String): String {
    try {
      return withContext(Dispatchers.IO) {
        val fileRef = storageRef.child(storagePath)
        fileRef.putFile(localUri).await()
        val downloadUrl = fileRef.downloadUrl.await()
        downloadUrl.toString()
      }
    } catch (e: Exception) {
      throw Exception("Failed to upload file: ${e.message}", e)
    }
  }

  private suspend fun deleteFileByUrl(fileUrl: String) {
    if (fileUrl.isBlank()) return

    try {
      withContext(Dispatchers.IO) {
        val fileRef = storage.getReferenceFromUrl(fileUrl)
        fileRef.delete().await()
      }
    } catch (e: Exception) {
      Log.w("StorageService", "Failed to delete old file: $fileUrl", e)
    }
  }

  /** Retrieves the display name of a file from its Content URI. */
  suspend fun getFileNameFromUri(uri: Uri): String? {
    if (uri.scheme == "content") {
      val contentName =
          withContext(Dispatchers.IO) {
            NepTuneApplication.appContext.contentResolver.query(uri, null, null, null, null)?.use {
                cursor ->
              if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                  cursor.getString(nameIndex)
                } else {
                  null
                }
              } else {
                null
              }
            }
          }
      if (contentName != null) {
        return contentName
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }
  /**
   * Retrieves the download URL of a file from Storage.
   *
   * @param storagePath The full path to the file.
   * @return The download URL, or null in case of an error.
   */
  suspend fun getDownloadUrl(storagePath: String): String? {
    return try {
      val fileRef = storageRef.child(storagePath)
      fileRef.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.w("StorageService", "Failed to get download URL: $storagePath", e)
      null
    }
  }
}
