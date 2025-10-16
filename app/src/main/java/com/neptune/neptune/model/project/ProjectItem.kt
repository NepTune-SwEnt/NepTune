package com.neptune.neptune.model.project

import com.google.firebase.Timestamp

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String = "",
    val isStoredInCloud: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val previewPath: String? = null,
    val filePath: String? = null,
    val previewUrl: String? = null,
    val fileUrl: String? = null,
    val lastUpdated: Timestamp = Timestamp.now(),
    val ownerId: String? = null,
    val collaborators: List<String> = emptyList(),
)
