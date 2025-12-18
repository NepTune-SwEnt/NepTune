package com.neptune.neptune.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.domain.usecase.ViewModelAudioPreviewGenerator
import com.neptune.neptune.model.profile.ProfileRepository
import com.neptune.neptune.model.profile.ProfileRepositoryProvider
import com.neptune.neptune.model.project.ProjectItem
import com.neptune.neptune.model.project.ProjectItemsRepositoryLocal
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
import com.neptune.neptune.ui.sampler.SamplerViewModel
import com.neptune.neptune.ui.theme.NepTuneTheme
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

object SampleUiActionsTestTags {
  val DOWNLOAD_ZIP_BTN = "download_zip_btn"
  val DOWNLOAD_PROCESSED_BTN = "download_processed_btn"
}
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
open class SampleUiActions(
    private val repo: SampleRepository,
    private val storageService: StorageService,
    private val downloadsFolder: File,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val downloadProgress: MutableStateFlow<Int?> = MutableStateFlow(0),
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository
) {
  val downloadBusy = MutableStateFlow(false)
  val downloadError = MutableStateFlow<String?>(null)

  @Throws(IOException::class)
  open suspend fun onDownloadZippedClicked(sample: Sample) {
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

      val repoJSON = ProjectItemsRepositoryLocal(context)
      val newUid = repoJSON.getNewId()
      val processedAudioFile = File(File(context.filesDir, "previews"), "$newUid.wav")
      withContext(ioDispatcher) {
        storageService.downloadFileByPath(sample.storageProcessedSamplePath, processedAudioFile) {}
        val newFile = storageService.persistZipToDownloads(zip, File(context.filesDir, "projects"))
        repoJSON.addProject(ProjectItem(
          uid = newUid,
          name = sample.name,
          description = sample.description,
          audioPreviewLocalPath = processedAudioFile.toString(),
          projectFileLocalPath = newFile.toURI().toString(),
          ownerId = null,
          collaborators = listOf()
        ))
      }
      repo.increaseDownloadCount(sample.id)

      // record download interaction
      profileRepo.recordTagInteraction(tags = sample.tags, likeDelta = 0, downloadDelta = 1)
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

  suspend fun onLikeClicked(sample: Sample, isLiked: Boolean): Boolean {
    val sampleId = sample.id
    val alreadyLiked = repo.hasUserLiked(sampleId)

    val result =
        if (!alreadyLiked && isLiked) {
          repo.toggleLike(sampleId, true)
          true
        } else if (alreadyLiked && !isLiked) {
          repo.toggleLike(sampleId, false)
          false
        } else {
          // Doesn't change
          alreadyLiked
        }
    if (!alreadyLiked && isLiked) {
      profileRepo.recordTagInteraction(tags = sample.tags, likeDelta = 1, downloadDelta = 0)
    }
    return result
  }

  /** Delegate function to get download URL from StorageService */
  suspend fun getDownloadUrl(storagePath: String): String? {
    return storageService.getDownloadUrl(storagePath)
  }

  /** replaces / \ : * ? " < > | AND whitespaces with _ */
  private fun safeFileName(raw: String): String {
    // remove characters that are problematic on filesystems
    return raw.trim()
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .ifBlank { "audio" }
  }

  @Throws(IOException::class)
  open suspend fun onDownloadProcessedClicked(sample: Sample) {
    if (downloadBusy.value) return
    downloadBusy.value = true
    downloadError.value = null
    downloadProgress.value = 0

    try {
      val processedPath = sample.storageProcessedSamplePath
      if (processedPath.isBlank()) {
        downloadError.value = "No processed audio available for this sample."
        return
      }
      val baseName = safeFileName(sample.name.ifBlank { "audio" })
      // put it next to downloads (or inside a subfolder)
      val outFile = File(downloadsFolder, "$baseName.wav")

      withContext(ioDispatcher) {
        storageService.downloadFileByPath(processedPath, outFile) { percent ->
          downloadProgress.value = percent
        }
      }

      // optional: count as a download interaction too (your choice)
      repo.increaseDownloadCount(sample.id)
      profileRepo.recordTagInteraction(tags = sample.tags, likeDelta = 0, downloadDelta = 1)
    } catch (e: SecurityException) {
      downloadError.value = "Storage permission required: ${e.message}"
    } catch (e: IOException) {
      downloadError.value = "File error: ${e.message}"
    } catch (e: Exception) {
      downloadError.value = "Download failed: ${e.message}"
      Log.e("SampleActions", "Processed download failed", e)
    } finally {
      downloadBusy.value = false
      downloadProgress.value = null
    }
  }
}

/**
 * the window that pops up when you click on the download button.
 *
 * @param sampleName the name of the sample
 * @param processedAvailable whether the processed audio is available
 * @param onDismiss the callback to be invoked when the dialog is dismissed
 * @param onDownloadZip the callback to be invoked when the download zip button is clicked
 * @param onDownloadProcessed the callback to be invoked when the download processed button is
 *   clicked
 */
@Composable
fun DownloadChoiceDialog(
    sampleName: String,
    processedAvailable: Boolean,
    onDismiss: () -> Unit,
    onDownloadZip: () -> Unit,
    onDownloadProcessed: () -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Download") },
      text = { Text("Choose what to download for \"$sampleName\"") },
      confirmButton = {
        Button(
            onClick = onDownloadZip,
            modifier = Modifier.testTag(SampleUiActionsTestTags.DOWNLOAD_ZIP_BTN),
        ) {
          Text("Download ZIP")
        }
      },
      dismissButton = {
        // second action button (kept simple)
        Button(
            onClick = onDownloadProcessed,
            enabled = processedAvailable,
            modifier = Modifier.testTag(SampleUiActionsTestTags.DOWNLOAD_PROCESSED_BTN),
        ) {
          Text("Processed Audio")
        }
      })
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
