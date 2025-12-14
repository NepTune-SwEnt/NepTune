package com.neptune.neptune.ui.picker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.neptune.neptune.domain.usecase.GetLibraryUseCase
import com.neptune.neptune.domain.usecase.ImportMediaUseCase
import com.neptune.neptune.util.AudioUtils
import com.neptune.neptune.utils.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImportViewModelProcessTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var viewModel: ImportViewModel
  private lateinit var importMediaUseCase: ImportMediaUseCase
  private lateinit var getLibraryUseCase: GetLibraryUseCase
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    // We mock the UseCases to verify calls
    importMediaUseCase = mockk(relaxed = true)
    getLibraryUseCase = mockk(relaxed = true)

    viewModel = ImportViewModel(importMediaUseCase, getLibraryUseCase)

    // Mock the AudioUtils object to control conversion result
    mockkObject(AudioUtils)
  }

  @After
  fun tearDown() {
    unmockkObject(AudioUtils)
  }

  @Test
  fun processAndImportRecordingConvertsToWavImportsNewFileAndDeletesOriginalOnSuccess() = runTest {
    // Given
    val cacheDir = context.cacheDir
    val m4aFile = File(cacheDir, "rec_temp_123.m4a").apply { createNewFile() }
    val rawProjectName = "My Cool Project!" // Contains spaces and special char

    // Expected sanitized name: "My_Cool_Project.wav"
    // Logic: Replace non-alphanumeric with '_', then trim.

    // Simulate successful conversion
    every { AudioUtils.convertToWav(any(), any()) } returns true

    // When
    viewModel.processAndImportRecording(m4aFile, rawProjectName)

    // Then
    // 1. Verify importMedia was called with the WAV file
    coVerify(timeout = 1000) {
      importMediaUseCase.invoke(
          match<File> { file -> file.name == "My_Cool_Project.wav" && file.extension == "wav" })
    }

    // 2. Verify original M4A file is deleted
    assertThat(m4aFile.exists()).isFalse()
  }

  @Test
  fun processAndImportRecordingImportOriginalFileOnConversionFailure() = runTest {
    // Given
    val cacheDir = context.cacheDir
    val m4aFile = File(cacheDir, "rec_temp_fail.m4a").apply { createNewFile() }
    val rawProjectName = "FailedProject"

    // Simulate failed conversion
    every { AudioUtils.convertToWav(any(), any()) } returns false

    // When
    viewModel.processAndImportRecording(m4aFile, rawProjectName)

    // Then
    // 1. Verify importMedia was called with the ORIGINAL M4A file
    coVerify(timeout = 1000) { importMediaUseCase.invoke(m4aFile) }

    // 2. Verify original file still exists
    assertThat(m4aFile.exists()).isTrue()
  }
}
