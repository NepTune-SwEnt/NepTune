package com.neptune.neptune.ui

import android.content.Context
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.profile.ProfileRepository
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

/** Tests for SampleUiActions. This has been written with the help of LLMs. */
@OptIn(ExperimentalCoroutinesApi::class)
class SampleUiActionsTest {

  private val repo: SampleRepository = mock()
  private val storageService: StorageService = mock()
  private val downloadsFolder: File = mock()
  private val context: Context = mock()
  private val profileRepo: ProfileRepository = mock()
  private val sample =
      Sample(
          id = 1.toString(),
          name = "Test Sample",
          description = "desc",
          durationSeconds = 10,
          tags = emptyList(),
          likes = 0,
          comments = 0,
          usersLike = emptyList(),
          downloads = 0)
    private val taggedSample = sample.copy(
        id = "tagged_sample",
        tags = listOf("miku", "lofi")
    )

  @Test
  fun onDownloadClickSuccessUpdatesDownloadCountAndUnzips() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val zipFile: File = mock()

    whenever(storageService.downloadZippedSample(eq(sample), eq(context), any()))
        .thenReturn(zipFile)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            ioDispatcher = dispatcher,
            profileRepo = profileRepo)

    assertFalse(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)

    actions.onDownloadClicked(sample)

    // Make sure all coroutines scheduled on dispatcher are executed
    testScheduler.advanceUntilIdle()

    verify(storageService).downloadZippedSample(eq(sample), eq(context), any())
    verify(storageService).persistZipToDownloads(zipFile, downloadsFolder)
    verify(repo).increaseDownloadCount(sample.id)

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
            ioDispatcher = dispatcher,
            profileRepo = profileRepo)

    actions.downloadBusy.value = true

    actions.onDownloadClicked(sample)

    // No work should have been scheduled, but advance just in case
    testScheduler.advanceUntilIdle()

    verify(repo, never()).increaseDownloadCount(sample.id)
    verify(storageService, never()).downloadZippedSample(sample, context)
    verify(storageService, never()).persistZipToDownloads(any(), any())

    assertTrue(actions.downloadBusy.value)
    assertNull(actions.downloadError.value)
  }

  @Test
  fun onDownloadClickIOExceptionSetsErrorMessage() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)

    whenever(storageService.downloadZippedSample(eq(sample), eq(context), any()))
        .thenThrow(IOException("disk full"))

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            ioDispatcher = dispatcher,
            profileRepo = profileRepo)

    actions.onDownloadClicked(sample)

    testScheduler.advanceUntilIdle()

    // Download should be attempted
    verify(storageService).downloadZippedSample(eq(sample), eq(context), any())

    // But on IOException, we should NOT unzip or increment count
    verify(storageService, never()).persistZipToDownloads(any(), any())
    verify(repo, never()).increaseDownloadCount(any<String>())

    assertFalse(actions.downloadBusy.value)

    val error = actions.downloadError.value
    assertNotNull(error)
    assertTrue(error!!.startsWith("File error:"))
  }

  @Test
  fun onLikeClickedLikesUnlikedSample() = runTest {
    whenever(repo.hasUserLiked(sample.id)).thenReturn(false)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            profileRepo = profileRepo)

    val result = actions.onLikeClicked(sample, isLiked = true)

    assertTrue(result)
    verify(repo).toggleLike(sample.id, true)
  }

  @Test
  fun onLikeClickedUnlikesLikedSample() = runTest {
    whenever(repo.hasUserLiked(sample.id)).thenReturn(true)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            profileRepo = profileRepo)

    val result = actions.onLikeClicked(sample, isLiked = false)

    assertFalse(result)
    verify(repo).toggleLike(sample.id, false)
  }

  @Test
  fun onLikeClickedNoChanges() = runTest {
    whenever(repo.hasUserLiked(sample.id)).thenReturn(true)

    val actions =
        SampleUiActions(
            repo = repo,
            storageService = storageService,
            downloadsFolder = downloadsFolder,
            context = context,
            profileRepo = profileRepo)

    val result = actions.onLikeClicked(sample, isLiked = true)

    assertTrue(result)
    verify(repo, never()).toggleLike(any(), any())
  }
    @Test
    fun onLikeClickedLikesUnlikedSampleAndRecordsPositiveTagInteraction() = runTest {
        whenever(repo.hasUserLiked(taggedSample.id)).thenReturn(false)

        val actions =
            SampleUiActions(
                repo = repo,
                storageService = storageService,
                downloadsFolder = downloadsFolder,
                context = context,
                profileRepo = profileRepo,
            )

        val result = actions.onLikeClicked(taggedSample, isLiked = true)

        assertTrue(result)
        verify(repo).toggleLike(taggedSample.id, true)

        // +1 like, 0 downloads
        verify(profileRepo).recordTagInteraction(
            eq(taggedSample.tags),
            eq(1),  // likeDelta
            eq(0),  // downloadDelta
        )
    }


    @Test
    fun onLikeClickedNoChangesDoesNotTouchRepoOrTagProfile() = runTest {
        whenever(repo.hasUserLiked(taggedSample.id)).thenReturn(true)

        val actions =
            SampleUiActions(
                repo = repo,
                storageService = storageService,
                downloadsFolder = downloadsFolder,
                context = context,
                profileRepo = profileRepo,
            )

        val result = actions.onLikeClicked(taggedSample, isLiked = true)

        assertTrue(result)
        verify(repo, never()).toggleLike(any(), any())

        // And no tag interaction should be recorded
        verify(profileRepo, never()).recordTagInteraction(any(), any(), any())
    }
}
