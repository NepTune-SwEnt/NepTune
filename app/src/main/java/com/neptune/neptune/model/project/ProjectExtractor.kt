package com.neptune.neptune.model.project

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json

/** Utility class to handle extraction and deserialization of a Neptune project (.zip). */
class ProjectExtractor {
  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Extracts the metadata JSON content from the project ZIP file.
   * * @param zipFile The project ZIP file (assumed to be found locally).
   *
   * @return The deserialized SamplerProjectMetadata.
   */
  fun extractMetadata(zipFile: File): SamplerProjectMetadata {
    require(zipFile.exists()) { "Project ZIP file not found: ${zipFile.path}" }

    ZipFile(zipFile).use { zip ->
      val entry: ZipEntry =
          zip.getEntry("config.json")
              ?: throw IllegalArgumentException("config.json not found in ZIP file.")

      zip.getInputStream(entry).use { inputStream ->
        val jsonContent = inputStream.bufferedReader().readText()
        return json.decodeFromString(jsonContent)
      }
    }
  }

  /**
   * Finds the local audio file URI within the extracted ZIP structure. NOTE: This would return a
   * temporary path to the extracted audio file.
   * * @param metadata Metadata containing the audio file names.
   *
   * @param audioFileName The specific audio file name to target.
   * @return A placeholder URI string (in a real app, this would be the extracted file path).
   */
  fun getAudioFileUri(metadata: SamplerProjectMetadata, audioFileName: String): String {
    val fileEntry = metadata.audioFiles.find { it.name == audioFileName }
    require(fileEntry != null) { "Audio file $audioFileName not found in metadata." }
    return "file:///tmp/neptune/extracted/${audioFileName}"
  }
}
