package com.neptune.neptune.domain.usecase

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.neptune.neptune.NepTuneApplication.Companion.appContext
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.media.AudioPreviewGenerator
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import com.neptune.neptune.ui.sampler.SamplerViewModel
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID

open class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager,
    // factory used to obtain fresh AudioPreviewGenerator instances (kept default for
    // backward-compatibility)
    private val audioPreviewGeneratorFactory: () -> AudioPreviewGenerator = { SamplerViewModel() }
) {
  suspend operator fun invoke(sourceUriString: String): MediaItem {
    val probe = importer.importFile(URI(sourceUriString))
    return invokeImplementation(probe)
  }

  // Overload for a File created by the in-app recorder. Uses importer.importRecorded
  suspend operator fun invoke(recordedFile: File): MediaItem {
    val probe = importer.importRecorded(recordedFile)
    return invokeImplementation(probe)
  }

  private suspend fun invokeImplementation(probe: FileImporter.ImportedFile): MediaItem {
    val localAudio = File(URI(probe.localUri.toString()))
    return finalizeImport(localAudio, probe.durationMs)
  }

  // Make this protected/open so tests can override the factory if needed
  protected open fun createAudioPreviewGenerator(): AudioPreviewGenerator = audioPreviewGeneratorFactory()

  private suspend fun finalizeImport(localAudio: File, durationMs: Long?): MediaItem {
    val projectZip =
        try {
          packager.createProjectZip(audioFile = localAudio, durationMs = durationMs)
        } catch (e: Exception) {
          runCatching { localAudio.delete() }
          throw e
        }
    runCatching { localAudio.delete() }

    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)

    val projectZipPath: String = projectZip.toURI().toString()
    val projectsJsonRepo = ProjectItemsRepositoryLocal(appContext)
    val previewGenerator = createAudioPreviewGenerator()
    previewGenerator.loadProjectData(projectZipPath)
    val tempPreviewUri: Uri? = previewGenerator.audioBuilding()

    // Copy the preview file (if any) from the temporary Uri into a dedicated "previews" folder
    val audioPreviewLocalPath: String = run {
      if (tempPreviewUri == null) return@run ""
      try {
        val previewsDir = File(appContext.filesDir, "previews")
        if (!previewsDir.exists()) previewsDir.mkdirs()

        val contentResolver = appContext.contentResolver
        val mime = contentResolver.getType(tempPreviewUri)
        val extFromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val pathExt = tempPreviewUri.path?.let { File(it).extension }?.takeIf { it.isNotBlank() }
        val ext = extFromMime ?: pathExt ?: "mp3"

        val destFile = File(previewsDir, "${item.id}.$ext")

        // Try to open via content resolver first; fall back to file path if needed
        val inputStream =
            runCatching { contentResolver.openInputStream(tempPreviewUri) }.getOrNull()
        if (inputStream != null) {
          inputStream.use { input ->
            FileOutputStream(destFile).use { out -> input.copyTo(out, 4 * 1024) }
          }
        } else {
          // Try file path fallback
          tempPreviewUri.path?.let { path ->
            File(path).inputStream().use { fis ->
              FileOutputStream(destFile).use { out -> fis.copyTo(out, 4 * 1024) }
            }
          }
        }

        destFile.toURI().toString()
      } catch (e: Exception) {
        Log.w("ImportMediaUseCase", "Failed to copy temp preview to previews folder", e)
        ""
      }
    }

    projectsJsonRepo.addProject(
        ProjectItem(
            uid = projectsJsonRepo.getNewId(),
            name = projectZip.nameWithoutExtension,
            projectFileLocalPath = projectZipPath,
            audioPreviewLocalPath = audioPreviewLocalPath))
    return item
  }
}
