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

/*
   Use case to import a media file from an external URI into the app's local audio workspace.
   This involves copying the file locally, packaging it into a .zip with a config.json, and
   persisting a MediaItem that points to the .zip location.
   Partially written with ChatGPT
*/
class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager
) {
  suspend operator fun invoke(sourceUriString: String): MediaItem {
    // Copy picked audio (still needed to read bytes)
    val probe = importer.importFile(URI(sourceUriString))
    val localAudio = File(URI(probe.localUri.toString()))

    // Create .zip with config.json + audio
    val projectZip =
        try {
          packager.createProjectZip(audioFile = localAudio, durationMs = probe.durationMs)
        } catch (e: Exception) {
          // ensure we don't leak the copied audio if packaging fails
          runCatching { localAudio.delete() }
          throw e
        }
    // remove the copied audio; project now owns the bytes
    runCatching { localAudio.delete() }

    // Persist only the project file path
    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)

    // Add new MediaItem to repository in `projects.json`
    val vm = ProjectItemsRepositoryLocal(appContext)
    vm.addProject(
        ProjectItem(
            uid = vm.getNewId(),
            name = projectZip.nameWithoutExtension,
            projectFilePath = projectZip.toURI().toString()))
    return item
  }
}
