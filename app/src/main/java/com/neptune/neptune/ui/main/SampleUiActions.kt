package com.neptune.neptune.ui.main

import android.content.Context
import android.util.Log
import com.neptune.neptune.data.storage.StorageService
import com.neptune.neptune.model.sample.Sample
import com.neptune.neptune.model.sample.SampleRepository
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  val downloadBusy = MutableStateFlow(false)
  val downloadError = MutableStateFlow<String?>(null)

  @Throws(IOException::class)
  suspend fun onDownloadClicked(sample: Sample) {
    if (downloadBusy.value) return
    repo.increaseDownloadCount(sample.id)
    downloadBusy.value = true
    downloadError.value = null
    try {
      val zip = withContext(ioDispatcher) { storageService.downloadZippedSample(sample, context) }
      withContext(ioDispatcher) { storageService.unzipSample(zip, downloadsFolder) }
    } catch (e: SecurityException) {
      downloadError.value = "Storage permission required: ${e.message}"
    } catch (e: IOException) {
      downloadError.value = "File error: ${e.message}"
    } catch (e: Exception) {
      downloadError.value = "Download failed: ${e.message}"
      downloadError.value = "Download failed: ${e.message}"
      Log.e("SampleActions", "Download failed", e)
    } finally {
      downloadBusy.value = false
    }
  }
}
