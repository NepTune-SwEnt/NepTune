package com.neptune.neptune.domain.usecase

import android.net.Uri
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.project.ProjectExtractor
import java.io.File

// Backwards-compatible SamplerProvider interface used by tests and earlier code.
// Now loadProjectData is suspend and returns an optional Uri pointing to the extracted audio
// preview (so callers don't need to create or depend on a ViewModel).
interface SamplerProvider {
  suspend fun loadProjectData(zipFilePath: String): Uri?
}

/**
 * Interface representing a service that can load a sampler project and generate an audio preview
 * Uri. Implementations can be provided by the UI layer (eg. a ViewModel) or by a test double.
 */
interface AudioPreviewGenerator : SamplerProvider

/**
 * Default implementation that extracts the audio file from the ZIP and returns its Uri. This avoids
 * creating a ViewModel inside domain code and keeps the operation synchronous from the caller's
 * perspective (it's suspend so it should be called off the main thread when appropriate).
 */
class ViewModelAudioPreviewGenerator : AudioPreviewGenerator {
  override suspend fun loadProjectData(zipFilePath: String): Uri? {
    try {
      val cleanPath = zipFilePath.removePrefix("file:").removePrefix("file://")
      val zipFile = File(cleanPath)
      if (!zipFile.exists()) return null

      val extractor = ProjectExtractor()
      val projectData = extractor.extractMetadata(zipFile)
      val audioFileName = projectData.audioFiles.firstOrNull()?.name ?: return null

      // Extract the audio file into a cache location and return its Uri
      return extractor.extractAudioFile(zipFile, NepTuneApplication.appContext, audioFileName)
    } catch (e: Exception) {
      // Bubbling the exception would also be fine; return null as best-effort behavior
      return null
    }
  }
}
