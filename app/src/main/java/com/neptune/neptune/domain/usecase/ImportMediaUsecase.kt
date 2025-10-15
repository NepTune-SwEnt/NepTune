package com.neptune.neptune.domain.usecase

import com.neptune.neptune.data.NeptunePackager
import com.neptune.neptune.domain.model.MediaItem
import com.neptune.neptune.domain.port.FileImporter
import com.neptune.neptune.domain.port.MediaRepository
import java.io.File
import java.net.URI
import java.util.UUID

class ImportMediaUseCase(
    private val importer: FileImporter,
    private val repo: MediaRepository,
    private val packager: NeptunePackager
) {
  suspend operator fun invoke(sourceUriString: String): MediaItem {
    // 1) Copy picked audio (still needed to read bytes)
    val probe = importer.importFile(URI(sourceUriString))
    val localAudio = File(URI(probe.localUri.toString()))

    // 2) Create .zip with config.json + audio
    val projectZip = try {
        packager.createProjectZip(audioFile = localAudio, durationMs = probe.durationMs)
    } catch (e: Exception) {
        // ensure we don't leak the copied audio if packaging fails
        runCatching { localAudio.delete() }
        throw e
    }
    // remove the copied audio; project now owns the bytes
    runCatching { localAudio.delete() }

    //Persist only the project file path
    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)
    return item
  }
}
