package com.neptune.neptune.model.project

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.tasks.await

const val PROJECT_ITEMS_COLLECTION_PATH = "projects"

data class SampleDocument(
    val uid: String,
    val name: String,
    val description: String,
    @get:PropertyName("isStoredInCloud") val isStoredInCloud: Boolean,
    @get:PropertyName("isFavorite") val isFavorite: Boolean,
    val tags: List<String>,
    val audioPreviewUri: String?,
    val projectFileUri: String?,
    val lastUpdated: Timestamp,
    val ownerId: String?,
    val collaborators: List<String>
)

fun ProjectItem.toSample(): SampleDocument {
  return SampleDocument(
      uid = uid,
      name = name,
      description = description,
      isStoredInCloud = isStoredInCloud,
      isFavorite = isFavorite,
      tags = tags,
      audioPreviewUri = audioPreviewUri,
      projectFileUri = projectFileUri,
      lastUpdated = lastUpdated,
      ownerId = ownerId,
      collaborators = collaborators,
  )
}

class ProjectItemsRepositoryFirestore(private val db: FirebaseFirestore) : ProjectItemsRepository {
  private val ownerAttributeName = "ownerId"

  override fun getNewId(): String {
    return db.collection(PROJECT_ITEMS_COLLECTION_PATH).document().id
  }

  override suspend fun getAllProjects(): List<ProjectItem> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ToDosRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(PROJECT_ITEMS_COLLECTION_PATH)
            .whereEqualTo(ownerAttributeName, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { documentToProjectItem(it) }
  }

  override suspend fun getProject(projectID: String): ProjectItem {
    val document = db.collection(PROJECT_ITEMS_COLLECTION_PATH).document(projectID).get().await()
    return documentToProjectItem(document)
        ?: throw Exception("ProjectItemsRepositoryFirestore: Project not found")
  }

  override suspend fun addProject(project: ProjectItem) {
    db.collection(PROJECT_ITEMS_COLLECTION_PATH)
        .document(project.uid)
        .set(project.toSample())
        .await()
  }

  override suspend fun editProject(projectID: String, newValue: ProjectItem) {
    db.collection(PROJECT_ITEMS_COLLECTION_PATH).document(projectID).set(newValue).await()
  }

  override suspend fun deleteProject(projectID: String) {
    db.collection(PROJECT_ITEMS_COLLECTION_PATH).document(projectID).delete().await()
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
      val isFavorite = document.getBoolean("isFavorite") ?: return null
      val tags = document.get("tags") as? List<String> ?: emptyList()
      val previewUrl = document.getString("previewUrl")
      val projectFileUri = document.getString("fileUrl")
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
          audioPreviewUri = previewUrl,
          projectFileUri = projectFileUri,
          lastUpdated = lastUpdated,
          ownerId = ownerId,
          collaborators = collaborators)
    } catch (e: Exception) {
      Log.e("ProjectItemsRepositoryFirestore", "Error converting document to ProjectItem", e)
      null
    }
  }
}
