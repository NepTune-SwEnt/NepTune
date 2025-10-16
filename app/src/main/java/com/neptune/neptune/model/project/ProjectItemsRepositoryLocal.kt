package com.neptune.neptune.model.project

class ProjectItemsRepositoryLocal : ProjectItemsRepository {
    override fun getNewId(): String {
        TODO("Not yet implemented")
    }

    override suspend fun getAllProjects(): List<ProjectItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getProject(projectID: String): ProjectItem {
        TODO("Not yet implemented")
    }

    override suspend fun addProject(project: ProjectItem) {
        TODO("Not yet implemented")
    }

    override suspend fun editProject(
        projectID: String,
        newValue: ProjectItem
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteProject(projectID: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getProjectDuration(projectID: String): Int {
        TODO("Not yet implemented")
    }
}