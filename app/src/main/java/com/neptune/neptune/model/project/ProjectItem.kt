package com.neptune.neptune.model.project

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a project item.
 *
 * @property uid Unique identifier for the project.
 * @property name Name of the project.
 * @property description Description of the project.
 * @property isStoredInCloud Indicates if the project is stored in the cloud.
 * @property isFavorite Indicates if the project is marked as a favorite.
 * @property tags List of tags associated with the project.
 * @property previewPath Local path to the project's preview image.
 * @property filePath Local path to the project's file.
 * @property previewUrl URL to the project's preview image in the cloud.
 * @property fileUrl URL to the project's file in the cloud.
 * @property lastUpdated Timestamp of the last update to the project.
 * @property ownerId Identifier of the user who owns the project.
 * @property collaborators List of user identifiers who are collaborators on the project.
 * @author Uri Jaquet
 */
data class ProjectItem(
    val uid: String,
    val name: String,
    val description: String = "",
    val isStoredInCloud: Boolean = false,
    @get:PropertyName("isFavorite") val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val previewPath: String? = null,
    val filePath: String? = null,
    val previewUrl: String? = null,
    val fileUrl: String? = null,
    val lastUpdated: Timestamp = Timestamp.now(),
    val ownerId: String? = null,
    val collaborators: List<String> = emptyList(),
)
