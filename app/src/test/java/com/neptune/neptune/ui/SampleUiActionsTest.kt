package com.neptune.neptune.ui

import android.content.Context
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.main.SampleUiActions
import java.io.File
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SampleUiActionsTest {

  private val repo: SampleRepository = mock()
  private val storageService: StorageService = mock()
  private val downloadsFolder: File = mock()
  private val context: Context = mock()

  private val sample =
      Sample(
          id = 1,
          name = "Test Sample",
          description = "desc",
          durationSeconds = 10,
          tags = emptyList(),
          likes = 0,
          comments = 0,
          downloads = 0)

  @Test
  fun onDownloadClickSuccessUpdatesDownloadCountAndUnzips() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val zipFile: File = mock()
    whenever(storageService.downloadZippedSample(sample, context)).thenReturn(zipFile)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            ioDispatcher = dispatcher)

    assertFalse(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)

    actions.onDownloadClicked(sample)

    verify(repo).increaseDownloadCount(sample.id)
    verify(storageService).downloadZippedSample(sample, context)
    verify(storageService).persistZipToDownloads(zipFile, downloadsFolder)

    assertFalse(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)
  }

  @Test
  fun onDownloadClickWhenBusyDoesNothing() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            ioDispatcher = dispatcher)

    actions.downloadBusy.value = true

    actions.onDownloadClicked(sample)

    verify(repo, never()).increaseDownloadCount(any())
    verify(storageService, never()).downloadZippedSample(any(), any())
    verify(storageService, never()).persistZipToDownloads(any(), any())

    assertTrue(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)
  }

  @Test
  fun onDownloadClickIOExceptionSetsErrorMessage() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)

    whenever(storageService.downloadZippedSample(sample, context))
        .thenThrow(IOException("disk full"))

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            ioDispatcher = dispatcher)

    actions.onDownloadClicked(sample)

    verify(repo).increaseDownloadCount(sample.id)
    verify(storageService).downloadZippedSample(sample, context)
    verify(storageService, never()).persistZipToDownloads(any(), any())

    assertFalse(actions.downloadBusy.value)

    val error = actions.downloadError.value
    assertNotNull(error)
    assertTrue(error!!.startsWith("File error:"))
  }
}
