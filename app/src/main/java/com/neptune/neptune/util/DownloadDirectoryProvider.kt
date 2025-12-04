package com.neptune.neptune.util

import android.content.Context
import android.os.Environment
import java.io.File

object DownloadDirectoryProvider {
  /**
   * + * Resolves the app download directory. Uses app-scoped external storage only when it is
   * + * mounted and writable; otherwise falls back to internal storage. +
   */
  fun resolveDownloadsDir(context: Context, explicitDownloadsFolder: File? = null): File {
    explicitDownloadsFolder?.let {
      return it
    }

    val external = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (external != null) {
      val state = Environment.getExternalStorageState(external)
      if (state == Environment.MEDIA_MOUNTED && external.canWrite()) {
        return external
      }
    }

    return context.filesDir
  }
}
