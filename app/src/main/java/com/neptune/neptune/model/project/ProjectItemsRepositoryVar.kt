package com.neptune.neptune.model.project

class ProjectItemsRepositoryVar : ProjectItemsRepository {

    private val projects = mutableListOf<ProjectItem>()
    private var idCounter = 0

    override fun getNewId(): String {
        return (idCounter++).toString()
    }

    override suspend fun getAllProjects(): List<ProjectItem> {
        return projects
    }

    override suspend fun getProject(projectID: String): ProjectItem {
        return projects.find { it.id == projectID }
            ?: throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
    }

    override suspend fun addProject(project: ProjectItem) {
        if (projects.any { it.id == project.id }) {
            throw Exception("ProjectItemsRepositoryVar: ProjectItem with the same ID already exists")
        }
        projects.add(project)
    }

    override suspend fun editProject(
        projectID: String,
        newValue: ProjectItem
    ) {
        val index = projects.indexOfFirst { it.id == projectID }
        if (index != -1) {
            projects[index] = newValue.copy(id = projectID)

            return
        }
        throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
    }

    override suspend fun deleteProject(projectID: String) {
        val index = projects.indexOfFirst { it.id == projectID }
        if (index != -1) {
            projects.removeAt(index)
            return
        }
        throw Exception("ProjectItemsRepositoryVar: ProjectItem not found")
    }
}
