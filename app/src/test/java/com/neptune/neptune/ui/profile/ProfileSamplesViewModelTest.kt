package com.neptune.neptune.ui.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.fakes.FakeProfileRepository
import com.neptune.neptune.model.fakes.FakeSampleRepository
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.utils.MainDispatcherRule
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileSamplesViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @get:Rule val mainDispatcherRule = MainDispatcherRule(dispatcher)

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun onLikeClickUpdatesLikesAndCache() =
      runTest(dispatcher) {
        val sample = testSample(likes = 0)
        val sampleRepo = FakeSampleRepository(listOf(sample))
        val profileRepo = FakeProfileRepository()

        val viewModel =
            ProfileSamplesViewModel(
                ownerId = sample.ownerId,
                sampleRepo = sampleRepo,
                profileRepo = profileRepo,
                auth = null,
                enableActions = false)

        advanceUntilIdle()
        assertEquals(1, viewModel.samples.value.size)

        viewModel.onLikeClick(sample, isLiked = true)
        advanceUntilIdle()

        val likedSample = viewModel.samples.value.first()
        assertEquals(sample.likes + 1, likedSample.likes)
        assertTrue(viewModel.likedSamples.value[sample.id] == true)

        viewModel.onLikeClick(sample, isLiked = false)
        advanceUntilIdle()

        val unlikedSample = viewModel.samples.value.first()
        assertEquals(sample.likes, unlikedSample.likes)
        assertFalse(viewModel.likedSamples.value[sample.id] == true)
      }

  @Test
  fun onDownloadSampleDelegatesToActionsAndUpdatesDownloads() =
      runTest(dispatcher) {
        val sample = testSample(downloads = 0)
        val sampleRepo = FakeSampleRepository(listOf(sample))
        val profileRepo = FakeProfileRepository()
        val storageService: StorageService = mock()
        val downloadsDir = Files.createTempDirectory("downloads").toFile()
        val zipFile =
            Files.createTempFile("sample_download", ".zip").toFile().apply { writeText("zip") }

        whenever(storageService.downloadZippedSample(eq(sample), eq(context), any())).thenAnswer {
          val progressCallback = it.getArgument<(Int) -> Unit>(2)
          progressCallback.invoke(100)
          zipFile
        }
        whenever(storageService.persistZipToDownloads(any(), any())).thenAnswer {
          val output = java.io.File(downloadsDir, zipFile.name)
          output.writeText("copied")
          output
        }

        val viewModel =
            ProfileSamplesViewModel(
                ownerId = sample.ownerId,
                sampleRepo = sampleRepo,
                profileRepo = profileRepo,
                explicitStorageService = storageService,
                explicitDownloadsFolder = downloadsDir,
                explicitIoDispatcher = dispatcher,
                auth = null)

        viewModel.onDownloadSample(sample)
        advanceUntilIdle()

        verify(storageService).downloadZippedSample(eq(sample), eq(context), any())
        verify(storageService).persistZipToDownloads(any(), any())

        val updated =
            sampleRepo.observeSamples().first { list ->
              list.any { it.id == sample.id && it.downloads == sample.downloads + 1 }
            }
        assertEquals(sample.downloads + 1, updated.first { it.id == sample.id }.downloads)
      }

  private fun testSample(
      id: String = "sample-1",
      likes: Int = 0,
      downloads: Int = 0,
      ownerId: String = "owner"
  ): Sample {
    return Sample(
        id = id,
        name = "Sample $id",
        description = "desc",
        durationSeconds = 10,
        tags = emptyList(),
        likes = likes,
        usersLike = emptyList(),
        comments = 0,
        downloads = downloads,
        isPublic = true,
        ownerId = ownerId,
        storageZipPath = "zip/$id.zip",
        storageImagePath = "image/$id.jpg",
        storagePreviewSamplePath = "preview/$id.mp3")
  }
}
