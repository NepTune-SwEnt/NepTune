package com.neptune.neptune.ui.sampler

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.NepTuneApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SamplerViewModelTest {

    private lateinit var viewModel: SamplerViewModel

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        NepTuneApplication.appContext = ApplicationProvider.getApplicationContext()
        viewModel = SamplerViewModel()
    }

    @Test
    fun resetSampleAndSave_resetsStateAndSaves() = runTest {
        val zipPath = "fake/path.zip"
        val viewModel = spyk(SamplerViewModel())

        coEvery { viewModel.stopPreview() } just Runs
        coEvery { viewModel.saveProjectData(zipPath) } returns mockk()

        viewModel.resetSampleAndSave(zipPath)

        val state = viewModel.uiState.value
        assertEquals(false, state.isPlaying)
        assertEquals(false, state.previewPlaying)
        assertEquals(state.inputTempo, state.tempo)
    }
}