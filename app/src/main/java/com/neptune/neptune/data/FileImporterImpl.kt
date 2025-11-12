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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnsupportedAudioFormat(msg: String) : IllegalArgumentException(msg)

/*
 * Implementation of FileImporter interface that imports audio files from SAF/file URIs.
 * Formats are defined in a single source of truth: `supportedFormats`.
 * Validates MIME and extension, derives a sane file name, copies the file locally,
 * and retrieves duration metadata.
 * written with help from ChatGPT
 */
class FileImporterImpl(
    private val context: Context,
    private val cr: ContentResolver,
    private val paths: StoragePaths,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : FileImporter {

  private val fileImporterTag = "FileImporter"
  private val defaultBaseName = "audio"


  @RequiresApi(Build.VERSION_CODES.Q)
  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile =
      withContext(io) {
        val safUri = sourceUri.toString().toUri()
        val isFile = safUri.scheme == ContentResolver.SCHEME_FILE
        val parsed = resolveAndValidateAudio(safUri)

        val dir = paths.audioWorkspace()
        val target = uniqueFile(dir, "${parsed.base}.${parsed.ext}")

        if (isFile) {
          // For file:// URIs, copy directly from the file system
          val srcFile = File(safUri.path ?: sourceUri.path ?: "")
          if (!srcFile.exists() || !srcFile.isFile) {
            throw IllegalArgumentException("Source file does not exist: $safUri")
          }
          srcFile.inputStream().use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
          }
        } else {
          // For content:// URIs use ContentResolver
          val inputStream =
              cr.openInputStream(safUri)
                  ?: throw IllegalArgumentException("Cannot open input stream for URI: $safUri")

          inputStream.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
          }
        }

        val duration =
            runCatching {
                  val mmr = MediaMetadataRetriever()
                  try {
                    if (isFile) {
                      // Use file path for reliability
                      val srcFile = File(safUri.path ?: sourceUri.path ?: "")
                      mmr.setDataSource(srcFile.absolutePath)
                    } else {
                      mmr.setDataSource(context, safUri)
                    }
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                  } finally {
                    try {
                      mmr.release()
                    } catch (_: Exception) {}
                  }
                }
                .getOrNull()

        Log.v(fileImporterTag, "imported ${target.name} (${target.length()} bytes, $duration ms)")

        return@withContext FileImporter.ImportedFile(
            displayName = target.name,
            mimeType = parsed.mime,
            sourceUri = sourceUri,
            localUri = target.toURI(),
            sizeBytes = target.length(),
            durationMs = duration ?: 0L)
      }

  // New method: import a file created by the in-app recorder
  override suspend fun importRecorded(file: File): FileImporter.ImportedFile =
      withContext(io) {
        if (!file.exists() || !file.isFile)
            throw IllegalArgumentException("Recorded file does not exist: ${file.path}")

        // Derive base/extension and mime
        val ext = file.extension.lowercase()
        val mime =
            AudioFormats.mimeFromExt(ext)
                ?: throw UnsupportedAudioFormat("Only $AudioFormats.supportedLabel are supported. Got ext=$ext")

        val rawBase = file.nameWithoutExtension
        val base =
            rawBase
                // remove all whitespace entirely
                .replace(Regex("\\s+"), "")
                // replace other invalid chars with '_' and collapse consecutive underscores
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .replace(Regex("_+"), "_")
                .trim('_', '.', ' ')
                .ifEmpty { defaultBaseName }

        val dir = paths.audioWorkspace()
        val target = uniqueFile(dir, "${base}.${ext}")

        val moved =
            try {
              // Prefer atomic move (rename), fallback to copy
              file.renameTo(target)
            } catch (_: Exception) {
              false
            }

        if (!moved) {
          // copy and delete original
          file.inputStream().use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
          }
          try {
            file.delete()
          } catch (_: Exception) {}
        }

        val duration =
            runCatching {
                  val mmr = MediaMetadataRetriever()
                  try {
                    mmr.setDataSource(target.absolutePath)
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                  } finally {
                    try {
                      mmr.release()
                    } catch (_: Exception) {}
                  }
                }
                .getOrNull()

        Log.v(
            fileImporterTag,
            "imported recorded ${target.name} (${target.length()} bytes, $duration ms)")

        return@withContext FileImporter.ImportedFile(
            displayName = target.name,
            mimeType = mime,
            sourceUri = file.toURI(),
            localUri = target.toURI(),
            sizeBytes = target.length(),
            durationMs = duration ?: 0L)
      }


  // Ensures the file is one of the supported formats by MIME and/or extension; derives a sane name.
  private fun resolveAndValidateAudio(uri: Uri): ParsedFromUri {
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

    val extFromName = display?.substringAfterLast('.', "")?.lowercase().orEmpty()
    val extFromPath = displayFromPath?.substringAfterLast('.', "")?.lowercase().orEmpty()
    val extFromMime = AudioFormats.extFromMime(crMime).orEmpty()

    val ext =
        sequenceOf(extFromName, extFromMime, extFromPath)
            .firstOrNull { it in AudioFormats.allowedExts }
            .orEmpty()

    val rawBase =
        (display ?: defaultBaseName).removeSuffix(if (ext.isNotEmpty()) ".${'$'}ext" else "")
    val base =
        rawBase
            .replace(Regex("\\s+"), "_")
            // replace invalid chars with '_' and collapse consecutive underscores
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_', '.', ' ')
            .ifEmpty { defaultBaseName }

    val normalizedMime: String? =
        when {
          crMime in AudioFormats.allowedMimes -> crMime
          ext.isNotEmpty() -> AudioFormats.mimeFromExt(ext)
          else -> null
        }

    if (normalizedMime !in AudioFormats.allowedMimes) {
      throw UnsupportedAudioFormat(
          "Only $AudioFormats.supportedLabel are supported. Got mime=$crMime name=$display")
    }

    val finalExt = AudioFormats.extFromMime(normalizedMime) ?: ext.ifEmpty { AudioFormats.allowedExts.first() }

    return ParsedFromUri(normalizedMime!!, base, finalExt)
  }

  // If file exists, appends -2, -3 etc. to base name to make it unique (no spaces)
  private fun uniqueFile(dir: File, candidate: String): File {
    var f = File(dir, candidate)
    if (!f.exists()) return f
    val base = candidate.substringBeforeLast('.')
    val ext = candidate.substringAfterLast('.', "")
    var i = 2
    do {
      // Use dash suffix (no space) to be consistent with StoragePaths.projectFile
      f = File(dir, "$base-" + i + "." + ext)
      i++
    } while (f.exists())
    return f
  }

  private data class ParsedFromUri(val mime: String, val base: String, val ext: String)
}
