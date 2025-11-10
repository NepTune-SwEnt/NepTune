package com.neptune.neptune.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageService(private val storage: FirebaseStorage) {
  private val storageRef = storage.reference

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
}
