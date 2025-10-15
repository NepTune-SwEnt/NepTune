package com.neptune.neptune.data

import android.content.Context
import java.io.File

open class StoragePaths(private val context: Context) {

    // /Android/data/<pkg>/files/imports/audio
    fun audioWorkspace(): File =
        File(context.getExternalFilesDir(null), "imports/audio").also { it.mkdirs() }

    // /Android/data/<pkg>/files/projects
    fun projectsWorkspace(): File =
        File(context.getExternalFilesDir(null), "projects").also { it.mkdirs() }

    private val ZIP_SUFFIX = Regex("(?i)\\.zip$")

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
        if (name.isBlank() || name == "." || name == "_" ) name = "project"
        return name
    }

    /** Always return a file under projects/ with exactly one .zip extension. */
    fun projectFile(baseName: String): File {
        val ws = projectsWorkspace().canonicalFile // ensure dir exists and resolve symlinks
        var stem = sanitizeBaseName(baseName)

        // remove any number of trailing ".zip" (case-insensitive)
        while (ZIP_SUFFIX.containsMatchIn(stem)) {
            stem = stem.replace(ZIP_SUFFIX, "")
        }
        if (stem.isBlank()) stem = "project"

        val candidate = File(ws, "$stem.zip").canonicalFile

        // final guard: ensure resolved path is inside workspace
        check(candidate.path.startsWith(ws.path)) {
            "Resolved path escapes workspace: ${candidate.path}"
        }
        return candidate
    }
}
