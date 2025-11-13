package com.neptune.neptune.ui.navigation

import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.model.project.ProjectExtractor
import com.neptune.neptune.ui.sampler.SamplerViewModel
import java.io.File
import java.io.FileOutputStream
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProjectLoacalSavingTest {

  @Test
  fun testSaveProjectData() = runBlocking {
    val context = NepTuneApplication.appContext
    val tempZip = File(context.cacheDir, "testProject.zip")

    context.assets.open("fakeProject.zip").use { input ->
      FileOutputStream(tempZip).use { output -> input.copyTo(output) }
    }

    val viewModel = SamplerViewModel()

    viewModel.loadProjectData(tempZip.absolutePath)

    viewModel.updateSustain(0.8f)
    viewModel.updateAttack(0.35f)
    viewModel.updateTempo(117)

    viewModel.saveProjectDataSync(tempZip.absolutePath)

    val metadata = ProjectExtractor().extractMetadata(tempZip)
    val sustainValue = metadata.parameters.find { it.type == "sustain" }?.value
    val attackValue = metadata.parameters.find { it.type == "attack" }?.value

    assertEquals(0.8f, sustainValue)
    assertEquals(0.35f, attackValue)
  }
}
