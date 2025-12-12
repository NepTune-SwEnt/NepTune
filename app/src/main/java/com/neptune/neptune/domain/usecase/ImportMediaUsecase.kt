package com.neptune.neptune.domain.usecase

import com.neptune.neptune.NepTuneApplication.Companion.appContext
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.UUID

open class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager,
    private val audioPreviewGenerator: AudioPreviewGenerator = ViewModelAudioPreviewGenerator(),
    private val previewStoreHelper: PreviewStoreHelper = PreviewStoreHelper()
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

  // Make this protected/open so tests can override and inject a fake AudioPreviewGenerator
  protected open fun createSamplerProvider(): AudioPreviewGenerator = audioPreviewGenerator

  private suspend fun finalizeImport(localAudio: File, durationMs: Long?): MediaItem {
    // Run packager on IO dispatcher to avoid blocking callers
    val projectZip =
        try {
          withContext(Dispatchers.IO) { packager.createProjectZip(audioFile = localAudio, durationMs = durationMs) }
        } catch (e: Exception) {
          // Ensure we attempt to delete the temporary local file on IO dispatcher
          withContext(Dispatchers.IO) { localAudio.delete() }
          throw e
        }

    // Best-effort delete of the original file on IO dispatcher
    withContext(Dispatchers.IO) { localAudio.delete() }

    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)

    val projectZipPath: String = projectZip.toURI().toString()
    val projectsJsonRepo = ProjectItemsRepositoryLocal(appContext)

    // Use injected audio preview generator (no ViewModel creation in domain code)
    val sampler = createSamplerProvider()
    sampler.loadProjectData(projectZipPath)
    val tempPreviewUri = sampler.audioBuilding()

    // Copy the preview file (if any) from the temporary Uri into a dedicated "previews" folder
    val audioPreviewLocalPath: String =
        previewStoreHelper.saveTempPreviewToPreviewsDir(item.id, tempPreviewUri)

    projectsJsonRepo.addProject(
        ProjectItem(
            uid = projectsJsonRepo.getNewId(),
            name = projectZip.nameWithoutExtension,
            projectFileLocalPath = projectZipPath,
            audioPreviewLocalPath = audioPreviewLocalPath))
    return item
  }
}
