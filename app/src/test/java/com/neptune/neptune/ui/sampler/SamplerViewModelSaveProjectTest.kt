package com.neptune.neptune.ui.sampler

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.NepTuneApplication
import com.neptune.neptune.domain.usecase.PreviewStoreHelper
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import com.neptune.neptune.model.project.ProjectWriter
import io.mockk.*
import java.io.File
import junit.framework.Assert.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamplerViewModelSaveProjectTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    try {
      val f = NepTuneApplication::class.java.getDeclaredField("appContext")
      f.isAccessible = true
      f.set(null, context)
    } catch (_: Exception) {}
  }

  @After
  fun tearDown() {
    unmockkAll()
    clearAllMocks()
  }

  @Test
  fun saveProjectData_savesPreviewAndUpdatesProject() = runBlocking {
    // Prepare temp files
    val tmpAudio = File.createTempFile("sample_audio", ".wav")
    tmpAudio.writeBytes(ByteArray(512))

    val tmpZip = File.createTempFile("project", ".zip")

    // Prepare mocks
    val previewStoreHelper = mockk<PreviewStoreHelper>()

    val projectId = "proj-123"
    val project =
        ProjectItem(uid = projectId, name = "MyProject", projectFileLocalPath = tmpZip.absolutePath)

    // Mock ProjectItemsRepositoryLocal constructor and methods
    mockkConstructor(ProjectItemsRepositoryLocal::class)
    coEvery {
      anyConstructed<ProjectItemsRepositoryLocal>().findProjectWithProjectFile(any())
    } returns project
    coEvery { anyConstructed<ProjectItemsRepositoryLocal>().editProject(any(), any()) } just Runs

    // Mock ProjectWriter to avoid actual IO
    mockkConstructor(ProjectWriter::class)
    every { anyConstructed<ProjectWriter>().writeProject(any(), any(), any()) } just Runs

    val savedPreviewPath = "file://previews/$projectId.mp3"
    coEvery { previewStoreHelper.saveTempPreviewToPreviewsDir(projectId, any()) } returns
        savedPreviewPath

    // Create a VM that overrides audioBuilding to return the tmpAudio Uri (avoid heavy processing)
    val vm =
        object : SamplerViewModel(previewStoreHelper) {
          override fun audioBuilding(): Job? {
            // Simulate that processing produced this audio Uri synchronously
            val uri = Uri.fromFile(tmpAudio)
            // Also update state as audioBuilding would normally
            _uiState.update { it.copy(currentAudioUri = uri) }
            // Return null to indicate no background Job was started
            return null
          }
        }

    // Ensure the state contains the currentAudioUri so saveProjectDataSync can find the file
    vm._uiState.update { it.copy(currentAudioUri = Uri.fromFile(tmpAudio)) }

    // Call saveProjectData
    vm.saveProjectData(tmpZip.absolutePath)

    // Wait briefly for the viewModelScope coroutine to execute (matches pattern in other tests)
    Thread.sleep(300)

    // Verify repository was asked for the project by path
    coVerify(atLeast = 1) {
      anyConstructed<ProjectItemsRepositoryLocal>().findProjectWithProjectFile(tmpZip.absolutePath)
    }

    // Verify preview was saved
    coVerify { previewStoreHelper.saveTempPreviewToPreviewsDir(projectId, any()) }

    // Capture the edited project passed to editProject and assert it contains the saved preview
    // path
    val slot = slot<com.neptune.neptune.model.project.ProjectItem>()
    coVerify { anyConstructed<ProjectItemsRepositoryLocal>().editProject(projectId, capture(slot)) }
    val edited = slot.captured
    assertEquals(savedPreviewPath, edited.audioPreviewLocalPath)

    // Also assert VM state currentAudioUri points to our tmp audio
    assertEquals(Uri.fromFile(tmpAudio), vm.uiState.value.currentAudioUri)
  }
}
