package com.neptune.neptune.model.project

import com.google.firebase.firestore.FirebaseFirestore

const val TODOS_COLLECTION_PATH = "projects"

class ProjectItemsRepositoryFirestore(private val db: FirebaseFirestore) : ProjectItemsRepository {
    override fun getNewId(): String {
        return db.collection(TODOS_COLLECTION_PATH).document().id
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
}