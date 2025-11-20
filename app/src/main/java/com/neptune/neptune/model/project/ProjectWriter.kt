package com.neptune.neptune.model.project

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

class ProjectWriter {

  private val json = Json {}

  fun writeProject(
      zipFile: File,
      metadata: SamplerProjectData,
      audioFiles: List<File> = emptyList()
  ) {
    val cleanZipPath = zipFile.path.removePrefix("file:").removePrefix("file://")
    val targetZipFile = File(cleanZipPath)
    val tempZip = File(targetZipFile.parentFile, "tmp_${targetZipFile.name}")
    val jsonContent = json.encodeToString(SamplerProjectData.serializer(), metadata)

    ZipOutputStream(FileOutputStream(tempZip)).use { out ->
      audioFiles.forEach { file ->
        if (file.exists()) {
          out.putNextEntry(ZipEntry(file.name))
          file.inputStream().use { it.copyTo(out) }
          out.closeEntry()
        }
      }

      out.putNextEntry(ZipEntry("config.json"))
      out.write(jsonContent.toByteArray())
      out.closeEntry()

      if (zipFile.exists()) {
        ZipFile(zipFile).use { oldZip ->
          oldZip.entries().asSequence().forEach { entry ->
            if (entry.name != "config.json" && audioFiles.none { it.name == entry.name }) {
              out.putNextEntry(ZipEntry(entry.name))
              oldZip.getInputStream(entry).copyTo(out)
              out.closeEntry()
            }
          }
        }
      }
    }
    if (targetZipFile.exists()) targetZipFile.delete()
    val success = tempZip.renameTo(targetZipFile)
    if (!success) {
      tempZip.copyTo(zipFile, overwrite = true)
      tempZip.delete()
    }
  }
}
