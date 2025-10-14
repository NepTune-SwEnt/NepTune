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

    // 2) Create .neptune (zip) with config.json + audio
    val projectZip =
        packager.createProjectZip(audioFile = localAudio, durationMs = probe.durationMs)

    // 3) (Optional) remove the copied audio; project now owns the bytes
    runCatching { localAudio.delete() }

    // 4) Persist only the project file path
    val item =
        MediaItem(id = UUID.randomUUID().toString(), projectUri = projectZip.toURI().toString())
    repo.upsert(item)
    return item
  }
}
