package com.neptune.neptune.model.project

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

const val PROJECTITEMS_COLLECTION_PATH = "projects"

class ProjectItemsRepositoryFirestore(private val db: FirebaseFirestore) : ProjectItemsRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewId(): String {
    return db.collection(PROJECTITEMS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    val ownerId =
      Firebase.auth.currentUser?.uid
        ?: throw Exception("ToDosRepositoryFirestore: User not logged in.")

    val snapshot =
      db.collection(PROJECTITEMS_COLLECTION_PATH).whereEqualTo(ownerAttributeName, ownerId).get().await()

    return snapshot.mapNotNull { documentToProjectItem(it) }
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    val document = db.collection(PROJECTITEMS_COLLECTION_PATH).document(projectID).get().await()
    return documentToProjectItem(document) ?: throw Exception("ProjectItemsRepositoryFirestore: Project not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    db.collection(PROJECTITEMS_COLLECTION_PATH).document(project.uid).set(project).await()
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    db.collection(PROJECTITEMS_COLLECTION_PATH).document(projectID).set(newValue).await()
  }

  override suspend fun deleteProject(projectID: String) {
    db.collection(PROJECTITEMS_COLLECTION_PATH).document(projectID).delete().await()
  }

  override suspend fun getProjectDuration(projectID: String): Int {
    TODO("Not yet implemented")
  }

  /**
   * Converts a Firestore document to a ProjectItem.
   *
   * @param document The Firestore document to convert.
   * @return The ProjectItem.
   */
  private fun documentToProjectItem(document: DocumentSnapshot): ProjectItem? {
    return try {
      val uid = document.id
      val name = document.getString("name") ?: return null
      val description = document.getString("description") ?: return null
      val isFavorite = document.getBoolean("isFavorite") ?: false
      val tags = document.get("tags") as? List<String> ?: emptyList()
      val previewUrl = document.getString("previewUrl") ?: ""
      val fileUrl = document.getString("fileUrl") ?: ""
      val lastUpdated = document.getTimestamp("lastUpdated") ?: return null
      val ownerId = document.getString("ownerId") ?: return null
      val collaborators = document.get("collaborators") as? List<String> ?: emptyList()

      ProjectItem(
        uid = uid,
        name = name,
        description = description,
        isStoredInCloud = true,
        isFavorite = isFavorite,
        tags = tags,
        previewPath = null,
        filePath = null,
        previewUrl = previewUrl,
        fileUrl = fileUrl,
        lastUpdated = lastUpdated,
        ownerId = ownerId,
        collaborators = collaborators
      )
    } catch (e: Exception) {
      Log.e("ProjectItemsRepositoryFirestore", "Error converting document to ProjectItem", e)
      null
    }
  }
}
