package com.neptune.neptune.data.storage

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.neptune.neptune.model.sample.Sample
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StorageService(val storage: FirebaseStorage) {
  private val storageRef = storage.reference

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
  suspend fun downloadZippedSample(sample: Sample, context: Context): File =
      withContext(Dispatchers.IO) {
        val sampleRef = storageRef.child(sample.storageZipPath)
        if (!exists(sampleRef))
            throw IllegalArgumentException(
                "Sample file not found in storage at : ${sample.storageZipPath}")

        val tmp = File(context.cacheDir, "${sample.name}.zip")

        sampleRef.getFile(tmp).await()
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
  fun unzipSample(zipFile: File, outputDir: File): File {
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
}
