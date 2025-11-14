package com.neptune.neptune.fakes

import android.content.Context
import com.neptune.neptune.domain.port.FileImporter
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/**
 * Test importer: writes a small dummy audio file locally and returns it as if it was imported via
 * SAF. Skips ContentResolver/SAP entirely.
 */
class FakeFileImporter(
    private val context: Context,
    private val fileName: String = "test_audio.wav",
    private val bytes: ByteArray = ByteArray(2048) { 0x42 } // dummy data
) : FileImporter {

  override suspend fun importFile(sourceUri: URI): FileImporter.ImportedFile {
    val out = File(context.cacheDir, fileName)
    FileOutputStream(out).use { it.write(bytes) }

    return FileImporter.ImportedFile(
        displayName = out.name,
        mimeType = "audio/wav",
        sourceUri = sourceUri,
        localUri = out.toURI(),
        sizeBytes = out.length(),
        durationMs = 1200L // 1.2s fake duration
        )
  }

  // Provide a simple recorded-file import for tests
  override suspend fun importRecorded(file: File): FileImporter.ImportedFile {
    // Ensure file exists; if not, create a small dummy file at that path
    val out =
        if (file.exists()) file
        else
            File(context.cacheDir, fileName).also {
              FileOutputStream(it).use { os -> os.write(bytes) }
            }

    return FileImporter.ImportedFile(
        displayName = out.name,
        mimeType = "audio/wav",
        sourceUri = out.toURI(),
        localUri = out.toURI(),
        sizeBytes = out.length(),
        durationMs = 1200L)
  }
}
