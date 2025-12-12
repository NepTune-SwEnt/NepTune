package com.neptune.neptune.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.neptune.neptune.NepTuneApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Helper to copy a temporary preview Uri into the app's previews folder and return the saved URI
 * string. All file IO is performed on the IO dispatcher.
 */
class PreviewStoreHelper(private val context: Context = NepTuneApplication.appContext) {
  suspend fun saveTempPreviewToPreviewsDir(itemId: String, tempPreviewUri: Uri?): String =
      withContext(Dispatchers.IO) {
        if (tempPreviewUri == null) return@withContext ""

        try {
          val previewsDir = File(context.filesDir, "previews")
          if (!previewsDir.exists()) previewsDir.mkdirs()

          val contentResolver = context.contentResolver
          val mime = contentResolver.getType(tempPreviewUri)
          val extFromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
          val pathExt = tempPreviewUri.path?.let { File(it).extension }?.takeIf { it.isNotBlank() }
          val ext = extFromMime ?: pathExt ?: "mp3"

          val destFile = File(previewsDir, "${itemId}.$ext")

          // Try to open via content resolver first; fall back to file path if needed
          val inputStream = runCatching { contentResolver.openInputStream(tempPreviewUri) }.getOrNull()
          if (inputStream != null) {
            inputStream.use { input ->
              FileOutputStream(destFile).use { out -> input.copyTo(out, 4 * 1024) }
            }
          } else {
            // Try file path fallback
            tempPreviewUri.path?.let { path ->
              File(path).inputStream().use { fis ->
                FileOutputStream(destFile).use { out -> fis.copyTo(out, 4 * 1024) }
              }
            }
          }

          destFile.toURI().toString()
        } catch (e: Exception) {
          Log.w("PreviewStoreHelper", "Failed to copy temp preview to previews folder", e)
          ""
        }
      }
}

