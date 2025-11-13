package com.neptune.neptune.domain.port

import java.io.File
import java.net.URI

// Port interface for importing files from external URIs into the app's local storage
interface FileImporter {
  data class ImportedFile(
      val displayName: String,
      val mimeType: String?,
      val sizeBytes: Long?,
      val sourceUri: URI,
      val localUri: URI,
      val durationMs: Long?
  )

  suspend fun importFile(sourceUri: URI): ImportedFile

  // Import a file created by the in-app recorder (File on disk). Implementations may
  // move/rename the file into the app workspace and return metadata similar to importFile.
  suspend fun importRecorded(file: File): ImportedFile
}
