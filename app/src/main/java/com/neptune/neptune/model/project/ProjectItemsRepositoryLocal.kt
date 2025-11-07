package com.neptune.neptune.model.project

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

// Pass the context in through the constructor
class ProjectItemsRepositoryLocal(private val context: Context) : ProjectItemsRepository {
  // Now the context is available immediately.
  private val projectsFile = File(context.filesDir, "projects.json")
  private val gson = Gson()

  init {
    // For testing purposes, we can initialize the file if it doesn't exist.
    if (!projectsFile.exists()) {
      projectsFile.createNewFile()
      // Optionally, pre-populate with some initial data for testing
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

  override suspend fun getAllProjects(): List<ProjectItem> {
    return readProjects().values.toList()
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    return readProjects()[projectID]
      ?: throw NoSuchElementException("Project with ID $projectID not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    val projects = readProjects()
    projects[project.uid] = project
    writeProjects(projects)
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    val projects = readProjects()
    if (projects.containsKey(projectID)) {
      projects[projectID] = newValue
      writeProjects(projects)
    } else {
      throw NoSuchElementException("Project with ID $projectID not found")
    }
  }

  override suspend fun deleteProject(projectID: String) {
    val projects = readProjects()
    if (projects.remove(projectID) == null) {
      throw NoSuchElementException("Project with ID $projectID not found")
    }
    writeProjects(projects)
  }

  override suspend fun getProjectDuration(projectID: String): Int {
    TODO("Not yet implemented")
  }
}
