package com.neptune.neptune.model.project

import com.google.firebase.Timestamp

data class ProjectItem(
    val id: String,
    val name: String,
    val description: String,
    val isFavorite: Boolean,
    val tags: List<String>,
    val previewUrl: String?,
    val fileUrl: String?,
    val lastUpdated: Timestamp,
    val ownerId: String?,
    val collaborators: List<String>,
)
