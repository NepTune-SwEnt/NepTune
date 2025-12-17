package com.neptune.neptune.ui

import android.content.Context
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.main.SampleUiActions
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SampleUiActionsTest {

  private val sampleRepo: SampleRepository = mock()
  private val storageService: StorageService = mock()
  private val context: Context = mock()
  private val profileRepo: ProfileRepository = mock()

  // IMPORTANT: real folder (not a mock) or File(downloadsFolder, "...") may break
  private lateinit var downloadsFolder: File

  private val sample =
      Sample(
          id = "sampleId",
          name = "Test Sample",
          description = "desc",
          durationMilliSecond = 10,
          tags = listOf("lofi"),
          likes = 0,
          comments = 0,
          usersLike = emptyList(),
          downloads = 0,
          storageProcessedSamplePath = "processed_audios/path.wav",
      )

  @Before
  fun setUp() {
    downloadsFolder = createTempDir(prefix = "neptuneDownloads_")
  }

  @After
  fun tearDown() {
    downloadsFolder.deleteRecursively()
  }

  private fun createActions(
      dispatcher: TestDispatcher,
      downloadProgress: MutableStateFlow<Int?> = MutableStateFlow(null),
  ): SampleUiActions {
    return SampleUiActions(
        repo = sampleRepo,
        storageService = storageService,
        downloadsFolder = downloadsFolder,
        context = context,
        ioDispatcher = dispatcher,
        profileRepo = profileRepo,
        downloadProgress = downloadProgress,
    )
  }

  @Test
  fun onDownloadProcessedClickedSuccessDownloadsFileUpdatesProgressAndRepo() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val progressFlow = MutableStateFlow<Int?>(null)
    val emissions = mutableListOf<Int?>()

    val job = launch { progressFlow.collect { emissions.add(it) } }

    val actions = createActions(dispatcher, progressFlow)

    doAnswer { invocation ->
          val onProgress = invocation.getArgument<(Int) -> Unit>(2)
          onProgress(100)
          Unit
        }
        .whenever(storageService)
        .downloadFileByPath(eq(sample.storageProcessedSamplePath), any(), any())

    actions.onDownloadProcessedClicked(sample)
    testScheduler.advanceUntilIdle()
    job.cancel()

    // ✔ verify progress happened
    assertTrue(emissions.contains(100))

    verify(storageService).downloadFileByPath(eq(sample.storageProcessedSamplePath), any(), any())
    verify(sampleRepo).increaseDownloadCount(sample.id)
    verify(profileRepo).recordTagInteraction(eq(sample.tags), eq(0), eq(1))

    assertFalse(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)
  }

  @Test
  fun onDownloadProcessedClickedWhenBusyDoesNothing() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val actions = createActions(dispatcher)

    actions.downloadBusy.value = true

    actions.onDownloadProcessedClicked(sample)
    testScheduler.advanceUntilIdle()

    verify(storageService, never()).downloadFileByPath(any(), any(), any())
    verify(sampleRepo, never()).increaseDownloadCount(any())
    verify(profileRepo, never()).recordTagInteraction(any(), any(), any())
  }

  @Test
  fun onDownloadProcessedClickedBlankPathSetsErrorAndDoesNotCallStorage() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val actions = createActions(dispatcher)

    val noProcessed = sample.copy(storageProcessedSamplePath = "")

    actions.onDownloadProcessedClicked(noProcessed)
    testScheduler.advanceUntilIdle()

    verify(storageService, never()).downloadFileByPath(any(), any(), any())
    verify(sampleRepo, never()).increaseDownloadCount(any())
    verify(profileRepo, never()).recordTagInteraction(any(), any(), any())

    assertFalse(actions.downloadBusy.value)
    assertNotNull(actions.downloadError.value)
    assertTrue(actions.downloadError.value!!.contains("No processed audio available"))
  }

  @Test
  fun onDownloadProcessedClickedRuntimeFailureSetsDownloadFailedAndStops() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val actions = createActions(dispatcher)

    whenever(
            storageService.downloadFileByPath(
                eq(sample.storageProcessedSamplePath),
                any(),
                any(),
            ))
        .thenThrow(RuntimeException("network failure"))

    actions.onDownloadProcessedClicked(sample)
    testScheduler.advanceUntilIdle()

    verify(storageService).downloadFileByPath(eq(sample.storageProcessedSamplePath), any(), any())
    verify(sampleRepo, never()).increaseDownloadCount(any())
    verify(profileRepo, never()).recordTagInteraction(any(), any(), any())

    assertFalse(actions.downloadBusy.value)
    assertNotNull(actions.downloadError.value)
    assertTrue(actions.downloadError.value!!.contains("Download failed"))
  }

  @Test
  fun onDownloadProcessedClickedCreatesSanitizedFileNameAndWavExtension() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val actions = createActions(dispatcher)

    // name contains forbidden characters + weird whitespace
    val sample =
        sample.copy(
            name = "  my:bad/\\name*?\"<>|   ",
            storageProcessedSamplePath = "processed_audios/something.wav")

    val outFileCaptor = argumentCaptor<File>()

    doAnswer { invocation ->
          val onProgress = invocation.getArgument<(Int) -> Unit>(2)
          onProgress(100)
          Unit
        }
        .whenever(storageService)
        .downloadFileByPath(eq(sample.storageProcessedSamplePath), outFileCaptor.capture(), any())

    actions.onDownloadProcessedClicked(sample)
    testScheduler.advanceUntilIdle()

    val outFile = outFileCaptor.firstValue
    assertTrue(outFile.name.endsWith(".wav"))

    // forbidden chars replaced with "_" and multi spaces collapsed
    assertFalse(outFile.name.contains(":"))
    assertFalse(outFile.name.contains("/"))
    assertFalse(outFile.name.contains("\\"))
    assertFalse(outFile.name.contains("*"))
    assertFalse(outFile.name.contains("?"))
    assertFalse(outFile.name.contains("\""))
    assertFalse(outFile.name.contains("<"))
    assertFalse(outFile.name.contains(">"))
    assertFalse(outFile.name.contains("|"))

    verify(sampleRepo).increaseDownloadCount(sample.id)
    verify(profileRepo).recordTagInteraction(eq(sample.tags), eq(0), eq(1))
    assertNull(actions.downloadError.value)
    assertFalse(actions.downloadBusy.value)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun onDownloadProcessedClickedUpdatesProgressIncrementally() = runTest {
    val ioDispatcher = StandardTestDispatcher(testScheduler)
    val collectorDispatcher = UnconfinedTestDispatcher(testScheduler)

    val progressFlow = MutableStateFlow<Int?>(null)

    val emissions = mutableListOf<Int?>()
    val collectJob = launch(collectorDispatcher) { progressFlow.collect { emissions.add(it) } }

    val actions = createActions(ioDispatcher, progressFlow)

    doAnswer { invocation ->
          val onProgress = invocation.getArgument<(Int) -> Unit>(2)
          onProgress(10)
          onProgress(55)
          onProgress(100)
          Unit
        }
        .whenever(storageService)
        // don't over-constrain here; mismatched path = zero calls
        .downloadFileByPath(any(), any(), any())

    actions.onDownloadProcessedClicked(sample)

    testScheduler.advanceUntilIdle()
    collectJob.cancel()

    // If this fails, your stub didn't match OR the method didn't run
    verify(storageService).downloadFileByPath(any(), any(), any())

    // SampleUiActions sets 0 at start and null at end (finally)
    assertTrue(emissions.contains(0))
    assertTrue(emissions.contains(10))
    assertTrue(emissions.contains(55))
    assertTrue(emissions.contains(100))
    assertEquals(null, emissions.last())
  }
}
