/**
 * This file was coded with the help of LLMs.
 *
 * @author Uri Jaquet
 */
package com.neptune.neptune.model.project

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProjectItemsRepositoryLocal(context: Context) : ProjectItemsRepository {
  private val projectsFile = File(context.filesDir, "projects.json")
  private val gson = Gson()
  private val mutex = Mutex()
  private val appContext = context.applicationContext

  init {
    if (!projectsFile.exists()) {
      projectsFile.createNewFile()
      writeProjects(emptyMap())
    }
  }

  private fun readProjects(): MutableMap<String, ProjectItem> {
    if (!projectsFile.exists()) {
      return mutableMapOf()
    }
    val json = projectsFile.readText()
    val type = object : TypeToken<Map<String, ProjectItem>>() {}.type
    return gson.fromJson(json, type) ?: mutableMapOf()
  }

  private fun writeProjects(projects: Map<String, ProjectItem>) {
    val json = gson.toJson(projects)
    projectsFile.writeText(json)
  }

  override fun getNewId(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllProjects(): List<ProjectItem> =
      mutex.withLock { readProjects().values.toList() }

  override suspend fun getProject(projectID: String): ProjectItem =
      mutex.withLock {
        readProjects()[projectID]
            ?: throw NoSuchElementException("Project with ID $projectID not found")
      }

  override suspend fun addProject(project: ProjectItem) =
      mutex.withLock {
        val projects = readProjects()
        projects[project.uid] = project
        writeProjects(projects)
      }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) =
      mutex.withLock {
        val projects = readProjects()
        if (projects.containsKey(projectID)) {
          projects[projectID] = newValue
          writeProjects(projects)
        } else {
          throw NoSuchElementException("Project with ID $projectID not found")
        }
      }

  override suspend fun deleteProject(projectID: String) =
      mutex.withLock {
        val projects = readProjects()
        val removed =
            projects.remove(projectID)
                ?: throw NoSuchElementException("Project with ID $projectID not found")
        writeProjects(projects)

        // Delete associated local files (project zip, audio preview, image preview) if they exist.
        val pathsToDelete =
            listOfNotNull(
                removed.projectFileLocalPath,
                removed.audioPreviewLocalPath,
                removed.imagePreviewLocalPath)

        Log.d("ProjectItemsRepositoryLocal", "Deleting files: $pathsToDelete")
        pathsToDelete.forEach { path ->
          try {
            val file = File(path.removePrefix("file:"))
            if (file.exists()) {
              Log.d("ProjectItemsRepositoryLocal", "Deleting file: $file")
              // Try normal delete; if it fails and it's a directory, try deleteRecursively
              val deleted = file.delete()
              if (!deleted && file.exists()) {
                try {
                  file.deleteRecursively()
                } catch (_: Exception) {
                  // ignore failures to delete
                }
              }
            } else {
              Log.d("ProjectItemsRepositoryLocal", "Failed to delete file: $path, does not exist")
            }
          } catch (_: Exception) {
            // ignore any exception while deleting files
            Log.d("ProjectItemsRepositoryLocal", "Failed to delete file: $path")
          }
        }
      }

  /**
   * Find project by project file.
   *
   * @param projectFile The project file as string to find.
   * @return The project item corresponding to the provided project file.
   */
  suspend fun findProjectWithProjectFile(projectFile: String): ProjectItem =
      mutex.withLock {
        val projects = readProjects()
        projects.values.firstOrNull { it.projectFileLocalPath == projectFile }
            ?: throw NoSuchElementException("Project with projectFile $projectFile not found")
      }
}
