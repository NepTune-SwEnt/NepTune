package com.neptune.neptune.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.theme.NepTuneTheme
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * SampleActions Handles user actions related to samples such as liking and downloading. Manages
 * download state and error reporting. Uses coroutines for asynchronous operations.
 *
 * @property repo SampleRepository for accessing and manipulating samples.
 * @property storageService StorageService for handling file downloads and unzipping.
 * @property downloadsFolder File representing the directory where downloads are saved.
 * @property context Context for accessing application resources.
 * @property ioDispatcher CoroutineDispatcher for IO-bound operations, defaults to Dispatchers.IO.
 *   written with assistance from ChatGPT
 */
class SampleUiActions(
    private val repo: SampleRepository,
    private val storageService: StorageService,
    private val downloadsFolder: File,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val downloadProgress: MutableStateFlow<Int?> = MutableStateFlow(0)
) {
  val downloadBusy = MutableStateFlow(false)
  val downloadError = MutableStateFlow<String?>(null)

  @Throws(IOException::class)
  suspend fun onDownloadClicked(sample: Sample) {
    if (downloadBusy.value) return
    downloadBusy.value = true
    downloadError.value = null
    downloadProgress.value = 0
    try {
      val zip =
          withContext(ioDispatcher) {
            storageService.downloadZippedSample(sample, context) { percent ->
              downloadProgress.value = percent
            }
          }
      withContext(ioDispatcher) { storageService.persistZipToDownloads(zip, downloadsFolder) }
      repo.increaseDownloadCount(sample.id)
    } catch (e: SecurityException) {
      downloadError.value = "Storage permission required: ${e.message}"
    } catch (e: IOException) {
      downloadError.value = "File error: ${e.message}"
    } catch (e: Exception) {
      downloadError.value = "Download failed: ${e.message}"
      Log.e("SampleActions", "Download failed", e)
    } finally {
      downloadBusy.value = false
      downloadProgress.value = null
    }
  }

  suspend fun onLikeClicked(sampleId: String, isLiked: Boolean): Boolean {
    val alreadyLiked = repo.hasUserLiked(sampleId)

    return if (!alreadyLiked && isLiked) {
      repo.toggleLike(sampleId, true)
      true
    } else if (alreadyLiked && !isLiked) {
      repo.toggleLike(sampleId, false)
      false
    } else {
      // Doesn't change
      alreadyLiked
    }
  }
}

@Composable
fun DownloadProgressBar(downloadProgress: Int, testTag: String) {
  Box(
      modifier =
          Modifier.fillMaxSize().background(NepTuneTheme.colors.background.copy(alpha = 0.6f)),
      contentAlignment = Alignment.Center) {
        LinearProgressIndicator(
            progress = { downloadProgress / 100f },
            modifier = Modifier.padding(16.dp).fillMaxWidth(0.5f).testTag(testTag),
            color = NepTuneTheme.colors.onBackground,
            trackColor = NepTuneTheme.colors.background,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
      }
}
