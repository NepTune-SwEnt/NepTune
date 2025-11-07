package com.neptune.neptune.data

import android.content.Context
import java.io.File

open class StoragePaths(private val context: Context) {
  private val projects = "projects"
  private val project = "project"
  private val audioPath = "imports/audio"
  private val recordPath = "records"

  // /Android/data/<pkg>/files/imports/audio
  fun audioWorkspace(): File = File(context.filesDir, audioPath).also { it.mkdirs() }

  fun recordWorkspace(): File = File(context.filesDir, recordPath).also { it.mkdirs() }

  // /Android/data/<pkg>/files/projects
  fun projectsWorkspace(): File = File(context.filesDir, projects).also { it.mkdirs() }

  private val zip_suffix = Regex("(?i)\\.zip$")

  /** Turn an arbitrary baseName into a safe, simple file stem. */
  private fun sanitizeBaseName(raw: String): String {
    // strip path parts
    var name = raw.substringAfterLast('/').substringAfterLast('\\')

    // allow only letters, digits, dot, underscore, dash; replace others with '_'
    name = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    // collapse ".." to "_"
    while (name.contains("..")) {
      name = name.replace("..", "_")
    }

    // avoid empty/bad names
    if (name.isBlank() || name == "." || name == "_") name = project
    return name
  }

  /** Always return a file under projects/ with exactly one .zip extension. */
  fun projectFile(baseName: String): File {
    val ws = projectsWorkspace().canonicalFile

    // sanitize + drop any ".zip" suffix(es)
    var stem = sanitizeBaseName(baseName)
    while (zip_suffix.containsMatchIn(stem)) {
      stem = stem.replace(zip_suffix, "")
    }
    if (stem.isBlank()) stem = project

    // Find existing siblings: stem.zip, stem-1.zip, stem-2.zip, ...
    val pattern = Regex("^" + Regex.escape(stem) + "(?:-(\\d+))?\\.zip$", RegexOption.IGNORE_CASE)
    val existingNums = mutableSetOf<Int>()
    ws.listFiles()?.forEach { f ->
      if (f.isFile) {
        val m = pattern.matchEntire(f.name)
        if (m != null) {
          val n = m.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
          existingNums += n
        }
      }
    }

    // Count how many already exist -> that's the requested suffix
    val count = existingNums.size
    // Preferred candidate based on the rule: suffix = count (0 means no suffix)
    var n = count
    fun nameFor(i: Int) = if (i == 0) "$stem.zip" else "$stem-$i.zip"

    // If that exact name is taken (gaps scenario), bump to next available
    while (File(ws, nameFor(n)).exists()) n++

    val candidate = File(ws, nameFor(n)).canonicalFile

    // Safety checks (keep these guards)
    check(candidate.path.startsWith(ws.path + File.separator)) {
      "Resolved path escapes workspace: ${candidate.path}"
    }
    check(!stem.startsWith(".")) { "Hidden or invalid project name: $stem" }
    check(stem.length <= 120) { "Project name too long: ${stem.length} chars" }

    return candidate
  }
}
