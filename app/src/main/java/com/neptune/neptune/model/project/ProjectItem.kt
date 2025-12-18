package com.neptune.neptune.model.project

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.neptune.neptune.model.sample.Sample

/**
 * Data class representing a project item.
 *
 * @property uid Unique identifier for the project.
 * @property name Name of the project.
 * @property description Description of the project.
 * @property isStoredInCloud Indicates if the project is stored in the cloud.
 * @property isFavorite Indicates if the project is marked as a favorite.
 * @property tags List of tags associated with the project.
 * @property audioPreviewLocalPath Local path to the project's preview image.
 * @property projectFileLocalPath Local path to the project's file.
 * @property audioPreviewCloudUri URL to the project's preview image in the cloud.
 * @property projectFileCloudUri URL to the project's file in the cloud.
 * @property lastUpdated Timestamp of the last update to the project.
 * @property ownerId Identifier of the user who owns the project.
 * @property collaborators List of user identifiers who are collaborators on the project.
 * @author Uri Jaquet
 */
data class ProjectItem(
    val uid: String,
    val name: String,
    val description: String = "",
    @get:PropertyName("isStoredInCloud") val isStoredInCloud: Boolean = false,
    @get:PropertyName("isFavorite") val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val audioPreviewLocalPath: String? = null,
    val projectFileLocalPath: String? = null,
    val imagePreviewLocalPath: String? = null,
    val audioPreviewCloudUri: String? = null,
    val projectFileCloudUri: String? = null,
    val imagePreviewCloudUri: String? = null,
    val lastUpdated: Timestamp = Timestamp.now(),
    val ownerId: String? = null,
    val collaborators: List<String> = emptyList(),
) {
  fun toSample(): Sample {
    return Sample(
        id = uid,
        name = name,
        description = description,
        durationMillis = 0,
        tags = tags,
        likes = 0,
        usersLike = emptyList(),
        comments = 0,
        downloads = 0,
        isPublic = false,
        ownerId = "",
        storageZipPath = "",
        storageImagePath = "",
        storagePreviewSamplePath = "")
  }
}
