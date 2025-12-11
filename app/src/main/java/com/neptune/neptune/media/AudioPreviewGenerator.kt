package com.neptune.neptune.media

import android.net.Uri

/**
 * Abstraction for generating an audio preview from a project ZIP. Implementations may run a
 * DSP pipeline and return a temporary Uri pointing to the processed preview audio file.
 */
interface AudioPreviewGenerator {
  fun loadProjectData(zipFilePath: String)

  suspend fun audioBuilding(): Uri?
}

