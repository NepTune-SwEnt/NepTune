package com.neptune.neptune.domain.usecase

import android.net.Uri
import com.neptune.neptune.ui.sampler.SamplerViewModel

/**
 * Interface representing a service that can load a sampler project and generate an audio preview Uri.
 * Implementations can be provided by the UI layer (eg. a ViewModel) or by a test double.
 */
interface AudioPreviewGenerator {
  fun loadProjectData(zipFilePath: String)
  suspend fun audioBuilding(): Uri?
}

/**
 * Default adapter that delegates to a SamplerViewModel instance. This keeps the domain code
 * dependent only on the interface; the implementation here still uses the SamplerViewModel but
 * can be replaced by other implementations in tests or different environments.
 */
class ViewModelAudioPreviewGenerator(
    private val sampler: SamplerViewModel = SamplerViewModel()
) : AudioPreviewGenerator {
  override fun loadProjectData(zipFilePath: String) = sampler.loadProjectData(zipFilePath)

  override suspend fun audioBuilding(): Uri? = sampler.audioBuilding()
}

