package com.neptune.neptune.domain.usecase

import android.util.Log
import com.neptune.neptune.NepTuneApplication.Companion.appContext
import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import java.io.File
import java.net.URI
import java.util.UUID

open class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager,
    private val audioPreviewGenerator: SamplerProvider? = null,
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

  // Make this protected/open so tests can override and inject a fake SamplerProvider
  protected open fun createSamplerProvider(): SamplerProvider =
      audioPreviewGenerator ?: ViewModelAudioPreviewGenerator()

  private suspend fun finalizeImport(localAudio: File, durationMs: Long?): MediaItem {
    // Run packager on IO dispatcher to avoid blocking callers
    val projectZip =
        try {
          packager.createProjectZip(audioFile = localAudio, durationMs = durationMs)
        } catch (e: Exception) {
          // Ensure we attempt to delete the temporary local file on IO dispatcher
          val isLocalAudioDeleted = localAudio.delete()
          Log.d("ImportMediaUseCase", "finalizeImport: $isLocalAudioDeleted")
          throw e
        }

    // Best-effort delete of the original file on IO dispatcher
    val isLocalAudioDeleted = localAudio.delete()
    Log.d("ImportMediaUseCase", "finalizeImport: $isLocalAudioDeleted")

    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)

    val projectZipPath: String = projectZip.toURI().toString()
    val projectsJsonRepo = ProjectItemsRepositoryLocal(appContext)

    // Use injected audio preview generator (no ViewModel creation in domain code)
    // The generator's loadProjectData is suspend and returns a Uri pointing to extracted audio
    val sampler = createSamplerProvider()

    // Generate preview using the sampler provider off the IO dispatcher
    val tempPreviewUri = sampler.loadProjectData(projectZipPath)

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
