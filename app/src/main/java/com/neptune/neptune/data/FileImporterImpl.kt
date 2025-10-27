package com.neptune.neptune.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
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
  private val audioString = "audio"
  private val mp3Ext = "mp3"
  private val wavExt = "wav"
  private val mp3Mime = "audio/mpeg"
  private val wavMime = "audio/wav"
  private val fileImporterTag = "FileImporter"
  private val allowedMimes = setOf(mp3Mime, wavMime)
  private val allowedExts = setOf(mp3Ext, wavExt)

  @RequiresApi(Build.VERSION_CODES.Q)
  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    val safUri = sourceUri.toString().toUri()
    val parsed: ParsedFromUri = resolveAndValidateAudio(safUri)

    // Single audio workspace
    val dir = paths.audioWorkspace()
    // Create unique file in workspace
    val target = uniqueFile(dir, "${parsed.base}.${parsed.ext}")

    val inputStream =
        cr.openInputStream(safUri)
            ?: throw IllegalArgumentException("Cannot open input stream for URI: $safUri")
    inputStream.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } }
    // Try to get duration (may fail for some formats/encodings)
    val duration =
        runCatching {
              MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(context, safUri)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
              }
            }
            .getOrNull()
    Log.v(fileImporterTag, "imported ${target.name} (${target.length()} bytes, $duration ms)")
    return FileImporter.ImportedFile(
        displayName = target.name,
        mimeType = parsed.mime,
        sourceUri = sourceUri,
        localUri = target.toURI(), // file://... in audio workspace
        sizeBytes = target.length(),
        durationMs = duration ?: 0L)
  }

  // Ensures the file is MP3 or WAV by MIME and/or extension; derives a sane name.
  private fun resolveAndValidateAudio(uri: Uri): ParsedFromUri {
    // 1) Pull display + mime, with robust fallbacks for file:// URIs
    val isFile = uri.scheme == ContentResolver.SCHEME_FILE
    val crMime = if (!isFile) cr.getType(uri) else null

    val displayFromQuery: String? =
        if (!isFile)
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
              if (c.moveToFirst()) c.getString(0) else null
            }
        else null

    val displayFromPath: String? =
        uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    val display = displayFromQuery ?: displayFromPath

    // Resolve extensions from name and MIME (both may be null)
    val extFromName = display?.substringAfterLast('.', "")?.lowercase().orEmpty()
    val extFromMime =
        android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(crMime ?: "")
            ?.lowercase()
            .orEmpty()

    // If both are empty, try path suffix once more
    val extFromPath = displayFromPath?.substringAfterLast('.', "")?.lowercase().orEmpty()

    val ext =
        when {
          extFromName in allowedExts -> extFromName
          extFromMime in allowedExts -> extFromMime
          extFromPath in allowedExts -> extFromPath
          else -> ""
        }

    // Normalize base name (without extension), robust fallback
    val rawBase = (display ?: audioString).removeSuffix(if (ext.isNotEmpty()) ".$ext" else "")
    val base =
        rawBase
            .replace(Regex("[^A-Za-z0-9._-]+"), "_") // sanitize
            .trim('_', '.', ' ')
            .ifEmpty { audioString }

    // Normalize/validate MIME from CR or from extension
    val normalizedMime: String? =
        when {
          crMime in allowedMimes -> crMime
          ext == mp3Ext -> mp3Mime
          ext == wavExt -> wavMime
          // For file:// with no CR type, infer from path if possible
          isFile && ext.isNotEmpty() && "audio/$ext" in allowedMimes -> "audio/$ext"
          else -> null
        }

    if (normalizedMime !in allowedMimes) {
      throw UnsupportedAudioFormat("Only MP3/WAV are supported. Got mime=$crMime name=$display")
    }

    // Final extension consistent with normalized MIME
    val finalExt =
        when (normalizedMime) {
          mp3Mime -> mp3Ext
          wavMime -> wavExt
          else -> ext.ifEmpty { mp3Ext } // safe default; shouldn't happen after validation
        }

    return ParsedFromUri(normalizedMime, base, finalExt)
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

  private data class ParsedFromUri(val mime: String?, val base: String, val ext: String)
}
