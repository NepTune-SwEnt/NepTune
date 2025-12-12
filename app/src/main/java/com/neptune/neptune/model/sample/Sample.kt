package com.neptune.neptune.model.sample

import com.neptune.neptune.model.project.ProjectItem

data class Sample(
    val id: String,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val tags: List<String>,
    val likes: Int,
    val usersLike: List<String>,
    val comments: Int,
    val downloads: Int,
    val isPublic: Boolean = false,
    val ownerId: String = "",
    val storageZipPath: String = "",
    val storageImagePath: String = "",
    val storagePreviewSamplePath: String = "",
    val storageProcessedSamplePath: String = "",
    val creationTime: Long = 0
) {
  fun toProjectItem(): ProjectItem {
    return ProjectItem(
        uid = id,
        name = name,
        description = description,
        tags = tags,
        isStoredInCloud = true,
        audioPreviewCloudUri = storagePreviewSamplePath,
        imagePreviewCloudUri = storageImagePath,
        projectFileCloudUri = storageZipPath,
        ownerId = ownerId,
        collaborators = emptyList())
  }
}
