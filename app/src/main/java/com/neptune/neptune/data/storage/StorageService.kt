package com.neptune.neptune.data.storage

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.model.sample.Sample
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

const val PROCESSED_AUDIO = "processed_audios"

open class StorageService(
    val storage: FirebaseStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  private val storageRef = storage.reference
  private val sampleRepo = com.neptune.neptune.model.sample.SampleRepositoryProvider.repository

  suspend fun exists(ref: StorageReference): Boolean {
    return try {
      ref.metadata.await()
      true
    } catch (e: StorageException) {
      if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) {
        throw e
      }
      false
    }
  }
  // correct -> obtain path from Firestore and not Storage
  @Throws(IOException::class)
  open suspend fun downloadZippedSample(
      sample: Sample,
      context: Context,
      onProgress: (Int) -> Unit = {}
  ): File {
    val tmp = File(context.cacheDir, "${sample.name}.zip")
    return downloadToFile(sample.storageZipPath, tmp, onProgress)
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
  open suspend fun persistZipToDownloads(zipFile: File, outputDir: File): File {
    return withContext(ioDispatcher) {
      require(zipFile.isFile) { "zipFile must be a file: ${zipFile.path}" }
      require(checkZipContainsRequiredFiles(zipFile)) {
        "Archive missing required files in ${zipFile.name}"
      }

      // Now extract files
      Log.d("StorageService", "Extracting zip to $outputDir")
      val outFile = File(outputDir, zipFile.name)
      FileInputStream(zipFile).use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
      }
      outFile
    }
  }

  private fun checkZipContainsRequiredFiles(zipFile: File): Boolean {
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
    return hasAudio && hasJson
  }

  /**
   * Uploads sample zip and image files, cleans up old files, and updates the sample in the repo.
   *
   * @param sample The sample metadata to update.
   * @param localZipUri The local Uri for the .zip file.
   * @param localImageUri The local Uri for the cover image.
   */
  suspend fun uploadSampleFiles(
      sample: Sample,
      localZipUri: Uri,
      localImageUri: Uri?,
      localProcessedUri: Uri?
  ) {
    val sampleId = sample.id

    val oldSample: Sample? =
        try {
          sampleRepo.getSample(sampleId)
        } catch (_: Exception) {
          null
        }

    oldSample?.let {
      deleteFileByPath(it.storageZipPath)
      deleteFileByPath(it.storageImagePath)
      deleteFileByPath(it.storageProcessedSamplePath)
    }

    val newStorageZipPath = "samples/${sampleId}.zip"
    val newStorageImagePath =
        if (localImageUri != null) "sample_image/${sampleId}/${getFileNameFromUri(localImageUri)}"
        else ""
    val newProcessedAudioPath =
        if (localProcessedUri != null) {
          "${PROCESSED_AUDIO}/${sampleId}.wav"
        } else {
          ""
        }
    coroutineScope {
      val deferredZip = async { uploadFile(localZipUri, newStorageZipPath) }

      val deferredImage =
          if (localImageUri != null && newStorageImagePath.isNotEmpty())
              async { uploadFile(localImageUri, newStorageImagePath) }
          else null

      val deferredProcessed =
          if (localProcessedUri != null && newProcessedAudioPath.isNotEmpty()) {
            async { uploadFile(localProcessedUri, newProcessedAudioPath) }
          } else {
            null
          }
      deferredZip.await()
      deferredImage?.await()
      deferredProcessed?.await()
      val finalSample =
          sample.copy(
              storageZipPath = newStorageZipPath,
              storageImagePath = newStorageImagePath,
              storageProcessedSamplePath = newProcessedAudioPath)
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
  suspend fun uploadFile(localUri: Uri, storagePath: String) {
    try {
      withContext(ioDispatcher) {
        val fileRef = storageRef.child(storagePath)
        fileRef.putFile(localUri).await()
      }
    } catch (e: Exception) {
      throw Exception("Failed to upload file to $storagePath: ${e.message}", e)
    }
  }

  private suspend fun deleteFileByPath(storagePath: String) {
    if (storagePath.isBlank()) return

    try {
      withContext(ioDispatcher) {
        val fileRef = storageRef.child(storagePath)
        fileRef.delete().await()
      }
    } catch (e: Exception) {
      Log.w("StorageService", "Failed to delete old file: $storagePath", e)
    }
  }

  /** Retrieves the display name of a file from its Content URI. */
  suspend fun getFileNameFromUri(uri: Uri): String? {
    if (uri.scheme == "content") {
      val contentName =
          withContext(ioDispatcher) {
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
  open suspend fun getDownloadUrl(storagePath: String): String? {
    if (storagePath.isBlank()) return null
    return try {
      val fileRef = storageRef.child(storagePath)
      fileRef.downloadUrl.await().toString()
    } catch (e: Exception) {
      Log.w("StorageService", "Failed to get download URL: $storagePath", e)
      null
    }
  }

  /**
   * Attempts to read the duration from the JSON. If it is 0, extracts the audio to measure it.
   *
   * This function is temporally using the duration of the audio without his effects. It should be
   * improved in the future to calculate with the effects applied.
   */
  suspend fun getProjectDuration(zipUri: Uri?): Long {
    if (zipUri == null) return 0

    return withContext(ioDispatcher) {
      try {
        val context = NepTuneApplication.appContext
        val zipFile = File(zipUri.path ?: "")
        if (!zipFile.exists()) return@withContext 0

        val extractor = ProjectExtractor()

        val metadata = extractor.extractMetadata(zipFile)
        val firstAudio = metadata.audioFiles.firstOrNull()

        val durationFromMeta = firstAudio?.durationSeconds ?: 0f
        if (durationFromMeta > 0.1f) {
          return@withContext durationFromMeta.toLong()
        }

        Log.w(
            "StorageService", "Duration not found in the JSON, calculated via audio extraction...")

        val audioFileName = firstAudio?.name ?: return@withContext 0
        val tempAudioUri = extractor.extractAudioFile(zipFile, context, audioFileName)

        val realDuration = getAudioDuration(context, tempAudioUri)
        var path = tempAudioUri.path
        if (path == null) {
          path = ""
          Log.w("StorageService", "Failed to calculate project duration")
        }

        if (!File(path).delete()) {
          Log.e("StorageService", "Temporary audio file could not be deleted: $path")
        }

        return@withContext realDuration
      } catch (e: Exception) {
        Log.e("StorageService", "Failed to calculate project duration", e)
        0
      }
    }
  }

  private suspend fun downloadToFile(
      storagePath: String,
      outFile: File,
      onProgress: (Int) -> Unit = {}
  ): File =
      withContext(ioDispatcher) {
        require(storagePath.isNotBlank()) { "storagePath is blank" }

        val ref = storageRef.child(storagePath)
        require(exists(ref)) { "File not found in storage at: $storagePath" }

        ref.getFile(outFile)
            .addOnProgressListener { snap ->
              val total = snap.totalByteCount
              if (total > 0) {
                val p = (100.0 * snap.bytesTransferred / total).toInt().coerceIn(0, 100)
                onProgress(p)
              }
            }
            .await()

        onProgress(100)
        outFile
      }

  /**
   * Download a file given its path
   *
   * @param outFile The path where we save
   * @param onProgress for the download bar
   */
  suspend fun downloadFileByPath(
      storagePath: String,
      outFile: File,
      onProgress: (Int) -> Unit = {}
  ): File = downloadToFile(storagePath, outFile, onProgress)

  /** Helper to retrieve the duration of an audio file (mp3/wav) via MediaMetadataRetriever */
  private suspend fun getAudioDuration(context: Context, audioUri: Uri): Long {
    return withContext(ioDispatcher) {
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(context, audioUri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        time?.toLongOrNull() ?: 0L
      } catch (e: Exception) {
        Log.e("StorageService", "Failed to retrieve audio duration", e)
        0
      } finally {
        retriever.release()
      }
    }
  }
}
