package com.neptune.neptune.data

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import kotlin.math.round
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Creates a Neptune project zip file containing the audio file and a config.json
 * Partially written with ChatGPT
 */
class NeptunePackager(
    private val paths: StoragePaths,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
  private val secondsDivisor = 1000.0
  private val roundingFactor = 10.0
  private val nullTime = 0.0

  suspend fun createProjectZip(
      audioFile: File,
      durationMs: Long?,
      volume: Int = 100,
      startSeconds: Double = 0.0
  ): File =
      withContext(io) {
        require(audioFile.exists() && audioFile.isFile) {
          "Audio file does not exist: ${audioFile.path}"
        }
        val base = audioFile.nameWithoutExtension
        val zipFile = paths.projectFile(base)
        val durationSec = durationMs?.div(secondsDivisor) ?: nullTime
        val durationRounded = round(durationSec * roundingFactor) / roundingFactor
        val configJson =
            """
              {
                "audioFiles": [
                  {
                    "name": "${audioFile.name}",
                    "volume": $volume,
                    "start": $startSeconds,
                    "duration": $durationRounded
                  }
                ],
                "parameters": []
              }
            """
                .trimIndent()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
          // Add audio file entry
          zipOut.putNextEntry(java.util.zip.ZipEntry(audioFile.name))
          audioFile.inputStream().use { it.copyTo(zipOut) }
          zipOut.closeEntry()
          // Add config.json entry
          zipOut.putNextEntry(java.util.zip.ZipEntry("config.json"))
          zipOut.write(configJson.toByteArray())
          zipOut.closeEntry()
        }

        return@withContext zipFile
      }
}
