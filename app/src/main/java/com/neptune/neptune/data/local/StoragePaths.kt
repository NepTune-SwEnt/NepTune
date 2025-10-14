package com.neptune.neptune.data

import android.content.Context
import java.io.File

open class StoragePaths(private val context: Context) {
  // Directory for imported audio files temporarily stored before being added to a project
  // hardcoded path : /Android/data/com.neptune.neptune/files/ for projects and
  // /Android/data/com.neptune.neptune/files/imports/audio for temporary audio
  fun audioWorkspace(): File =
      File(context.getExternalFilesDir(null), "imports/audio").also { it.mkdirs() }
  // Directory for Neptune project files (.neptune)
  fun projectsWorkspace(): File =
      File(context.getExternalFilesDir(null), "projects").also { it.mkdirs() }

  fun projectFile(baseName: String): File = File(projectsWorkspace(), "$baseName.zip")
}
