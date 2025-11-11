package com.neptune.neptune.data.storage

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.model.sample.SampleRepositoryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class StorageService(
    private val storage: FirebaseStorage,
    private val sampleRepo: SampleRepository = SampleRepositoryProvider.repository
) {

  private val storageRef = storage.reference

  suspend fun updateSampleFiles(sample: Sample, localZipUri: Uri, localImageUri: Uri) {
    val sampleId = sample.id
    val localPreviewUri: Uri = Uri.EMPTY // TODO implement the mp3

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

    val newStorageZipPath = "samples/${sampleId}/${getFileNameFromUri(localZipUri)}"
    val newStorageImagePath = "samples/${sampleId}.jpg"
    val newStoragePreviewPath = "" // TODO implement the mp3

    coroutineScope {
      val deferredZipUrl = async { uploadFileAndGetUrl(localZipUri, newStorageZipPath) }

      val deferredImageUrl = async { uploadFileAndGetUrl(localImageUri, newStorageImagePath) }

      val newZipUrl = deferredZipUrl.await()
      val newImageUrl = deferredImageUrl.await()
      // newPreviewUrl = deferredPreviewUrl.await() // TODO implement the mp3

      val finalSample =
          sample.copy(
              storageZipPath = newZipUrl,
              storageImagePath = newImageUrl,
              storagePreviewSamplePath = "" // TODO implement the mp3
              )
      sampleRepo.addSample(finalSample)
    }
  }

  /**
   * Uploads a file from a local URI to a specified storage path.
   *
   * @param localUri The URI of the file on the device (e.g., after cropping).
   * @param storagePath The full path in Firebase Storage (e.g., "profile_pictures/userID.jpg").
   * @return The public download URL of the file.
   *
   * This function was made using AI assistance.
   */
  suspend fun uploadFileAndGetUrl(localUri: Uri, storagePath: String): String {
    try {
      val fileRef = storageRef.child(storagePath)
      fileRef.putFile(localUri).await()
      val downloadUrl = fileRef.downloadUrl.await()
      return downloadUrl.toString()
    } catch (e: Exception) {
      throw Exception("Failed to upload file: ${e.message}", e)
    }
  }

  private suspend fun deleteFileByUrl(fileUrl: String) {
    if (fileUrl.isBlank()) return

    try {
      val fileRef = storage.getReferenceFromUrl(fileUrl)
      fileRef.delete().await()
    } catch (e: Exception) {
      Log.w("StorageService", "Failed to delete old file: $fileUrl", e)
    }
  }

  /** Retrieves the display name of a file from its Content URI. */
  private fun getFileNameFromUri(uri: Uri): String? {
    if (uri.scheme == "content") {
      NepTuneApplication.appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor
        ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            return cursor.getString(nameIndex)
          }
        }
      }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
  }
}
