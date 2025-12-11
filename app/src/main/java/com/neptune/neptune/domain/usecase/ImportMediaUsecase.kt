package com.neptune.neptune.domain.usecase

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

class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager
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

    val vm = ProjectItemsRepositoryLocal(appContext)
    vm.addProject(
        ProjectItem(
            uid = vm.getNewId(),
            name = projectZip.nameWithoutExtension,
            projectFileLocalPath = projectZip.toURI().toString()))
    return item
  }
}
