package com.neptune.neptune.domain.port

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
}
