package com.neptune.neptune.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.neptune.neptune.domain.port.FileImporter
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/*
 * Implementation of FileImporter interface that imports audio files (MP3/WAV) from SAF URIs
 */
class UnsupportedAudioFormat(msg: String) : IllegalArgumentException(msg)

class FileImporterImpl(
    private val context: Context,
    private val cr: ContentResolver,
    private val paths: StoragePaths
) : FileImporter {

  private val allowedMimes = setOf("audio/mpeg", "audio/wav")
  private val allowedExts = setOf("mp3", "wav")

  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    val safUri = android.net.Uri.parse(sourceUri.toString())
    val (mime, base, ext) = resolveAndValidateAudio(safUri)

    // Single audio workspace
    val dir = paths.audioWorkspace()
    // Create unique file in workspace
    val target = uniqueFile(dir, "$base.$ext")

    cr.openInputStream(safUri)!!.use { input ->
      FileOutputStream(target).use { output -> input.copyTo(output) }
    }
    // Try to get duration (may fail for some formats/encodings)
    val duration =
        runCatching {
              MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(context, safUri)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
              }
            }
            .getOrNull()
      Log.v("FileImporter", "imported ${target.name} (${target.length()} bytes, $duration ms)")
    return FileImporter.ImportedFile(
        displayName = target.name,
        mimeType = mime,
        sourceUri = sourceUri,
        localUri = target.toURI(), // file://... in audio workspace
        sizeBytes = target.length(),
        durationMs = duration)
  }

  // Ensures the file is MP3 or WAV by MIME and/or extension; derives a sane name.
  private fun resolveAndValidateAudio(uri: android.net.Uri): Triple<String?, String, String> {
    val display =
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
          if (it.moveToFirst()) it.getString(0) else null
        }

    val extFromName = display?.substringAfterLast('.', "")?.lowercase().orEmpty()
    val mimeFromCR = cr.getType(uri)
    val extFromMime =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeFromCR ?: "")?.lowercase().orEmpty()

    // Decide extension preference: filename first, then MIME hint
    val ext =
        when {
          extFromName in allowedExts -> extFromName
          extFromMime in allowedExts -> extFromMime
          else -> ""
        }
    val base = (display ?: "audio").removeSuffix(if (ext.isNotEmpty()) ".$ext" else "")

    // Validate
    val normalizedMime =
        when {
          mimeFromCR in allowedMimes -> mimeFromCR
          ext == "mp3" -> "audio/mpeg"
          ext == "wav" -> "audio/wav"
          else -> null
        }
    if (normalizedMime !in allowedMimes) {
      throw UnsupportedAudioFormat("Only MP3/WAV are supported. Got mime=$mimeFromCR name=$display")
    }
    val finalExt =
        when (normalizedMime) {
          "audio/mpeg" -> "mp3"
          "audio/wav" -> "wav"
          else -> ext.ifEmpty { "mp3" } // safe default (shouldn’t happen after validation)
        }
    return Triple(normalizedMime, base, finalExt)
  }

  fun getAudioDurationMsCompat(context: Context, uri: Uri): Long? {
    return runCatching {
          MediaMetadataRetriever().use { mmr ->
            // Prefer FD path (works broadly on old APIs)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
              mmr.setDataSource(afd.fileDescriptor)
            }
                ?: run {
                  // Fallback: context+uri
                  mmr.setDataSource(context, uri)
                }

            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
          }
        }
        .getOrNull()
  }

  // If file exists, appends (2), (3) etc. to base name to make it unique
  private fun uniqueFile(dir: File, candidate: String): File {
    var f = File(dir, candidate)
    if (!f.exists()) return f
    val base = candidate.substringBeforeLast('.')
    val ext = candidate.substringAfterLast('.', "")
    var i = 2
    do {
      f = File(dir, "$base ($i).$ext")
      i++
    } while (f.exists())
    return f
  }
}
