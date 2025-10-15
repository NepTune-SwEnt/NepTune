package com.neptune.neptune.data

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import kotlin.math.round

/*
 * Creates a Neptune project zip file containing the audio file and a config.json
 */
class NeptunePackager(private val paths: StoragePaths) {
  fun createProjectZip(
      audioFile: File,
      durationMs: Long?,
      volume: Int = 100,
      startSeconds: Double = 0.0
  ): File {
    require(audioFile.exists() && audioFile.isFile) { "Audio file does not exist: ${audioFile.path}" }
    val base = audioFile.nameWithoutExtension
    val zipFile = paths.projectFile(base)
    val durationSec = durationMs?.div(1000.0) ?: 0.0
    val durationRounded = round(durationSec * 10.0) / 10.0
    val configJson =
        """
            {
            "files": [
            {"filename":"${audioFile.name}", "volume":$volume,
                "start":$startSeconds, "duration":$durationRounded}
                ],
            "filters": []
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

    return zipFile
  }
}
